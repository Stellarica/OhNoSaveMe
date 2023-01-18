package net.stellarica.server.multiblocks

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.utils.Direction

data class MultiblockInstance(
	val origin: Point,
	val world: Instance,
	val direction: Direction,
	val type: MultiblockType
) {
	fun validate() = type.validate(direction, origin, world)
}