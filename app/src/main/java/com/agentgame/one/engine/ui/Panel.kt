package com.agentgame.one.engine.ui

import com.agentgame.one.engine.render.Color4

/** A simple panel container for grouping UI (Godot's `Panel` analogue). */
open class Panel(nodeName: String = "Panel") : Control(nodeName) {
    init {
        backgroundColor = Color4(0.08f, 0.09f, 0.14f, 0.85f)
        borderColor = Color4(0.35f, 0.4f, 0.6f, 0.6f)
        roundedCorners = 10f
    }

    fun panelColor(c: Color4): Panel { backgroundColor = c; return this }
}
