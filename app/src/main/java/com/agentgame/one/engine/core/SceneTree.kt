package com.agentgame.one.engine.core

import com.agentgame.one.engine.Engine
import com.agentgame.one.engine.physics.PhysicsServer2D

/**
 * Manages the current scene tree (Godot's `SceneTree` analogue).
 *
 * It is the root of the node hierarchy, drives the per-frame [processFrame] and fixed
 * [physicsFrame] steps (parents before children, depth-first), tracks groups, and defers
 * node freeing / actions until the end of the frame.
 */
class SceneTree(val engine: Engine) {

    val root: Node = Node("Root").apply { tree = this@SceneTree }

    private val _groupRegistry = mutableMapOf<String, MutableSet<Node>>()
    private val _deferred = ArrayDeque<() -> Unit>()
    private val _toFree = mutableListOf<Node>()

    val physics: PhysicsServer2D = PhysicsServer2D(this)

    // Signals emitted on tree lifecycle (root changes).
    private val _emitter = Node("__treeEmitter__")
    val enterRoot: Signal = _emitter.signal("tree_entered")
    val exitRoot: Signal = _emitter.signal("tree_exited")

    var paused: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                _emitter.signal("paused").emit(value)
            }
        }
    var pausedSignal: Signal get() = _emitter.signal("paused")

    private var currentFrameTime = 0.0

    // ---- frame stepping ------------------------------------------------------
    /** Runs one render/process frame. [delta] is the elapsed wall time in seconds. */
    fun processFrame(delta: Float) {
        if (!paused) {
            processNode(root, delta, ProcessMode.INHERIT)
        } else {
            // still process WHEN_PAUSED nodes
            processNodePausedOnly(root, delta)
        }
        flushDeferred()
    }

    /** Runs one fixed physics step. */
    fun physicsFrame(delta: Float) {
        if (!paused) {
            physicsNode(root, delta, ProcessMode.INHERIT)
            physics.step(delta)
        } else {
            physicsNodePausedOnly(root, delta)
        }
        flushDeferred()
    }

    private fun nodeProcesses(node: Node, mode: Int): Boolean {
        val effective = resolveProcessMode(node, mode)
        return when (effective) {
            Node.ProcessMode.ALWAYS, Node.ProcessMode.WHEN_PAUSED -> true
            else -> false
        }
    }

    private fun resolveProcessMode(node: Node, inherited: Int): Int {
        return when (node.processMode) {
            Node.ProcessMode.INHERIT -> inherited
            Node.ProcessMode.DISABLED -> Node.ProcessMode.DISABLED
            else -> node.processMode
        }
    }

    private fun processNode(node: Node, delta: Float, mode: Int) {
        if (node.processMode == Node.ProcessMode.DISABLED) return
        val effective = resolveProcessMode(node, mode)
        val allowed = effective != Node.ProcessMode.WHEN_PAUSED
        if (allowed) {
            try {
                node.onProcess(delta)
                node.script?.onProcess(delta)
            } catch (t: Throwable) {
                engine.reportError("Error processing '${node.getPath()}': $t")
            }
        }
        for (c in node._children) processNode(c, delta, effective)
    }

    private fun processNodePausedOnly(node: Node, delta: Float) {
        if (node.processMode == Node.ProcessMode.DISABLED) return
        val effective = resolveProcessMode(node, Node.ProcessMode.INHERIT)
        if (effective == Node.ProcessMode.ALWAYS || effective == Node.ProcessMode.WHEN_PAUSED) {
            try {
                node.onProcess(delta)
                node.script?.onProcess(delta)
            } catch (t: Throwable) {
                engine.reportError("Error processing '${node.getPath()}': $t")
            }
        }
        for (c in node._children) processNodePausedOnly(c, delta)
    }

    private fun physicsNode(node: Node, delta: Float, mode: Int) {
        if (node.processMode == Node.ProcessMode.DISABLED) return
        val effective = resolveProcessMode(node, mode)
        val allowed = effective != Node.ProcessMode.WHEN_PAUSED
        if (allowed) {
            try {
                node.onPhysicsProcess(delta)
                node.script?.onPhysicsProcess(delta)
            } catch (t: Throwable) {
                engine.reportError("Error in physics for '${node.getPath()}': $t")
            }
        }
        for (c in node._children) physicsNode(c, delta, effective)
    }

    private fun physicsNodePausedOnly(node: Node, delta: Float) {
        if (node.processMode == Node.ProcessMode.DISABLED) return
        val effective = resolveProcessMode(node, Node.ProcessMode.INHERIT)
        if (effective == Node.ProcessMode.ALWAYS || effective == Node.ProcessMode.WHEN_PAUSED) {
            try {
                node.onPhysicsProcess(delta)
                node.script?.onPhysicsProcess(delta)
            } catch (t: Throwable) {
                engine.reportError("Error in physics for '${node.getPath()}': $t")
            }
        }
        for (c in node._children) physicsNodePausedOnly(c, delta)
    }

    // ---- deferred / freeing ----------------------------------------------------
    fun callDeferred(block: () -> Unit) {
        _deferred.addLast(block)
    }

    fun queueFree(node: Node) {
        _toFree.add(node)
    }

    private fun flushDeferred() {
        while (_deferred.isNotEmpty()) {
            val next = _deferred.removeFirst()
            try {
                next()
            } catch (t: Throwable) {
                engine.reportError("Deferred call error: $t")
            }
        }
        if (_toFree.isNotEmpty()) {
            for (n in _toFree) n.free()
            _toFree.clear()
        }
    }

    // ---- groups --------------------------------------------------------------
    fun addToGroup(group: String, node: Node) {
        _groupRegistry.getOrPut(group) { mutableSetOf() }.add(node)
    }

    fun removeFromGroup(group: String, node: Node) {
        _groupRegistry[group]?.remove(node)
    }

    fun getNodesInGroup(group: String): List<Node> =
        _groupRegistry[group]?.toList() ?: emptyList()

    fun hasGroup(group: String): Boolean = _groupRegistry.containsKey(group) && _groupRegistry[group]!!.isNotEmpty()

    fun getGroups(): Set<String> = _groupRegistry.keys.toSet()

    // ---- tree utilities --------------------------------------------------------
    fun getCurrentScene(): Node? {
        return root.children.firstOrNull { it.hasMeta("is_scene_root") }
    }

    fun changeSceneTo(node: Node) {
        val current = getCurrentScene()
        if (current != null) root.removeChild(current)
        node.setMeta("is_scene_root", true)
        root.addChild(node)
        node.notifyReady()
    }

    fun rootNode(): Node = root

    fun frameTime(): Double = currentFrameTime

    // ---- pointer dispatch to UI controls --------------------------------------
    /**
     * Routes a touch to the topmost [com.agentgame.one.engine.ui.Control] under the pointer
     * (Godot-style GUI input). Returns true if a control consumed the event.
     */
    fun dispatchTouch(event: com.agentgame.one.engine.input.InputEventTouch): Boolean {
        val controls = mutableListOf<Pair<Int, com.agentgame.one.engine.ui.Control>>()
        collectControls(root, controls)
        controls.sortByDescending { it.first }
        for ((z, c) in controls) {
            if (!c.visible || !c.canReceivePointer()) continue
            if (c.globalRect.contains(event.screenPos)) {
                val consumed = when (event.action) {
                    com.agentgame.one.engine.input.InputEventTouch.Action.PRESSED -> c.onPointerDown(event.screenPos)
                    else -> c.onPointerUp(event.screenPos)
                }
                if (consumed) {
                    event.accept()
                    return true
                }
            }
        }
        return false
    }

    private fun collectControls(node: Node, out: MutableList<Pair<Int, com.agentgame.one.engine.ui.Control>>) {
        if (node is com.agentgame.one.engine.ui.Control) {
            out.add(Pair(node.zIndex, node))
        }
        for (c in node._children) collectControls(c, out)
    }
}
