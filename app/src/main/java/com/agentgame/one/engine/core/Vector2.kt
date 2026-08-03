package com.agentgame.one.engine.core

/**
 * A small, dependency-free 2D vector used throughout the engine (Godot's `Vector2` analogue).
 * Immutable-friendly: all ops return new instances.
 */
data class Vector2(val x: Float = 0f, val y: Float = 0f) {

    val lengthSquared: Float get() = x * x + y * y
    val length: Float get() = kotlin.math.sqrt(lengthSquared)
    val isZero: Boolean get() = x == 0f && y == 0f

    fun normalized(): Vector2 {
        val l = length
        return if (l == 0f) ZERO else Vector2(x / l, y / l)
    }

    fun dot(o: Vector2): Float = x * o.x + y * o.y
    fun cross(o: Vector2): Float = x * o.y - y * o.x

    fun distanceTo(o: Vector2): Float = (this - o).length
    fun distanceSquaredTo(o: Vector2): Float = (this - o).lengthSquared

    operator fun plus(o: Vector2) = Vector2(x + o.x, y + o.y)
    operator fun minus(o: Vector2) = Vector2(x - o.x, y - o.y)
    operator fun times(s: Float) = Vector2(x * s, y * s)
    operator fun times(o: Vector2) = Vector2(x * o.x, y * o.y)
    operator fun div(s: Float) = Vector2(x / s, y / s)
    operator fun unaryMinus() = Vector2(-x, -y)

    /** Rotates the vector by [angle] radians (screen coordinates, y-down). */
    fun rotated(angle: Float): Vector2 {
        val c = kotlin.math.cos(angle)
        val s = kotlin.math.sin(angle)
        return Vector2(x * c - y * s, x * s + y * c)
    }

    fun lerp(to: Vector2, t: Float): Vector2 =
        Vector2(x + (to.x - x) * t, y + (to.y - y) * t)

    fun clamped(magnitude: Float): Vector2 {
        val l = length
        return if (l > magnitude) normalized() * magnitude else this
    }

    fun withX(nx: Float) = Vector2(nx, y)
    fun withY(ny: Float) = Vector2(x, ny)

    companion object {
        val ZERO = Vector2(0f, 0f)
        val ONE = Vector2(1f, 1f)
        val UP = Vector2(0f, -1f)
        val DOWN = Vector2(0f, 1f)
        val LEFT = Vector2(-1f, 0f)
        val RIGHT = Vector2(1f, 0f)
        fun fromAngle(radians: Float): Vector2 = Vector2(kotlin.math.cos(radians), kotlin.math.sin(radians))
    }
}

/** A 2D axis-aligned rectangle, used for layouts and collisions. */
data class Rect2(val x: Float = 0f, val y: Float = 0f, val w: Float = 0f, val h: Float = 0f) {
    val left get() = x
    val top get() = y
    val right get() = x + w
    val bottom get() = y + h
    val position get() = Vector2(x, y)
    val size get() = Vector2(w, h)
    val center get() = Vector2(x + w / 2f, y + h / 2f)
    val hasArea get() = w > 0f && h > 0f

    fun contains(p: Vector2): Boolean = p.x >= x && p.x <= right && p.y >= y && p.y <= bottom
    fun intersects(o: Rect2): Boolean =
        x < o.right && right > o.x && y < o.bottom && bottom > o.y

    fun translated(offset: Vector2) = Rect2(x + offset.x, y + offset.y, w, h)
    fun grow(amount: Float) = Rect2(x - amount, y - amount, w + amount * 2f, h + amount * 2f)
    fun expanded(toInclude: Rect2): Rect2 {
        val nx = minOf(x, toInclude.x)
        val ny = minOf(y, toInclude.y)
        return Rect2(nx, ny, maxOf(right, toInclude.right) - nx, maxOf(bottom, toInclude.bottom) - ny)
    }

    companion object {
        fun fromCenter(center: Vector2, size: Vector2): Rect2 =
            Rect2(center.x - size.x / 2f, center.y - size.y / 2f, size.x, size.y)
    }
}
