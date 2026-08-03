package com.agentgame.one.engine.core

/**
 * Lightweight animation/tweening system (Godot's `Tween` analogue). Animates a property of a
 * target node (e.g. `position`, `rotation`, `scale`, or any numeric / [Vector2] property)
 * between two values over a duration with an easing curve, then emits `finished`.
 */
class Tween(nodeName: String = "Tween") : Node(nodeName) {

    enum class Transition { LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT, SINE }

    class Step(
        val target: Node,
        val property: String,
        val from: Any,
        val to: Any,
        val duration: Float,
        val delay: Float,
        val transition: Transition,
    )

    private val steps = mutableListOf<Step>()
    private var time = 0f
    private var index = 0
    private var started = false

    val finished: Signal get() = signal("finished")
    val stepFinished: Signal get() = signal("step_finished")

    init {
        signal("finished")
        signal("step_finished")
    }

    fun tweenProperty(target: Node, property: String, to: Any, duration: Float): Tween {
        val from = target.getProperty(property) ?: to
        steps.add(Step(target, property, from, to, duration, 0f, Transition.LINEAR))
        return this
    }

    fun tweenProperty(
        target: Node, property: String, from: Any, to: Any, duration: Float,
        transition: Transition = Transition.LINEAR, delay: Float = 0f,
    ): Tween {
        steps.add(Step(target, property, from, to, duration, delay, transition))
        return this
    }

    fun ease(transition: Transition): Tween {
        if (steps.isNotEmpty()) {
            val last = steps.removeAt(steps.size - 1)
            steps.add(Step(last.target, last.property, last.from, last.to, last.duration, last.delay, transition))
        }
        return this
    }

    fun play(): Tween { started = true; time = 0f; index = 0; return this }
    fun stop(): Tween { started = false; return this }

    fun kill(): Tween {
        started = false
        steps.clear()
        return this
    }

    override fun onProcess(delta: Float) {
        if (!started || steps.isEmpty() || index >= steps.size) return
        val step = steps[index]
        if (time < step.delay) {
            time += delta
            return
        }
        val t = ((time - step.delay) / step.duration).coerceIn(0f, 1f)
        val eased = applyTransition(t, step.transition)
        applyValue(step, eased)
        time += delta
        if (time - step.delay >= step.duration) {
            applyValue(step, 1f)
            stepFinished.emit(step.property)
            index++
            if (index >= steps.size) {
                started = false
                finished.emit()
            }
        }
    }

    private fun applyValue(step: Step, t: Float) {
        val target = step.target
        val from = step.from
        val to = step.to
        when {
            from is Vector2 && to is Vector2 -> target.setProperty(step.property, from.lerp(to, t))
            from is Float && to is Float -> target.setProperty(step.property, from + (to - from) * t)
            from is Int && to is Int -> target.setProperty(step.property, (from + ((to - from) * t).toInt()))
            from is ColorValue && to is ColorValue ->
                target.setProperty(step.property, from.lerp(to, t))
            t >= 1f -> target.setProperty(step.property, to)
        }
    }

    private fun applyTransition(t: Float, transition: Transition): Float {
        return when (transition) {
            Transition.LINEAR -> t
            Transition.EASE_IN -> t * t
            Transition.EASE_OUT -> 1f - (1f - t) * (1f - t)
            Transition.EASE_IN_OUT -> if (t < 0.5f) 2f * t * t else 1f - 2f * (1f - t) * (1f - t)
            Transition.SINE -> 0.5f - 0.5f * kotlin.math.cos(t * Math.PI.toFloat())
        }
    }
}

/** Small value wrapper so tweens can animate colours. */
data class ColorValue(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {
    fun lerp(o: ColorValue, t: Float): ColorValue = ColorValue(
        r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t, a + (o.a - a) * t
    )
}
