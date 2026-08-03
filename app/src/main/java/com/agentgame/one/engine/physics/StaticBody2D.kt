package com.agentgame.one.engine.physics

/**
 * A non-moving, solid collision body — floors, walls, platforms (Godot's `StaticBody2D` analogue).
 */
open class StaticBody2D(nodeName: String = "StaticBody2D") : PhysicsBody2D(nodeName) {
    /** If true, collisions only push the other body out (used for one-way platforms). */
    var oneWay: Boolean = false

    override fun isSolid(): Boolean = true
}
