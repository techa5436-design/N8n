package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Node2D
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Vector2

/**
 * Defines the collision shape of a physics body (Godot's `CollisionShape2D` analogue).
 * Usually a child of a [PhysicsBody2D]. Its position (relative to the body) offsets the shape.
 */
class CollisionShape2D(nodeName: String = "CollisionShape2D") : Node2D(nodeName) {

    var shapeType: ShapeType = ShapeType.RECT

    enum class ShapeType { RECT, CIRCLE, POLYGON }

    var size: Vector2 = Vector2(20f, 20f)
    var radius: Float = 10f
    var points: List<Vector2> = emptyList()

    /** Builds a [Shape] from this node's parameters. */
    fun buildShape(): Shape {
        val s = when (shapeType) {
            ShapeType.RECT -> RectShape(size)
            ShapeType.CIRCLE -> CircleShape(radius)
            ShapeType.POLYGON -> RectShape(points.asAabb())
        }
        s.position = position
        s.rotation = rotation
        return s
    }

    private fun List<Vector2>.asAabb(): Vector2 {
        if (isEmpty()) return Vector2(20f, 20f)
        var minX = first().x; var maxX = first().x
        var minY = first().y; var maxY = first().y
        for (p in this) {
            if (p.x < minX) minX = p.x
            if (p.x > maxX) maxX = p.x
            if (p.y < minY) minY = p.y
            if (p.y > maxY) maxY = p.y
        }
        return Vector2(maxX - minX, maxY - minY)
    }

    fun safeShape(): Shape? = buildShape()

    fun rect(w: Float, h: Float): CollisionShape2D {
        shapeType = ShapeType.RECT; size = Vector2(w, h); return this
    }

    fun circle(r: Float): CollisionShape2D {
        shapeType = ShapeType.CIRCLE; radius = r; return this
    }

    override fun computeLocalRect(global: com.agentgame.one.engine.core.Transform2D): Rect2 {
        val s = buildShape()
        val r = s.asRect().translated(global.origin)
        return r
    }
}
