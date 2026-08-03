package com.agentgame.one

import android.content.pm.ActivityInfo
import com.jme3.app.AndroidHarness

/**
 * Hosts the Infinite Zombie mode. jMonkeyEngine creates [GameApp] via reflection and runs it on
 * the OpenGL surface. Touch controls are handled inside the game.
 */
class GameActivity : AndroidHarness() {

    init {
        appClass = GameApp::class.java.name
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        eglBitsPerPixel = 24
        eglAlphaBits = 8
        eglDepthBits = 24
        eglSamples = 4
        eglStencilBits = 0
        exitDialogTitle = "Exit match?"
        exitDialogMessage = "Return to the lobby?"
    }
}
