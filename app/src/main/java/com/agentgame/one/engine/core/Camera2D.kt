package com.agentgame.one.engine.core

/**
 * Defines the viewport for a 2D scene (Godot's `Camera2D` analogue). The camera's transform
 * determines what portion of the world is visible on screen. Only one camera is active per
 * viewport; the render server picks the first enabled one in the scene.
 */
open class Camera2D(nodeName: String = "Camera2D") : Node2D(nodeName) {

    /** Pixel height of the visible world at zoom=1 (drives the orthographic size). */
    var zoom: Float = 1f
    var anchorMode: Int = ANCHOR_DRAG_CENTER

    object AnchorMode {
        const val ANCHOR_DRAG_CENTER = 0
        const val ANCHOR_DRAG_TOP_LEFT = 1
    }

    /** Optional view limits (world units). */
    var limitLeft: Float = Float.NEGATIVE_INFINITY
    var limitTop: Float = Float.NEGATIVE_INFINITY
    var limitRight: Float = Float.POSITIVE_INFINITY
    var limitBottom: Float = Float.POSITIVE_INFINITY

    /** A target node the camera smoothly follows (optional). */
    var target: Node? = null
    var smoothing: Float = 0f

    var current: Boolean = true

    /**
     * World position that the centre of the screen shows. Smoothly eases toward [target] when set.
     */
    fun effectivePosition(delta: Float): Vector2 {
        var p = globalPosition
        val t = target
        if (t != null) {
            val tp = t.getProperty("position") as? Vector2
            val desired = tp ?: p
            if (smoothing > 0f) {
                val k = 1f - kotlin.math.exp(-smoothing * delta)
                p = p.lerp(desired, k)
            } else {
                p = desired
            }
        }
        // clamp to limits
        if (limitLeft != Float.NEGATIVE_INFINITY) p = p.withX(kotlin.math.max(p.x, limitLeft))
        if (limitRight != Float.POSITIVE_INFINITY) p = p.withX(kotlin.math.min(p.x, limitRight))
        if (limitTop != Float.NEGATIVE_INFINITY) p = p.withY(kotlin.math.max(p.y, limitTop))
        if (limitBottom != Float.POSITIVE_INFINITY) p = p.withY(kotlin.math.min(p.y, limitBottom))
        return p
    }
}
