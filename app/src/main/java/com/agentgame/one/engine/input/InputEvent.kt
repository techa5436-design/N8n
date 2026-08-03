package com.agentgame.one.engine.input

import com.agentgame.one.engine.core.Vector2

/** Base input event (Godot's `InputEvent` analogue). */
sealed class InputEvent {
    /** Whether the event was consumed by a node / UI. */
    var consumed: Boolean = false

    fun accept() { consumed = true }
}

/** A touch / pointer press, move or release in screen coordinates. */
class InputEventTouch(
    val action: Action,
    val pointerId: Int,
    val screenPos: Vector2,
    val pressure: Float = 1f,
) : InputEvent() {
    enum class Action { PRESSED, RELEASED, MOVED }
}

/** A button press (from on-screen touch buttons or physical keys). */
class InputEventAction(
    val actionName: String,
    val pressed: Boolean,
) : InputEvent()

/** A screen tap (translated from touch press). */
class InputEventTap(val screenPos: Vector2) : InputEvent()
