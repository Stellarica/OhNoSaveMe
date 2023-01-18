package net.stellarica.server.crafts

import net.kyori.adventure.audience.Audience
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.Player
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.Rotation
import net.stellarica.server.multiblocks.MultiblockHandler
import net.stellarica.server.multiblocks.MultiblockInstance
import net.stellarica.server.util.OriginRelative
import net.stellarica.server.util.asDegrees
import net.stellarica.server.util.rotateCoordinates
import net.stellarica.server.util.sendRichMessage
import undetectableBlocks
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

open class Craft(var origin: Point, var world: Instance, var owner: Player) {
	val sizeLimit = 10000

	var multiblocks = mutableSetOf<WeakReference<MultiblockInstance>>()
	var detectedBlocks = mutableSetOf<Point>()

	var passengers = mutableSetOf<LivingEntity>()

	val blockCount: Int
		get() = detectedBlocks.size

	/**
	 * The blocks considered to be "inside" of the ship, but not neccecarily detected.
	 */
	protected var bounds = mutableSetOf<OriginRelative>()

	/**
	 * @return Whether [block] is considered to be inside this craft
	 */
	fun contains(block: Point?): Boolean {
		block ?: return false
		return detectedBlocks.contains(block) || bounds.contains(block.sub(origin).let {
			OriginRelative(
				it.blockX(),
				it.blockY(),
				it.blockZ()
			)
		})
	}

	fun calculateHitbox() {
		detectedBlocks
			.map { pos ->
				pos.sub(origin)
					.let { OriginRelative(it.blockX(), it.blockY(), it.blockZ()) }
			}
			.sortedBy { -it.y }
			.forEach { block ->
				val max = bounds.filter { it.x == block.x && it.z == block.z }.maxByOrNull { it.y }?.y ?: block.y
				for (y in block.y..max) {
					bounds.add(OriginRelative(block.x, y, block.z))
				}
			}
	}

	fun detect() {
		var nextBlocksToCheck = detectedBlocks
		nextBlocksToCheck.add(origin)
		detectedBlocks = mutableSetOf()
		val checkedBlocks = nextBlocksToCheck.toMutableSet()

		val startTime = System.currentTimeMillis()

		val chunks = mutableSetOf<Chunk>()

		while (nextBlocksToCheck.size > 0) {
			val blocksToCheck = nextBlocksToCheck
			nextBlocksToCheck = mutableSetOf()

			for (currentBlock in blocksToCheck) {

				if (undetectableBlocks.contains(world.getBlock(currentBlock))) continue

				if (detectedBlocks.size > sizeLimit) {
					owner.sendRichMessage("<gold>Detection limit reached. (${sizeLimit} blocks)")
					nextBlocksToCheck.clear()
					detectedBlocks.clear()
					break
				}

				detectedBlocks.add(currentBlock)

				// todo: this is terrible duct tape, fix it
				chunks.add(world.getChunk(currentBlock.blockX(), currentBlock.blockZ()) ?: world.loadChunk(currentBlock.blockX(), currentBlock.blockZ()).get())

				// Slightly condensed from MSP's nonsense, but this could be improved
				for (x in -1..1) {
					for (y in -1..1) {
						for (z in -1..1) {
							if (x == y && z == y && y == 0) continue
							val block = currentBlock.add(x.toDouble(), y.toDouble(), z.toDouble())
							if (!checkedBlocks.contains(block)) {
								checkedBlocks.add(block)
								nextBlocksToCheck.add(block)
							}
						}
					}
				}
			}
		}

		val elapsed = System.currentTimeMillis() - startTime
		owner.sendRichMessage("<green>Craft detected! (${detectedBlocks.size} blocks)")
		owner.sendRichMessage(
			"<gray>Detected ${detectedBlocks.size} blocks in ${elapsed}ms. " +
					"(${detectedBlocks.size / elapsed.coerceAtLeast(1)} blocks/ms)"
		)
		owner.sendRichMessage(
			"<gray>Calculated Hitbox in ${
				measureTimeMillis {
					calculateHitbox()
				}
			}ms. (${bounds.size} blocks)")

		// Detect all multiblocks
		multiblocks.clear()
		// this is probably slow
		multiblocks.addAll(
			chunks.map { chunk -> MultiblockHandler.multiblocks[world]!![chunk]?.filter { detectedBlocks.contains(it.origin) } ?: setOf() }.flatten()
				.map { WeakReference(it) })

		owner.sendRichMessage("<gray>Detected ${multiblocks.size} multiblocks")
	}

