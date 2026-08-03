package com.agentgame.one.engine.core

import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.render.Color4

/**
 * Base class for everything that is visible and drawable (Godot's `CanvasItem` analogue).
 *
 * Holds a local transform (position / rotation / scale), a z-index, visibility, colour
 * modulation, and a [onDraw] that the render server invokes with a ready-to-use Canvas.
 */
open class CanvasItem(nodeName: String = "CanvasItem") : Node(nodeName) {

    var visible: Boolean = true
    var zIndex: Int = 0
    var showBehindParent: Boolean = false

    /**
     * When true, the item is drawn in screen space (ignoring the active camera). UI Controls
     * default to true; world Node2D items default to false.
     */
    open var screenSpace: Boolean = false

    /** Modulate (tint + alpha) applied when drawing. */
    var modulate: Color4 = Color4.WHITE

    // ---- transform (Godot: Node2D.position/rotation/scale live here too) ------
    var position: Vector2 = Vector2.ZERO
    var rotation: Float = 0f   // radians
    var scale: Vector2 = Vector2.ONE

    /** Local transform from position/rotation/scale. */
    fun localTransform(): Transform2D = Transform2D.of(position, rotation, scale)

    /** World transform, computed by walking up the parent chain. */
    fun computeGlobalTransform(): Transform2D {
        var t = localTransform()
        var p = parent
        while (p is CanvasItem) {
            t = p.localTransform().multipliedBy(t)
            p = p.parent
        }
        return t
    }

    val globalPosition: Vector2 get() = computeGlobalTransform().origin
    val globalRotation: Float get() {
        val g = computeGlobalTransform()
        return kotlin.math.atan2(g.xAxis.y, g.xAxis.x)
    }

    /** World-space rectangle (axis aligned) used for culling / hit tests. */
    fun getGlobalRect(): Rect2 {
        val g = computeGlobalTransform()
        return Rect2(g.origin.x, g.origin.y, 0f, 0f).expanded(computeLocalRect(g))
    }

    protected open fun computeLocalRect(global: Transform2D): Rect2 = Rect2()

    /** Concrete drawables override this. [paint] is a scratch paint you may mutate. */
    protected open fun onDraw(canvas: Canvas, paint: Paint) {}

    fun queueRedraw() {}

    // ---- helpers used by scripts / engine -------------------------------------
    fun globalPositionToLocal(p: Vector2): Vector2 = computeGlobalTransform().apply(p)
    fun localPositionToGlobal(p: Vector2): Vector2 = computeGlobalTransform().apply(p)
}
