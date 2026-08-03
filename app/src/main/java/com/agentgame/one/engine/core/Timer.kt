package com.agentgame.one.engine.core

/**
 * Counts down and fires a `timeout` signal (Godot's `Timer` analogue). Can be one-shot or
 * repeating and supports a per-tick `timeout`/`step` signal.
 */
open class Timer(nodeName: String = "Timer") : Node(nodeName) {

    /** Wait time in seconds. */
    var waitTime: Float = 1f
    var oneShot: Boolean = false
    var autostart: Boolean = false

    /** If > 0, time_left is reduced each physics tick instead of process tick. */
    var processCallback: Int = ProcessCallback.IDLE

    object ProcessCallback {
        const val IDLE = 0
        const val PHYSICS = 1
    }

    private var started = false
    private var timeLeft = 0f
    private val pausedGuard = false

    val isStopped: Boolean get() = !started
    val timeLeftValue: Float get() = timeLeft

    val timeout: Signal get() = signal("timeout")
    val step: Signal get() = signal("step")

    init {
        signal("timeout")
        signal("step")
    }

    override fun onReady() {
        if (autostart) start()
    }

    override fun onProcess(delta: Float) {
        if (!started) return
        if (processCallback != ProcessCallback.PHYSICS) tick(delta)
    }

    override fun onPhysicsProcess(delta: Float) {
        if (!started) return
        if (processCallback == ProcessCallback.PHYSICS) tick(delta)
    }

    private fun tick(delta: Float) {
        timeLeft -= delta
        step.emit(timeLeft)
        if (timeLeft <= 0f) {
            if (oneShot) {
                started = false
                timeout.emit()
            } else {
                timeLeft += waitTime
                timeout.emit()
            }
        }
    }

    fun start(): Timer {
        started = true
        timeLeft = waitTime
        return this
    }

    fun start(waitSeconds: Float): Timer {
        waitTime = waitSeconds
        return start()
    }

    fun stop(): Timer {
        started = false
        timeLeft = 0f
        return this
    }

    fun setWaitTime(seconds: Float): Timer {
        waitTime = seconds
        return this
    }
}
