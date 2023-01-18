package net.stellarica.server.multiblocks

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.utils.Direction
import net.stellarica.server.util.OriginRelative

data class MultiblockType(
	val id: String,
	val blocks: Map<OriginRelative, Block>
) {
	fun detect(origin: Point, world: Instance): MultiblockInstance? {
		setOf(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST).forEach { facing ->
			if (validate(facing, origin, world)) {
				return MultiblockInstance(
					origin,
					world,
					facing,
					this
				)
			}
		}
		return null
	}

	/**
	 * Whether the collection of blocks at [origin] in [world] matches this multiblock type
	 */
	fun validate(facing: Direction, origin: Point, world: Instance): Boolean {
		fun rotationFunction(it: OriginRelative) = when (facing) {
			Direction.NORTH -> it
			Direction.EAST -> OriginRelative(-it.z, it.y, it.x)
			Direction.SOUTH -> OriginRelative(-it.x, it.y, -it.z)
			Direction.WEST -> OriginRelative(it.z, it.y, -it.x)

			else -> throw IllegalArgumentException("Invalid multiblock facing direction: $facing")
		}

		blocks.forEach {
			val rotatedLocation = rotationFunction(it.key)
			val relativeLocation =
				origin.add(rotatedLocation.x.toDouble(), rotatedLocation.y.toDouble(), rotatedLocation.z.toDouble())
			if (world.getBlock(relativeLocation) != it.value) {
				return false
			} // A block we were expecting is missing, so break the function.
		}
		return true // Valid multiblock of this type there
	}
}
