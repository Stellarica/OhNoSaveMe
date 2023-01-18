package net.stellarica.server.util

/**
 * Coordinates relative to the origin of a multiblock
 */
data class OriginRelative(
	/**
	 * The x component
	 */
	val x: Int,
	/**
	 * The y component
	 */
	val y: Int,
	/**
	 * The z component
	 */
	val z: Int
)