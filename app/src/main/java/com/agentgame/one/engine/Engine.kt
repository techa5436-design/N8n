package com.agentgame.one.engine

import android.util.Log
import com.agentgame.one.engine.core.Node
import com.agentgame.one.engine.core.SceneTree
import com.agentgame.one.engine.input.Input
import com.agentgame.one.engine.render.RenderServer

/**
 * The top-level engine object (Godot's `Engine` singleton analogue). Owns the scene tree,
 * render server, input and the game-loop timing. A [GameView] drives this each frame.
 */
class Engine(val tag: String = "GameEngine") {

    val tree: SceneTree = SceneTree(this)
    val renderer: RenderServer = RenderServer(tree)
    val input: Input get() = Input

    /** Fixed physics timestep in seconds (Godot: 60 Hz default). */
    var physicsTicksPerSecond: Int = 60
    val fixedDelta: Float get() = 1f / physicsTicksPerSecond

    /** Max frames of physics to catch up in one frame (avoid spiral of death). */
    var maxPhysicsStepsPerFrame: Int = 5

    private var accumulator = 0f
    private var lastFrameNs = -1L

    private val errorListeners = mutableListOf<(String) -> Unit>()

    var onSceneChanged: ((Node) -> Unit)? = null

    fun addErrorListener(listener: (String) -> Unit) {
        errorListeners.add(listener)
    }

    fun reportError(message: String) {
        Log.e(tag, message)
        for (l in errorListeners) l(message)
    }

    /** Called each rendered frame. */
    fun onFrame(canvas: android.graphics.Canvas, width: Int, height: Int, delta: Float) {
        accumulator += delta.coerceAtLeast(0f).coerceAtMost(0.25f)
        var steps = 0
        while (accumulator >= fixedDelta && steps < maxPhysicsStepsPerFrame) {
            tree.physicsFrame(fixedDelta)
            accumulator -= fixedDelta
            steps++
        }
        tree.processFrame(delta)
        renderer.render(canvas, width, height, delta)
        input.endFrame()
    }

    /** Replaces the current scene. */
    fun changeScene(root: Node) {
        tree.changeSceneTo(root)
        onSceneChanged?.invoke(root)
    }

    /** Resets the fixed-step accumulator (call on resume / scene change). */
    fun resetAccumulator() {
        accumulator = 0f
    }
}
