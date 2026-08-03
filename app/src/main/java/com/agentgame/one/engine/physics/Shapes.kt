package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Vector2

/** Base shape used by collision bodies (Godot's `Shape2D` analogue). */
sealed class Shape {
    var position: Vector2 = Vector2.ZERO
    var rotation: Float = 0f

    abstract fun asRect(): Rect2
    abstract fun clone(): Shape

    /** Offset centre, used in overlap tests. */
    open fun center(ownerPos: Vector2): Vector2 = ownerPos + position
}

/** Circle collision shape. */
class CircleShape(var radius: Float = 10f) : Shape() {
    override fun asRect(): Rect2 {
        val c = position
        return Rect2(c.x - radius, c.y - radius, radius * 2f, radius * 2f)
    }

    override fun clone() = CircleShape(radius).also {
        it.position = position; it.rotation = rotation
    }
}

/** Axis-aligned rectangle collision shape (AABB in local space). */
class RectShape(var size: Vector2 = Vector2(20f, 20f)) : Shape() {
    override fun asRect(): Rect2 {
        val half = size / 2f
        val c = position
        return Rect2(c.x - half.x, c.y - half.y, size.x, size.y)
    }

    override fun clone() = RectShape(size).also {
        it.position = position; it.rotation = rotation
    }
}

object Collisions {
    fun overlap(ownerA: Vector2, shapeA: Shape, ownerB: Vector2, shapeB: Shape): Boolean {
        return when {
            shapeA is CircleShape && shapeB is CircleShape ->
                shapeA.center(ownerA).distanceSquaredTo(shapeB.center(ownerB)) <=
                    (shapeA.radius + shapeB.radius) * (shapeA.radius + shapeB.radius)
            shapeA is CircleShape && shapeB is RectShape -> circleRect(shapeA, shapeB, ownerA, ownerB)
            shapeA is RectShape && shapeB is CircleShape -> circleRect(shapeB, shapeA, ownerB, ownerA)
            else -> shapeA.asRect().translated(ownerA).intersects(shapeB.asRect().translated(ownerB))
        }
    }

    private fun circleRect(circle: CircleShape, rect: RectShape, circleOwner: Vector2, rectOwner: Vector2): Boolean {
        val r = rect.asRect().translated(rectOwner)
        val c = circle.center(circleOwner)
        val closestX = c.x.coerceIn(r.left, r.right)
        val closestY = c.y.coerceIn(r.top, r.bottom)
        val dx = c.x - closestX
        val dy = c.y - closestY
        return dx * dx + dy * dy <= circle.radius * circle.radius
    }

    /** Minimal push-out vector to separate [a] from [b] (b is the static one). */
    fun separation(ownerA: Vector2, shapeA: Shape, ownerB: Vector2, shapeB: Shape): Vector2? {
        if (!overlap(ownerA, shapeA, ownerB, shapeB)) return null
        val rectA = shapeA.asRect().translated(ownerA)
        val rectB = shapeB.asRect().translated(ownerB)
        val overlapX = kotlin.math.min(rectA.right, rectB.right) - kotlin.math.max(rectA.left, rectB.left)
        val overlapY = kotlin.math.min(rectA.bottom, rectB.bottom) - kotlin.math.max(rectA.top, rectB.top)
        return if (overlapX < overlapY) {
            val dir = if (rectA.center.x < rectB.center.x) -1f else 1f
            Vector2(dir * overlapX, 0f)
        } else {
            val dir = if (rectA.center.y < rectB.center.y) -1f else 1f
            Vector2(0f, dir * overlapY)
        }
    }
}