	fun movePassengers(offset: (Point) -> Point, rotation: Rotation) {
		passengers.forEach {
			// TODO: FIX
			// this is not a good solution because if there is any rotation, the player will not be translated by the offset
			// The result is that any ship movement that attempts to rotate and move in the same action will break.
			// For now there aren't any actions like that, but if there are in the future, this will need to be fixed.
			//
			// Rotating the whole ship around the adjusted origin will not work,
			// as rotating the ship 4 times does not bring it back to the original position
			//
			// However, without this dumb fix players do not rotate to the proper relative location
			val destination =
				if (rotation == Rotation.CLOCKWISE || rotation == Rotation.COUNTER_CLOCKWISE) rotateCoordinates(
					it.position,
					Pos(
						0.5,
						0.0,
						0.5
					).add(origin),
					rotation
				)
				else offset(it.position)

			// todo: handle teleporting to a different world
			it.teleport(Pos(
				destination.x(),
				destination.y(),
				destination.z(),
				it.position.yaw + rotation.asDegrees.toFloat(),
				it.position.pitch
			))
		}
	}

	fun sendRichMessage(message: String) {
		passengers.forEach {
			if (it is Audience) {
				it.sendRichMessage(message)
			}
		}
	}

	/**
	 * Translate the craft by [offset] blocks
	 * @see queueChange
	 */
	fun move(offset: Vec) {
		// don't want to let them pass a decimal movement
		// since the ships snap to blocks but entities can actually move by that much
		// relative entity teleportation will be messed up
		val change = Vec(offset.blockX().toDouble(), offset.blockY().toDouble(), offset.blockZ().toDouble())
		change({ current ->
			return@change current.add(change)
		}, world)
	}

	/**
	 * Rotate the craft and contents by [rotation]
	 * @see queueChange
	 */
	fun rotate(rotation: Rotation) {
		change({ current ->
			return@change rotateCoordinates(current, origin, rotation)
		}, world, rotation) {
			calculateHitbox() // rather than keep track of a hitbox rotation, just recacluate it when we rotate.
		}
	}

	private fun change(
		modifier: (Point) -> Point,
		targetWorld: Instance,
		rotation: Rotation = Rotation.NONE,
		callback: () -> Unit = {}
	) {
		val targets = ConcurrentHashMap<Point, Point>()
		runBlocking {
			detectedBlocks.chunked(500).forEach { section ->
				// chunk into sections to process parallel
				async(Dispatchers.Default) {
					section.forEach { current ->
						targets[current] = modifier(current)
					}
				}
			}
		}

		targets.forEach { (_, target) ->
			// todo: it's possible for detectedBlocks to contain it but not actually be detected (if the world is different)
			if (!targetWorld.getBlock(target).isAir && !detectedBlocks.contains(target)) {
				sendRichMessage("<gold>Blocked by ${world.getBlock(target).name()} at <bold>(${target.x()}, ${target.y()}, ${target.z()}</bold>)!\"")
				return
			}
		}

		val newDetectedBlocks = mutableSetOf<Point>()
		// iterating over twice isn't great
		targets.forEach { (current, target) ->
			val currentBlock = world.getBlock(current)

			// set the block
			targetWorld.setBlock(target, currentBlock) // todo: rotate currentBlock
			newDetectedBlocks.add(target)

			// move any entities
		}

		// there's probably a better way to get all detectedBlocks that aren't in newDetectedBlocks
		// todo: handle world changes
		detectedBlocks.filter { !newDetectedBlocks.contains(it) }.forEach {
			world.setBlock(it, Block.AIR)
		}

		detectedBlocks = newDetectedBlocks

		// move multiblocks
		multiblocks.map {
			val mb = it.get() ?: return@map null
			MultiblockHandler.multiblocks[world]!![world.getChunk(mb.origin.blockX(), mb.origin.blockZ())]!!.remove(mb)
			val new = mb.copy(origin = modifier(mb.origin))
			MultiblockHandler.multiblocks[targetWorld]!![targetWorld.getChunk(new.origin.blockX(), new.origin.blockZ())]!!.add(new)
			return@map new
		}

		movePassengers(modifier, rotation)
		world = targetWorld
		origin = modifier(origin)
		callback()
	}
}