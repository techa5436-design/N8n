package com.agentgame.one.engine.core

/**
 * Base class for 2D nodes (Godot's `Node2D` analogue). Everything positioned in a 2D world
 * derives from it. In this engine it is a thin typed alias over [CanvasItem], which already
 * carries position / rotation / scale.
 */
open class Node2D(nodeName: String = "Node2D") : CanvasItem(nodeName) {

    /** Convenience fluent setters for building scenes in code. */
    fun pos(x: Float, y: Float): Node2D { position = Vector2(x, y); return this }
    fun pos(p: Vector2): Node2D { position = p; return this }
    fun rot(radians: Float): Node2D { rotation = radians; return this }
    fun z(z: Int): Node2D { zIndex = z; return this }
    fun show(): Node2D { visible = true; return this }
    fun hide(): Node2D { visible = false; return this }
}
