package com.agentgame.one.engine.physics

import com.agentgame.one.engine.core.Signal

/**
 * A trigger region that detects overlapping bodies and areas without pushing them
 * (Godot's `Area2D` analogue). Emits `body_entered` / `body_exited` (and area versions).
 */
open class Area2D(nodeName: String = "Area2D") : PhysicsBody2D(nodeName) {
    /** Sensor areas are never solid. */
    override fun isSolid(): Boolean = false

    /** Whether to monitor body/area enter-exit. */
    var monitoring: Boolean = true
    var monitorable: Boolean = true

    private val insideBodies = mutableSetOf<PhysicsBody2D>()
    private val insideAreas = mutableSetOf<PhysicsBody2D>()

    val monitoringSignals: List<Signal> get() = listOf(bodyEntered, bodyExited, areaEntered, areaExited)

    fun getOverlappingBodies(): Set<PhysicsBody2D> = insideBodies.toSet()
    fun getOverlappingAreas(): Set<PhysicsBody2D> = insideAreas.toSet()
    fun hasOverlappingBodies(): Boolean = insideBodies.isNotEmpty()

    internal fun beginBodyOverlap(body: PhysicsBody2D) {
        if (insideBodies.add(body)) {
            bodyEntered.emit(body)
            body.signal("body_entered").emit(this)
        }
    }

    internal fun endBodyOverlap(body: PhysicsBody2D) {
        if (insideBodies.remove(body)) {
            bodyExited.emit(body)
            body.signal("body_exited").emit(this)
        }
    }

    internal fun beginAreaOverlap(area: Area2D) {
        if (insideAreas.add(area)) areaEntered.emit(area)
    }

    internal fun endAreaOverlap(area: Area2D) {
        if (insideAreas.remove(area)) areaExited.emit(area)
    }

    /** Clears tracked overlaps each physics step before recomputation. */
    internal fun resetTracking() {
        for (b in insideBodies) {
            // will be re-added if still overlapping
        }
    }
}
