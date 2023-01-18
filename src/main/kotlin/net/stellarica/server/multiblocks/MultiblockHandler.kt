package net.stellarica.server.multiblocks

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.tag.Tag
import net.minestom.server.utils.Direction
import net.stellarica.server.util.sendRichActionBar
import java.util.concurrent.ConcurrentHashMap

object MultiblockHandler {
	val types = mutableListOf<MultiblockType>()
	val tag = Tag.String("multiblocks")
	val multiblocks = ConcurrentHashMap<Instance, MutableMap<Chunk, MutableSet<MultiblockInstance>>>()

	fun registerListeners() {
		val events = MinecraftServer.getGlobalEventHandler()
		events.addListener(InstanceChunkUnloadEvent::class.java) { event ->
			writeMultiblocks(event.chunk, multiblocks[event.instance]!![event.chunk]!!.toSet())
			multiblocks[event.instance]!!.remove(event.chunk)
		}
		events.addListener(InstanceChunkLoadEvent::class.java) { event ->
			multiblocks.putIfAbsent(event.instance, mutableMapOf())
			multiblocks[event.instance]!!.set(event.chunk, readMultiblocks(event.chunk).toMutableSet())
		}
		events.addListener(InstanceTickEvent::class.java) { event ->
			validateLoadedMultiblocks(event.instance)
		}
		@Suppress("UnstableApiUsage")
		events.addListener(PlayerBlockInteractEvent::class.java) { event ->
			if (event.block != Block.DIAMOND_BLOCK) return@addListener
			// this line is... less than great
			if (multiblocks[event.instance]!!.values.flatten().map {it.origin}.contains(event.blockPosition)) {
				event.player.sendRichActionBar("<gold>Multiblock already detected!")
				return@addListener
			}
			val mb = detect(event.blockPosition, event.instance)
			if (mb != null) {
				event.player.sendRichActionBar("<green>Multiblock detected: ${mb.type.id}")
			} else {
				event.player.sendRichActionBar("<red>No multiblock found")
			}
		}
	}

	fun detect(origin: Point, world: Instance): MultiblockInstance? {
		val possible = mutableListOf<MultiblockInstance>()
		types.forEach {
			val instance = it.detect(origin, world)
			if (instance != null) {
				possible.add(instance)
			}
		}
		// return the largest possible, in case there are multiple
		return possible.maxByOrNull { it.type.blocks.size }?.also { inst ->
			// if (MultiblockDetectEvent.call(MultiblockDetectEvent.EventData(it))) return null // maybe check for a smaller one?
			val chunk = world.getChunk(origin.blockX(), origin.blockZ()) ?: return null
			multiblocks[world]!![chunk] = (multiblocks[world]?.get(chunk) ?: mutableSetOf()).also { it.add(inst) } // not fond of this
		}
	}

	// can't use multiblockinstance as we don't want to serialize the world or the type
	@Serializable
	private data class MultiblockData(
		val type: String,
		val oX: Double,
		val oY: Double,
		val oZ: Double,
		val direction: Direction
	) {

		constructor(multiblock: MultiblockInstance) :
				this(
					multiblock.type.id,
					multiblock.origin.x(),
					multiblock.origin.y(),
					multiblock.origin.z(),
					multiblock.direction
				)

		fun toMultiblockInstance(world: Instance) =
			MultiblockInstance(
				Pos(oX, oY, oZ),
				world,
				direction,
				types.first { it.id == type }
			)
	}

	fun readMultiblocks(chunk: Chunk): Set<MultiblockInstance> {
		// todo: don't use a string in the nbt, better to just serialize to nbt
		val string = chunk.getTag(tag) ?: return emptySet()
		return Json.decodeFromString<Set<MultiblockData>>(string).map { it.toMultiblockInstance(chunk.instance) }
			.toMutableSet()
	}

	fun writeMultiblocks(chunk: Chunk, multiblocks: Set<MultiblockInstance>) {
		// todo: see above todo
		if (multiblocks.isEmpty()) return
		chunk.setTag(tag, Json.encodeToString(multiblocks.map { MultiblockData(it) }))
	}

	fun validateLoadedMultiblocks(instance: Instance) {
		multiblocks.putIfAbsent(instance, mutableMapOf())
		multiblocks[instance]!!.forEach { (_, chunkMultiblocks) ->
			chunkMultiblocks.filter { it.validate() }
		}
	}
}