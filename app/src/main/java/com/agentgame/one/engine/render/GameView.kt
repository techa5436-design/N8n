package com.agentgame.one.engine.render

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.agentgame.one.engine.Engine
import com.agentgame.one.engine.core.Vector2
import com.agentgame.one.engine.input.InputEventTouch
import kotlin.concurrent.thread

/**
 * An Android [SurfaceView] that hosts the engine's render loop on its own thread.
 * It drives [Engine.onFrame] with the locked canvas and forwards touch events to the
 * engine's input system.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : SurfaceView(context, attrs, defStyle), SurfaceHolder.Callback {

    var engine: Engine? = null

    private var running = false
    private var thread: Thread? = null

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        thread = thread(name = "engine-loop", isDaemon = true) { loop() }
    }

    private fun stopLoop() {
        running = false
        thread?.interrupt()
        thread = null
    }

    private fun loop() {
        var last = System.nanoTime()
        val targetFrameNs = 16_666_667L // ~60 fps
        while (running) {
            val now = System.nanoTime()
            val delta = (now - last) / 1_000_000_000f
            last = now
            val eng = engine
            if (eng != null && holder.surface.isValid && width > 0 && height > 0) {
                val canvas = holder.lockCanvas() ?: continue
                try {
                    eng.onFrame(canvas, width, height, delta.coerceAtMost(0.1f))
                } catch (t: Throwable) {
                    eng.reportError("Frame error: $t")
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            // cap frame rate
            val frameCost = System.nanoTime() - now
            val sleepMs = (targetFrameNs - frameCost) / 1_000_000
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs) } catch (_: InterruptedException) { break }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> InputEventTouch.Action.PRESSED
            MotionEvent.ACTION_UP -> InputEventTouch.Action.RELEASED
            MotionEvent.ACTION_CANCEL -> InputEventTouch.Action.RELEASED
            MotionEvent.ACTION_MOVE -> InputEventTouch.Action.MOVED
            MotionEvent.ACTION_POINTER_DOWN -> InputEventTouch.Action.PRESSED
            MotionEvent.ACTION_POINTER_UP -> InputEventTouch.Action.RELEASED
            else -> return false
        }
        val id = if (event.actionMasked == MotionEvent.ACTION_MOVE) {
            event.getPointerId(0)
        } else {
            event.getPointerId(event.actionIndex)
        }
        val x = if (event.actionMasked == MotionEvent.ACTION_MOVE) event.x else event.getX(event.actionIndex)
        val y = if (event.actionMasked == MotionEvent.ACTION_MOVE) event.y else event.getY(event.actionIndex)

        engine?.let { eng ->
            val ev = InputEventTouch(action, id, Vector2(x, y))
            eng.input.feedTouch(ev)
            eng.tree.dispatchTouch(ev)
            eng.tree.root.onInput(ev)
            if (ev.consumed) return true
        }
        return true
    }
}
