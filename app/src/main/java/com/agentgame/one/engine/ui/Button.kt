package com.agentgame.one.engine.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.core.Signal
import com.agentgame.one.engine.core.Vector2
import com.agentgame.one.engine.render.Color4

/**
 * A tappable button (Godot's `Button` analogue). Emits a `pressed` signal and supports a pressed
 * visual state.
 */
open class Button(nodeName: String = "Button") : Label(nodeName) {

    var normalColor: Color4 = Color4(0.16f, 0.20f, 0.34f, 1f)
    var pressedColor: Color4 = Color4(0.30f, 0.38f, 0.62f, 1f)
    var hoverColor: Color4 = normalColor.darken(1.15f)

    var isPressed: Boolean = false
    var toggleMode: Boolean = false

    val pressedSignal: Signal get() = signal("pressed")
    val toggled: Signal get() = signal("toggled")

    init {
        signal("pressed")
        signal("toggled")
        backgroundColor = normalColor
    }

    override fun onPressed() {
        if (toggleMode) {
            isPressed = !isPressed
            toggled.emit(isPressed)
        }
        pressedSignal.emit()
    }

    fun setNormalColor(c: Color4): Button { normalColor = c; backgroundColor = c; return this }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        backgroundColor = if (isPressed) pressedColor else normalColor
        super.onDraw(canvas, paint)
    }
}
