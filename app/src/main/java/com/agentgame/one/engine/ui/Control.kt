package com.agentgame.one.engine.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.core.CanvasItem
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Vector2
import com.agentgame.one.engine.input.InputEventTouch
import com.agentgame.one.engine.render.Color4

/**
 * Base class for UI controls (Godot's `Control` analogue). Controls are positioned in screen
 * space, have a size, can clip children, and receive pointer events. Subclasses draw themselves
 * in [onDraw].
 */
open class Control(nodeName: String = "Control") : CanvasItem(nodeName) {

    override var screenSpace: Boolean = true

    var size: Vector2 = Vector2(200f, 50f)
    var backgroundColor: Color4 = Color4.TRANSPARENT
    var borderColor: Color4? = null
    var borderWidth: Float = 2f
    var roundedCorners: Float = 0f
    var mouseFilter: MouseFilter = MouseFilter.STOP

    enum class MouseFilter { STOP, PASS, IGNORE }

    /** Whether pointer events land on this control (STOP or PASS). */
    fun canReceivePointer(): Boolean = mouseFilter != MouseFilter.IGNORE

    val globalRect: Rect2 get() = Rect2(computeGlobalTransform().origin.x, computeGlobalTransform().origin.y, size.x, size.y)

    /** Called when a touch press lands on this control. Return true to consume. */
    open fun onPointerDown(screenPos: Vector2): Boolean {
        onPressed()
        return mouseFilter == MouseFilter.STOP
    }

    open fun onPointerUp(screenPos: Vector2): Boolean = true
    open fun onPressed() {}

    /** Sets position keeping it as the top-left corner (screen space). */
    fun setPosition(x: Float, y: Float): Control {
        position = Vector2(x, y)
        return this
    }

    fun setSize(w: Float, h: Float): Control {
        size = Vector2(w, h)
        return this
    }

    fun setSize(s: Vector2): Control { size = s; return this }

    fun setBg(color: Color4): Control { backgroundColor = color; return this }

    override fun computeLocalRect(global: com.agentgame.one.engine.core.Transform2D): Rect2 =
        Rect2(global.origin.x, global.origin.y, size.x, size.y)

    override fun onDraw(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.alpha = (modulate.a * 255).toInt()
        if (backgroundColor.a > 0f) {
            paint.color = modulate.times(backgroundColor).toArgb()
            if (roundedCorners > 0f) {
                canvas.drawRoundRect(0f, 0f, size.x, size.y, roundedCorners, roundedCorners, paint)
            } else {
                canvas.drawRect(0f, 0f, size.x, size.y, paint)
            }
        }
        borderColor?.let { bc ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = modulate.times(bc).toArgb()
            if (roundedCorners > 0f) {
                canvas.drawRoundRect(0f, 0f, size.x, size.y, roundedCorners, roundedCorners, paint)
            } else {
                canvas.drawRect(0f, 0f, size.x, size.y, paint)
            }
        }
    }
}
