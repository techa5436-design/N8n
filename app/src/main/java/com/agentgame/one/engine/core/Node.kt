package com.agentgame.one.engine.core

import com.agentgame.one.engine.Engine
import com.agentgame.one.engine.scripting.NodeScript

/**
 * The base class of the engine's scene architecture (Godot's `Node` analogue).
 *
 * Every object in a game is a [Node] arranged in a tree. The tree controls processing order
 * (parents before children, depth-first), rendering order and signal propagation. Nodes have a
 * name, an optional script, signals, groups, and a lifecycle of callbacks:
 *
 *  - [onEnterTree]  — called when the node (and its subtree) is added to the tree.
 *  - [onReady]      — called once, after the whole scene has entered the tree.
 *  - [onProcess]    — called every frame.
 *  - [onPhysicsProcess] — called on the fixed physics tick.
 *  - [onExitTree]   — called when removed from the tree.
 */
open class Node(open var nodeName: String = "Node") {

    // ---- tree wiring -------------------------------------------------------
    var parent: Node? = null
    internal val _children = mutableListOf<Node>()
    val children: List<Node> get() = _children
    var owner: Node? = null

    /** The running scene tree this node belongs to (null until added). */
    var tree: SceneTree? = null
        internal set

    val engine: Engine? get() = tree?.engine

    /** Whether this node and its children keep processing when paused. */
    var processMode: Int = ProcessMode.INHERIT

    object ProcessMode {
        const val INHERIT = 0
        const val PAUSABLE = 1
        const val WHEN_PAUSED = 2
        const val ALWAYS = 3
        const val DISABLED = 4
    }

    // ---- groups & metadata -------------------------------------------------
    private val _groups = mutableSetOf<String>()
    val groups: Set<String> get() = _groups

    private val _meta = mutableMapOf<String, Any?>()
    val meta: Map<String, Any?> get() = _meta

    private val _signals = mutableMapOf<String, Signal>()
    val signals: Map<String, Signal> get() = _signals

    /** Script attached to this node (GDScript-like interpreter instance). */
    var script: NodeScript? = null

    /** Optional per-frame hook (convenience for quick logic without subclassing). */
    var processLambda: ((Float) -> Unit)? = null
    var physicsLambda: ((Float) -> Unit)? = null

    // ---- lifecycle hooks (override in subclasses) --------------------------
    open fun onEnterTree() {}
    open fun onReady() {}
    open fun onProcess(delta: Float) {
        processLambda?.invoke(delta)
    }
    open fun onPhysicsProcess(delta: Float) {
        physicsLambda?.invoke(delta)
    }
    open fun onExitTree() {}

    /** Called when an input event is delivered to this node (if it processes input). */
    open fun onInput(event: Any) {}

    // ---- child management --------------------------------------------------
    fun addChild(child: Node): Node {
        check(child.parent == null) { "Node '${child.nodeName}' already has a parent" }
        _children.add(child)
        child.parent = this
        child.tree = tree
        child.owner = owner
        child.notifyEnterTree()
        return child
    }

    fun addSibling(sibling: Node): Node {
        val p = parent ?: return this
        p._children.add(p._children.indexOf(this) + 1, sibling)
        sibling.parent = p
        sibling.tree = tree
        sibling.owner = owner
        sibling.notifyEnterTree()
        return sibling
    }

    fun removeChild(child: Node): Node {
        if (_children.remove(child)) {
            child.notifyExitTree()
            child.parent = null
            child.tree = null
            child.owner = null
        }
        return child
    }

    fun reparent(newParent: Node): Node {
        val old = parent
        old?.removeChild(this)
        newParent.addChild(this)
        return this
    }

    fun getChildren(): List<Node> = _children

    fun getParent(): Node? = parent

    fun getTree(): SceneTree? = tree

    /** Queues this node to be freed at the end of the current frame (deferred). */
    fun queueFree() {
        tree?.queueFree(this)
    }

    /** Immediately removes and discards this node and its subtree. */
    fun free() {
        parent?.removeChild(this)
    }

    /** Defers an action to the end of the current frame. */
    fun callDeferred(block: () -> Unit) {
        tree?.callDeferred(block)
    }

    internal fun notifyEnterTree() {
        tree?.enterRoot?.emit(this)
        onEnterTree()
        script?._onEnterTree()
        // copy children so addChild during processing is safe
        for (c in _children.toList()) {
            c.tree = tree
            c.notifyEnterTree()
        }
    }

    internal fun notifyExitTree() {
        onExitTree()
        script?._onExitTree()
        for (c in _children.toList()) c.notifyExitTree()
        tree?.exitRoot?.emit(this)
    }

    internal fun notifyReady() {
        script?._onReady()
        for (c in _children.toList()) c.notifyReady()
    }

    // ---- path lookup --------------------------------------------------------
    fun getNode(path: String): Node? {
        if (path == ".") return this
        if (path == "..") return parent
        if (path.startsWith("..")) {
            return parent?.getNode(path.removePrefix("../"))
        }
        if (path.startsWith("/root/")) {
            return tree?.root?.getNode(path.removePrefix("/root/"))
        }
        if (path.startsWith("/")) {
            return tree?.root?.getNode(path.removePrefix("/"))
        }
        // absolute path from this node
        if (path.startsWith(".")) {
            var current: Node? = this
            val parts = path.removePrefix(".").split("/").filter { it.isNotEmpty() }
            for (part in parts) {
                when (part) {
                    ".." -> current = current?.parent
                    else -> current = current?.findChildNode(part)
                }
                if (current == null) return null
            }
            return current
        }
        // relative: first segment may be a child name
        var current: Node? = this
        for (part in path.split("/")) {
            when (part) {
                "" -> continue
                "." -> continue
                ".." -> current = current?.parent
                else -> current = current?.findChildNode(part)
            }
            if (current == null) return null
        }
        return current
    }

