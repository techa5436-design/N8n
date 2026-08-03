package com.agentgame.one.engine.scripting

import com.agentgame.one.engine.core.Node

/**
 * Interface implemented by script instances attached to a [com.agentgame.one.engine.core.Node].
 * The engine invokes these hooks at the right points in the node lifecycle, mirroring GDScript's
 * `_ready`, `_process`, `_physics_process`, etc.
 */
interface NodeScript {
    fun _onEnterTree() {}
    fun _onReady() {}
    fun _onExitTree() {}
    fun onProcess(delta: Float) {}
    fun onPhysicsProcess(delta: Float) {}
    fun onInput(event: Any) {}

    /** Route a signal emission to a handler like `on_timeout` / `on_body_entered`. */
    fun invokeSignalHandler(signalName: String, source: Node, args: Array<Any?>)

    /** The raw source of this script (used when serializing scenes). */
    fun sourceCode(): String
}
