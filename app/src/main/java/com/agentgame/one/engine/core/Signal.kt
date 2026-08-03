package com.agentgame.one.engine.core

/**
 * Godot-style signal system: a named, typed event a node can emit; other nodes (or lambdas)
 * can connect to it, keeping scenes loosely coupled. Backed by a dispatcher on the owning Node.
 */
class Signal(name: String, private val emitter: Node) {
    val signalName: String = name
    private val connections = mutableListOf<(Array<Any?>) -> Unit>()

    fun connect(callback: (Array<Any?>) -> Unit): Signal {
        connections.add(callback)
        return this
    }

    fun connect(target: Node, methodName: String): Signal {
        connections.add { args ->
            target.emitFromTarget(emitter, signalName, args)
        }
        return this
    }

    fun disconnect(callback: (Array<Any?>) -> Unit) {
        connections.remove(callback)
    }

    fun emit(vararg args: Any?) {
        // Copy so listeners can modify the tree without breaking iteration.
        val snapshot = connections.toList()
        for (cb in snapshot) {
            try {
                cb(arrayOf(*args))
            } catch (t: Throwable) {
                emitter.engine?.reportError("Error in signal '$signalName' handler: $t")
            }
        }
    }

    fun connectionCount(): Int = connections.size

    fun hasConnections(): Boolean = connections.isNotEmpty()
}
