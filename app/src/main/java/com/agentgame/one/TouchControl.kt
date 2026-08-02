package com.agentgame.one

import com.jme3.input.RawInputListener
import com.jme3.input.TouchInput
import com.jme3.input.event.BufferedTouchEvent
import com.jme3.input.event.JoyAxisEvent
import com.jme3.input.event.JoyButtonEvent
import com.jme3.input.event.KeyInputEvent
import com.jme3.input.event.MouseButtonEvent
import com.jme3.input.event.MouseMotionEvent
import com.jme3.input.event.TouchEvent

/**
 * Touch controls for the game.
 *
 * jME3 reports touch coordinates as pixels with the origin at the BOTTOM-LEFT of the screen.
 *  - Left half of the screen: a virtual movement joystick (drag relative to where you touched).
 *  - Right half of the screen: aim (drag to rotate the camera) AND continuous auto-fire at the
 *    crosshair (matches "FIRE — AUTO SHOOT AT CROSSHAIR").
 */
class TouchControl(private val app: GameApp) : RawInputListener {

    var inForward = 0f      // -1..1 (positive = forward)
    var inRight = 0f        // -1..1 (positive = right)
    var yawDelta = 0f       // camera yaw to apply this frame
    var firing = false      // auto-fire active

    private var movePointerId = -1
    private var aimPointerId = -1
    private val moveCenter = FloatArray(2)
    private var aimLastX = 0f

    // Movement joystick dead-reach in pixels (drag distance for full speed).
    private val joyRadius: Float
        get() = app.cam.getWidth() * 0.14f

    init {
        app.inputManager.addRawInputListener(this)
    }

    /** Call after the player has consumed this frame's inputs. */
    fun endFrame() {
        yawDelta = 0f
    }

    override fun onTouchEvent(evt: TouchEvent) {
        val b = evt as BufferedTouchEvent
        val x = b.x
        val y = b.y
        val id = b.pointerId
        val w = app.cam.getWidth().toFloat()

        if (app.player.dead) {
            // After death, any tap returns to the lobby.
            if (b.type == TouchInput.InputType.DOWN) {
                app.quitFromGame()
            }
            return
        }

        when (b.type) {
            TouchInput.InputType.DOWN -> {
                if (x < w / 2f && movePointerId < 0) {
                    movePointerId = id
                    moveCenter[0] = x
                    moveCenter[1] = y
                } else if (x >= w / 2f && aimPointerId < 0) {
                    aimPointerId = id
                    aimLastX = x
                    firing = true
                }
            }
            TouchInput.InputType.UP -> {
                if (id == movePointerId) {
                    movePointerId = -1
                    inForward = 0f
                    inRight = 0f
                }
                if (id == aimPointerId) {
                    aimPointerId = -1
                    firing = false
                }
            }
            TouchInput.InputType.MOVE -> {
                if (id == movePointerId) {
                    // origin bottom-left: drag up increases y = forward, drag right = right
                    inRight = clamp((x - moveCenter[0]) / joyRadius, -1f, 1f)
                    inForward = clamp((y - moveCenter[1]) / joyRadius, -1f, 1f)
                } else if (id == aimPointerId) {
                    val dx = x - aimLastX
                    yawDelta += dx * 0.006f
                    aimLastX = x
                }
            }
            else -> {}
        }
    }

    private fun clamp(v: Float, lo: Float, hi: Float): Float =
        if (v < lo) lo else if (v > hi) hi else v

    // ---- unused RawInputListener methods ----
    override fun beginInput() {}
    override fun endInput() {}
    override fun onJoyAxisEvent(evt: JoyAxisEvent) {}
    override fun onJoyButtonEvent(evt: JoyButtonEvent) {}
    override fun onMouseMotionEvent(evt: MouseMotionEvent) {}
    override fun onMouseButtonEvent(evt: MouseButtonEvent) {}
    override fun onKeyEvent(evt: KeyInputEvent) {}
}
