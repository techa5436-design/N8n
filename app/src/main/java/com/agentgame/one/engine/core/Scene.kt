package com.agentgame.one.engine.core

import com.agentgame.one.engine.physics.Area2D
import com.agentgame.one.engine.physics.CollisionShape2D
import com.agentgame.one.engine.physics.RigidBody2D
import com.agentgame.one.engine.physics.StaticBody2D
import com.agentgame.one.engine.ui.Button
import com.agentgame.one.engine.ui.Label
import com.agentgame.one.engine.ui.Panel

/**
 * Registry that creates a fresh node instance from a type name. Used by the scene text loader
 * (Godot's built-in node type registry analogue).
 */
object NodeFactory {
    private val map = mutableMapOf<String, () -> Node>()

    fun register(typeName: String, factory: () -> Node) {
        map[typeName] = factory
    }

    fun create(typeName: String): Node? = map[typeName]?.invoke()

    init {
        register("Node") { Node() }
        register("CanvasItem") { CanvasItem() }
        register("Node2D") { Node2D() }
        register("Sprite2D") { Sprite2D() }
        register("Polygon2D") { Polygon2D() }
        register("Camera2D") { Camera2D() }
        register("Timer") { Timer() }
        register("Tween") { Tween() }
        register("StaticBody2D") { StaticBody2D() }
        register("RigidBody2D") { RigidBody2D() }
        register("Area2D") { Area2D() }
        register("CollisionShape2D") { CollisionShape2D() }
        register("Label") { Label() }
        register("Button") { Button() }
        register("Panel") { Panel() }
    }
}

/**
 * A reusable, saveable scene template (Godot's `PackedScene` analogue). A scene is a subtree of
 * nodes that can be instanced (duplicated) many times, and saved to / loaded from a `.tscn` text
 * file that the AI agent can also edit.
 */
class PackedScene private constructor(
    private val builder: (() -> Node)?,
    private var text: String?,
    val resourcePath: String,
) {
    /** Instantiates a fresh copy of the scene subtree. Safe to call many times. */
    fun instantiate(): Node {
        val b = builder
        if (b != null) return b()
        return parseText(text ?: return Node())
    }

    fun saveToFile(): String {
        val root = builder?.invoke()
        text = root?.let { SceneIO.serialize(it) } ?: text
        return text ?: ""
    }

    companion object {
        /** Wraps a builder function into a PackedScene. */
        fun fromBuilder(resourcePath: String = "", builder: () -> Node): PackedScene {
            return PackedScene(builder, null, resourcePath)
        }

        /** Loads a PackedScene from `.tscn` text. */
        fun fromText(text: String, resourcePath: String = ""): PackedScene {
            return PackedScene(null, text, resourcePath)
        }

        fun load(path: String, text: String): PackedScene = fromText(text, "res://$path")
    }

    private fun parseText(txt: String): Node {
        return SceneIO.deserialize(txt) ?: Node()
    }
}

/**
 * Reads/writes a node tree to a compact `.tscn`-style text format:
 *   [node type="Sprite2D" name="Player"]
 *   position = Vector2(10, 20)
 *   color = Color4(1, 0, 0, 1)
 *   // script = "..."
 */
object SceneIO {

    private val CLASSIFY = mapOf(
        "position" to "vector", "scale" to "vector", "size" to "vector", "offset" to "vector",
        "color" to "color", "modulate" to "color", "borderColor" to "color",
        "visible" to "bool", "oneShot" to "bool", "autostart" to "bool",
        "waitTime" to "float", "zoom" to "float", "rotation" to "float",
        "zIndex" to "int", "fontSize" to "int",
    )

    fun serialize(root: Node): String {
        val sb = StringBuilder()
        sb.append("[scene]\n")
        writeNode(sb, root, null)
        return sb.toString()
    }

    private fun writeNode(sb: StringBuilder, node: Node, parentName: String?) {
        val parentAttr = if (parentName != null) " parent=\"$parentName\"" else ""
        sb.append("[node type=\"${node.javaClass.simpleName}\" name=\"${node.nodeName}\"$parentAttr]\n")
        writeProps(sb, node)
        // script block (attached scripts serialize their source)
        node.script?.let {
            val escaped = it.sourceCode().replace("\"", "\\\"")
            sb.append("script = \"\"\"$escaped\"\"\"\n")
        }
        sb.append("\n")
        for (c in node._children) {
            writeNode(sb, c, node.nodeName)
        }
    }

    private fun writeProps(sb: StringBuilder, node: Node) {
        // curated built-ins
        writeVectorProp(sb, "position", node.getProperty("position"))
        writeFloatProp(sb, "rotation", node.getProperty("rotation"))
        writeVectorProp(sb, "scale", node.getProperty("scale"))
        writeBoolProp(sb, "visible", node.getProperty("visible"))
        writeIntProp(sb, "zIndex", node.getProperty("zIndex"))
        writeColorProp(sb, "color", node.getProperty("color"))
        writeFloatProp(sb, "waitTime", node.getProperty("waitTime"))
        writeBoolProp(sb, "oneShot", node.getProperty("oneShot"))
        // meta extras (script-written values) that aren't core props
        for ((k, v) in node.meta) {
            if (k in CLASSIFY) continue
            writeValue(sb, k, v)
        }
    }

