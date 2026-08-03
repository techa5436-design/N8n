package com.agentgame.one.engine.input

import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.core.Vector2

/**
 * Singleton input manager (Godot's `Input` singleton analogue).
 *
 * Tracks action buttons, virtual joysticks (from touch) and routes raw touch/event data into
 * actions that game code can query via [isActionPressed] / [isActionJustPressed].
 */
object Input {

    /** An action maps to an on-screen button region and/or a joystick zone. */
    data class ActionDef(
        val name: String,
        var buttonRect: Rect2? = null,
        var joystickZone: Rect2? = null,
        var enabled: Boolean = true,
    )

    private val actions = mutableMapOf<String, ActionDef>()

    /** Region of the screen where a touch becomes a virtual joystick (e.g. left half). */
    var joystickZone: Rect2? = null
    private val pressed = mutableSetOf<String>()
    private val justPressed = mutableSetOf<String>()
    private val justReleased = mutableSetOf<String>()

    // virtual joysticks: id -> origin, currentVector
    data class Stick(val id: Int, var origin: Vector2, var vector: Vector2 = Vector2.ZERO)
    val sticks = mutableMapOf<Int, Stick>()

    private val activePointers = mutableMapOf<Int, Vector2>()

    fun registerAction(name: String, buttonRect: Rect2? = null, joystickZone: Rect2? = null): ActionDef {
        return actions.getOrPut(name) { ActionDef(name, buttonRect, joystickZone) }
    }

    fun isActionPressed(name: String): Boolean = pressed.contains(name)
    fun isActionJustPressed(name: String): Boolean = justPressed.contains(name)
    fun isActionJustReleased(name: String): Boolean = justReleased.contains(name)

    fun actionNames(): Set<String> = actions.keys.toSet()

    fun enableAction(name: String, enabled: Boolean) {
        actions[name]?.enabled = enabled
        if (!enabled) pressed.remove(name)
    }

    /** Feeds raw touch data. [screenPos] is in screen pixels (y-down). */
    fun feedTouch(event: InputEventTouch) {
        when (event.action) {
            InputEventTouch.Action.PRESSED -> {
                activePointers[event.pointerId] = event.screenPos
                val zone = joystickZone
                if (zone != null && zone.contains(event.screenPos) && sticks[event.pointerId] == null) {
                    sticks[event.pointerId] = Stick(event.pointerId, event.screenPos)
                }
                val stick = sticks[event.pointerId]
                if (stick != null) {
                    stick.origin = event.screenPos
                    stick.vector = Vector2.ZERO
                }
                handleButtonHit(event.screenPos)
            }
            InputEventTouch.Action.MOVED -> {
                val start = activePointers[event.pointerId] ?: return
                activePointers[event.pointerId] = event.screenPos
                val stick = sticks[event.pointerId]
                if (stick != null) {
                    val v = event.screenPos - stick.origin
                    stick.vector = (v / STICK_RADIUS).clamped(1f)
                    updateStickActions(stick)
                }
            }
            InputEventTouch.Action.RELEASED -> {
                activePointers.remove(event.pointerId)
                val stick = sticks.remove(event.pointerId)
                if (stick != null) {
                    stick.vector = Vector2.ZERO
                    updateStickActions(stick)
                }
                releaseButtons(event.screenPos)
            }
        }
    }

    private fun handleButtonHit(pos: Vector2) {
        for ((name, def) in actions) {
            val rect = def.buttonRect ?: continue
            if (def.enabled && rect.contains(pos)) {
                if (pressed.add(name)) justPressed.add(name)
            }
        }
    }

    private fun releaseButtons(pos: Vector2) {
        for ((name, def) in actions) {
            val rect = def.buttonRect ?: continue
            if (rect.contains(pos)) {
                if (pressed.remove(name)) justReleased.add(name)
            }
        }
    }

    private fun updateStickActions(stick: Stick) {
        val dx = stick.vector.x
        val dy = stick.vector.y
        setStickAction("ui_left", dx < -0.3f)
        setStickAction("ui_right", dx > 0.3f)
        setStickAction("ui_up", dy < -0.3f)
        setStickAction("ui_down", dy > 0.3f)
    }

    private fun setStickAction(name: String, value: Boolean) {
        if (value) { if (pressed.add(name)) justPressed.add(name) }
        else { if (pressed.remove(name)) justReleased.add(name) }
    }

    /** Simulates a button press (used by on-screen Button nodes / test harness). */
    fun actionPress(name: String) {
        if (pressed.add(name)) justPressed.add(name)
    }

    fun actionRelease(name: String) {
        if (pressed.remove(name)) justReleased.add(name)
    }

    /** Gets the joystick vector for a pointer id (or the union of active sticks). */
    fun joystickVector(id: Int? = null): Vector2 {
        if (id != null) return sticks[id]?.vector ?: Vector2.ZERO
        var out = Vector2.ZERO
        for (s in sticks.values) out += s.vector
        return out.clamped(1f)
    }

    /** Called once per frame to flush the just-pressed sets. */
    fun endFrame() {
        justPressed.clear()
        justReleased.clear()
    }

    fun reset() {
        pressed.clear(); justPressed.clear(); justReleased.clear(); sticks.clear(); activePointers.clear()
    }

    /** Sets which pointer ids act as virtual joysticks (left half of screen by default). */
    fun registerJoystickZone(zone: Rect2) {
        joystickZone = zone
    }

    private const val STICK_RADIUS = 60f
}