    private fun findChildNode(name: String): Node? {
        for (c in _children) if (c.nodeName == name) return c
        return null
    }

    fun getNodeOrNull(path: String): Node? = getNode(path)

    inline fun <reified T : Node> getNodeAs(path: String): T? = getNode(path) as? T

    fun getChild(index: Int): Node = _children[index]
    fun getChildCount(): Int = _children.size

    fun findChild(pattern: String, recursive: Boolean = true, owned: Boolean = false): Node? {
        for (c in _children) {
            if (c.nodeName.matches(pattern.toRegex())) return c
            if (recursive) c.findChild(pattern, true, owned)?.let { return it }
        }
        return null
    }

    fun findChildren(pattern: String, recursive: Boolean = true, owned: Boolean = false): List<Node> {
        val out = mutableListOf<Node>()
        for (c in _children) {
            if (c.nodeName.matches(pattern.toRegex())) out.add(c)
            if (recursive) out.addAll(c.findChildren(pattern, true, owned))
        }
        return out
    }

    fun findParent(pattern: String): Node? {
        val regex = pattern.toRegex()
        var cur = parent
        while (cur != null) {
            if (regex.matches(cur.nodeName)) return cur
            cur = cur.parent
        }
        return null
    }

    fun getPath(): String {
        if (parent == null) return "/root/$nodeName"
        return parent!!.getPath() + "/" + nodeName
    }

    fun isAncestorOf(node: Node): Boolean {
        var cur: Node? = node.parent
        while (cur != null) {
            if (cur === this) return true
            cur = cur.parent
        }
        return false
    }

    fun isDescendantOf(node: Node): Boolean = node.isAncestorOf(this)
    fun isInsideTree(): Boolean = tree != null && isAncestorOf(this).not() || (parent != null && parent!!.isInsideTree())

    // ---- groups -------------------------------------------------------------
    fun addToGroup(group: String): Node {
        _groups.add(group)
        tree?.addToGroup(group, this)
        return this
    }

    fun removeFromGroup(group: String): Node {
        _groups.remove(group)
        tree?.removeFromGroup(group, this)
        return this
    }

    fun isInGroup(group: String): Boolean = _groups.contains(group)

    fun getNodesInGroup(group: String): List<Node> = tree?.getNodesInGroup(group) ?: emptyList()

    // ---- metadata ------------------------------------------------------------
    fun setMeta(key: String, value: Any?): Node {
        _meta[key] = value
        return this
    }

    fun getMeta(key: String, default: Any? = null): Any? = _meta[key] ?: default
    fun hasMeta(key: String): Boolean = _meta.containsKey(key)

    // ---- signals -------------------------------------------------------------
    fun signal(name: String): Signal {
        return _signals.getOrPut(name) { Signal(name, this) }
    }

    fun hasSignal(name: String): Boolean = _signals.containsKey(name)
    fun getSignalList(): List<String> = _signals.keys.toList()

    /** Called by a connected Signal when the target node's method should run. */
    internal fun emitFromTarget(source: Node, signalName: String, args: Array<Any?>) {
        // If this node has a script, try to call a method named "on_<signal>"
        script?.invokeSignalHandler(signalName, source, args)
    }

    // ---- property access (used by the script interpreter, tweens & engine tooling) ---
    /**
     * Reads a property by name. Looks in metadata first, then reflects on the real Kotlin
     * properties of this node (so `position`, `rotation`, `scale`, `visible`, `color`, ... work).
     */
    fun getProperty(name: String): Any? {
        if (_meta.containsKey(name)) return _meta[name]
        return reflectGet(name)
    }

    fun setProperty(name: String, value: Any?): Boolean {
        // If it maps to a real mutable Kotlin property, set it there.
        if (reflectSet(name, value)) return true
        _meta[name] = value
        return true
    }

    private fun reflectGet(name: String): Any? {
        val cls = javaClass
        // search up the hierarchy
        var k: kotlin.reflect.KClass<*>? = cls.kotlin
        while (k != null) {
            val prop = k.memberProperties.firstOrNull { it.name == name }
            if (prop != null && prop is kotlin.reflect.KProperty1<*, *>) {
                @Suppress("UNCHECKED_CAST")
                return (prop as kotlin.reflect.KProperty1<Any, Any?>).get(this)
            }
            k = k.superclass
        }
        return null
    }

    private fun reflectSet(name: String, value: Any?): Boolean {
        var k: kotlin.reflect.KClass<*>? = javaClass.kotlin
        while (k != null) {
            val prop = k.memberProperties.firstOrNull { it.name == name }
            if (prop != null && prop is kotlin.reflect.KMutableProperty1<*, *>) {
                @Suppress("UNCHECKED_CAST")
                (prop as kotlin.reflect.KMutableProperty1<Any, Any?>).setter.call(this, value)
                return true
            }
            k = k.superclass
        }
        return false
    }

    // ---- timer helpers ---------------------------------------------------------
    fun getTimer(): Timer {
        var t = findChildNode("Timer") as? Timer
        if (t == null) {
            t = Timer().apply { nodeName = "Timer" }
            addChild(t)
        }
        return t
    }
}
