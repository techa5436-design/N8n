package com.agentgame.one

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import com.agentgame.one.engine.Engine
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.demo.PlatformerDemo
import com.agentgame.one.engine.input.Input
import com.agentgame.one.engine.render.GameView

/**
 * Hosts the Godot-style game engine on an Android [GameView]. Loads the bundled platformer demo
 * scene and wires up virtual joystick input for the left half of the screen.
 */
class EngineActivity : Activity() {

    private val engine = Engine("EngineActivity")
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        Input.reset()

        val w = resources.displayMetrics.widthPixels.toFloat()
        val h = resources.displayMetrics.heightPixels.toFloat()
        // left half = movement joystick
        Input.registerJoystickZone(Rect2(0f, 0f, w / 2f, h))
        Input.registerAction("jump")

        gameView = GameView(this)
        gameView.engine = engine
        setContentView(gameView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        engine.onSceneChanged = { Input.reset() }
        engine.changeScene(PlatformerDemo.buildScene(engine))
    }

    override fun onDestroy() {
        super.onDestroy()
        Input.reset()
    }
}
