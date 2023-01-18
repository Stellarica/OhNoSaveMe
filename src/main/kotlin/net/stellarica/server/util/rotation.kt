package net.stellarica.server.util

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.utils.Rotation
import kotlin.math.cos
import kotlin.math.sin

/**
 * Rotate [loc] around [origin] by [theta] radians.
 * Note, [theta] positive = clockwise, negative = counter clockwise
 */
fun rotateCoordinates(loc: Point, origin: Point, theta: Double): Point = Vec(
	origin.x() + (((loc.x() - origin.x()) * cos(theta)) - ((loc.z() - origin.z()) * sin(theta))),
	loc.y(),  // too many parentheses is better than too few
	origin.z() + (((loc.x() - origin.x()) * sin(theta)) + ((loc.z() - origin.z()) * cos(theta))),
)

/**
 * Rotate [loc] [rotation] around [origin]
 */
fun rotateCoordinates(loc: Point, origin: Point, rotation: Rotation): Point =
	rotateCoordinates(loc, origin, rotation.asRadians)

val Rotation.asRadians: Double
	get() = when (this) {
		Rotation.NONE -> 0.0
		Rotation.CLOCKWISE -> Math.PI / 2
		Rotation.FLIPPED -> Math.PI
		Rotation.COUNTER_CLOCKWISE -> -Math.PI / 2
		else -> throw IllegalArgumentException("I was too lazy to add asRadians for $this, fix it or cope")
	}

val Rotation.asDegrees: Double
	get() = Math.toDegrees(asRadians) // :iea: