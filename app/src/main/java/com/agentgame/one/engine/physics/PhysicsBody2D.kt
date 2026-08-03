package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Node2D
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Signal
import com.agentgame.one.engine.core.Vector2

/**
 * Base class for objects affected by (or collidable with) the 2D physics world
 * (Godot's `PhysicsBody2D` analogue). The concrete shape is supplied by a child
 * [CollisionShape2D] node (or set directly via [shape]).
 */
open class PhysicsBody2D(nodeName: String = "PhysicsBody2D") : Node2D(nodeName) {

    /** Collision layer and mask bitmasks (Godot-style). */
    var collisionLayer: Int = 1
    var collisionMask: Int = 1

    private var _shape: Shape? = null

    /** The collision shape; resolved from a child CollisionShape2D if not set directly. */
    open var shape: Shape?
        get() = _shape ?: findShapeChild()
        set(value) { _shape = value }

    private fun findShapeChild(): Shape? {
        for (c in children) {
            if (c is CollisionShape2D) {
                val s = c.safeShape() ?: continue
                return s
            }
        }
        return null
    }

    val bodyEntered: Signal get() = signal("body_entered")
    val bodyExited: Signal get() = signal("body_exited")
    val areaEntered: Signal get() = signal("area_entered")
    val areaExited: Signal get() = signal("area_exited")

    /** World-space collision rect (approximate). */
    fun getCollisionRect(): Rect2? {
        val s = shape ?: return null
        return s.asRect().translated(globalPosition)
    }

    fun layerMatches(mask: Int): Boolean = (collisionLayer and mask) != 0
    fun maskMatches(layer: Int): Boolean = (collisionMask and layer) != 0

    /** Called by the physics server to apply a contact. Overridden by RigidBody2D. */
    open fun onContact(other: PhysicsBody2D, contactNormal: Vector2) {}

    /** Whether this body should be considered solid (static/rigid but not sensor). */
    open fun isSolid(): Boolean = true
}
