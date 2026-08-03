package com.agentgame.one.engine.core

/**
 * A 2D affine transform (Godot's `Transform2D` analogue), built as: origin + xAxis + yAxis.
 * Rendering to android.graphics.Canvas is done by applying translate/rotate/scale from these.
 */
data class Transform2D(
    val origin: Vector2 = Vector2.ZERO,
    val xAxis: Vector2 = Vector2(1f, 0f),
    val yAxis: Vector2 = Vector2(0f, 1f),
) {
    val basisX get() = xAxis
    val basisY get() = yAxis

    /** Applies this transform to a point. */
    fun apply(p: Vector2): Vector2 = xAxis * p.x + yAxis * p.y + origin

    /** Composes two transforms: `this` applied after `other` (world = parent * local). */
    fun multipliedBy(other: Transform2D): Transform2D {
        val o = origin + xAxis * other.origin.x + yAxis * other.origin.y
        val nx = xAxis * other.xAxis.x + yAxis * other.xAxis.y
        val ny = xAxis * other.yAxis.x + yAxis * other.yAxis.y
        return Transform2D(o, nx, ny)
    }

    val isIdentity: Boolean get() =
        origin == Vector2.ZERO && xAxis == Vector2(1f, 0f) && yAxis == Vector2(0f, 1f)

    companion object {
        val IDENTITY = Transform2D()

        fun translated(offset: Vector2) = Transform2D(offset)
        fun rotated(angle: Float): Transform2D {
            val c = kotlin.math.cos(angle)
            val s = kotlin.math.sin(angle)
            return Transform2D(Vector2.ZERO, Vector2(c, s), Vector2(-s, c))
        }
        fun scaled(scale: Vector2) = Transform2D(
            Vector2.ZERO, Vector2(scale.x, 0f), Vector2(0f, scale.y)
        )

        /** position (pixels), rotation (radians), scale. y-axis points down. */
        fun of(position: Vector2, rotation: Float, scale: Vector2): Transform2D {
            val c = kotlin.math.cos(rotation)
            val s = kotlin.math.sin(rotation)
            val sx = scale.x
            val sy = scale.y
            return Transform2D(
                origin = position,
                xAxis = Vector2(c * sx, s * sx),
                yAxis = Vector2(-s * sy, c * sy),
            )
        }
    }
}