    private fun writeVectorProp(sb: StringBuilder, name: String, v: Any?) {
        if (v is Vector2) sb.append("$name = Vector2(${v.x}, ${v.y})\n")
    }

    private fun writeFloatProp(sb: StringBuilder, name: String, v: Any?) {
        if (v is Float) sb.append("$name = ${v}\n")
        if (v is Int) sb.append("$name = ${v}.0\n")
    }

    private fun writeIntProp(sb: StringBuilder, name: String, v: Any?) {
        if (v is Int) sb.append("$name = ${v}\n")
    }

    private fun writeBoolProp(sb: StringBuilder, name: String, v: Any?) {
        if (v is Boolean) sb.append("$name = ${v}\n")
    }

    private fun writeColorProp(sb: StringBuilder, name: String, v: Any?) {
        if (v is com.agentgame.one.engine.render.Color4) sb.append("$name = Color4(${v.r}, ${v.g}, ${v.b}, ${v.a})\n")
    }

    private fun writeValue(sb: StringBuilder, name: String, v: Any?) {
        when (v) {
            is Vector2 -> sb.append("$name = Vector2(${v.x}, ${v.y})\n")
            is Float -> sb.append("$name = ${v}\n")
            is Int -> sb.append("$name = ${v}\n")
            is Boolean -> sb.append("$name = ${v}\n")
            is String -> sb.append("$name = \"${v}\"\n")
            is com.agentgame.one.engine.render.Color4 -> sb.append("$name = Color4(${v.r}, ${v.g}, ${v.b}, ${v.a})\n")
            else -> {}
        }
    }

    /** Parses `.tscn` text back into a node tree. */
    fun deserialize(text: String): Node? {
        val root = Node()
        val nodeById = mutableMapOf<String, Node>()
        val pending = mutableListOf<Pair<Node, String?>>()
        var currentNode: Node? = null

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            when {
                line.startsWith("[scene]") -> {}
                line.startsWith("[node ") -> {
                    val type = extractAttr(line, "type") ?: "Node"
                    val name = extractAttr(line, "name") ?: "Node"
                    val parent = extractAttr(line, "parent")
                    val node = NodeFactory.create(type) ?: Node()
                    node.nodeName = name
                    pending.add(Pair(node, parent))
                    nodeById[name] = node
                    currentNode = node
                }
                line.contains("=") && currentNode != null -> {
                    val idx = line.indexOf('=')
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).trim()
                    applyProperty(currentNode, key, value)
                }
            }
        }
        // resolve parents (may be paths like "Main/Player") after all nodes known
        for ((node, parentStr) in pending) {
            if (parentStr == null) {
                root.addChild(node)
            } else {
                val parentNode = resolveByPath(nodeById, parentStr) ?: root
                parentNode.addChild(node)
            }
        }
        return root
    }

    private fun resolveByPath(byName: Map<String, Node>, path: String): Node? {
        var current: Node? = null
        for (segment in path.split('/')) {
            if (segment.isEmpty()) continue
            val candidate = byName[segment] ?: return null
            // start of the path -> current = candidate
            if (current == null) { current = candidate; continue }
            // subsequent segment: expect candidate to be a child of current
            current = current.children.firstOrNull { it === candidate } ?: return null
        }
        return current
    }

    private fun extractAttr(line: String, attr: String): String? {
        val regex = Regex("$attr=\"([^\"]*)\"")
        return regex.find(line)?.groupValues?.get(1)
    }

    private fun applyProperty(node: Node, key: String, value: String) {
        when {
            value.startsWith("Vector2(") -> {
                val (x, y) = parseFloats(value)
                node.setProperty(key, Vector2(x, y))
            }
            value.startsWith("Color4(") -> {
                val parts = parseFloats4(value)
                node.setProperty(key, com.agentgame.one.engine.render.Color4(parts[0], parts[1], parts[2], parts[3]))
            }
            value == "true" -> node.setProperty(key, true)
            value == "false" -> node.setProperty(key, false)
            value.startsWith("\"") -> node.setProperty(key, value.trim('"'))
            else -> {
                val num = value.toFloatOrNull()
                if (num != null) {
                    if (value.contains('.')) node.setProperty(key, num) else node.setProperty(key, num.toInt())
                }
            }
        }
    }

    private fun parseFloats(s: String): Pair<Float, Float> {
        val parts = parseFloats4(s)
        return Pair(parts[0], parts[1])
    }

    private fun parseFloats4(s: String): List<Float> {
        val body = s.substringAfter('(').substringBefore(')')
        return body.split(',').map { it.trim().toFloat() }
    }
}
