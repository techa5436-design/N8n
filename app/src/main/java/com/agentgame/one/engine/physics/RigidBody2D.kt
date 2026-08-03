package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Signal
import com.agentgame.one.engine.core.Vector2

/**
 * A physics body with velocity, mass and gravity that moves and collides (Godot's
 * `RigidBody2D` analogue). The physics server integrates its velocity each tick and resolves
 * collisions against solid bodies.
 */
open class RigidBody2D(nodeName: String = "RigidBody2D") : PhysicsBody2D(nodeName) {

    var velocity: Vector2 = Vector2.ZERO
    var angularVelocity: Float = 0f
    var mass: Float = 1f
    var gravityScale: Float = 1f
    var linearDamp: Float = 0f
    var lockRotation: Boolean = false
    var freeze: Boolean = false
    var freezeMode: Int = FreezeMode.STATIC

    object FreezeMode {
        const val STATIC = 0
        const val KINEMATIC = 1
    }

    /** Optional label the server can skip resolving for (e.g. bullets). */
    var sensor: Boolean = false

    val collided: Signal get() = signal("collided")
    val sleepStarted: Signal get() = signal("sleeping_started")

    val isOnFloor: Boolean get() = _onFloor
    val isOnWall: Boolean get() = _onWall
    val isOnCeiling: Boolean get() = _onCeiling

    internal var _onFloor = false
    internal var _onWall = false
    internal var _onCeiling = false

    /** Apply a force (mass-weighted impulse) over the next physics step. */
    fun applyImpulse(impulse: Vector2) {
        if (freeze) return
        velocity += impulse / mass.coerceAtLeast(0.0001f)
    }

    fun applyForce(force: Vector2, delta: Float) {
        if (freeze) return
        velocity += force / mass.coerceAtLeast(0.0001f) * delta
    }

    fun addForceAtPosition(force: Vector2, position: Vector2, delta: Float) {
        applyForce(force, delta)
    }

    fun moveAndSlide() {
        // Kinematic-style: velocity persists; the server integrates it.
    }

    fun moveAndCollide(linearVelocity: Vector2): Vector2 {
        // Immediate move (kinematic) without server resolution this tick.
        val old = position
        position = position + linearVelocity
        return position - old
    }

    fun getLinearVelocity(): Vector2 = velocity

    override fun onContact(other: PhysicsBody2D, contactNormal: Vector2) {
        _onFloor = contactNormal.y > 0.5f
        _onCeiling = contactNormal.y < -0.5f
        _onWall = kotlin.math.abs(contactNormal.x) > 0.5f
        collided.emit(other)
        bodyEntered.emit(other)
    }

    override fun isSolid(): Boolean = !sensor
}
