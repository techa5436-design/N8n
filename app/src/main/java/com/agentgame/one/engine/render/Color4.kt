package com.agentgame.one.engine.render

import android.graphics.Color

/** RGBA colour with convenience constructors. */
data class Color4(val r: Float, val g: Float, val b: Float, val a: Float = 1f) {

    /** Packed 0xAARRGGBB int for android.graphics. */
    fun toArgb(): Int = Color.argb(
        (a.coerceIn(0f, 1f) * 255).toInt(),
        (r.coerceIn(0f, 1f) * 255).toInt(),
        (g.coerceIn(0f, 1f) * 255).toInt(),
        (b.coerceIn(0f, 1f) * 255).toInt(),
    )

    operator fun times(o: Color4) = Color4(r * o.r, g * o.g, b * o.b, a * o.a)
    operator fun times(s: Float) = Color4(r * s, g * s, b * s, a)

    fun withAlpha(na: Float) = Color4(r, g, b, na)
    fun withR(n: Float) = Color4(n, g, b, a)
    fun withG(n: Float) = Color4(r, n, b, a)
    fun withB(n: Float) = Color4(r, g, n, a)

    fun darken(factor: Float) = Color4(r * factor, g * factor, b * factor, a)

    companion object {
        val WHITE = Color4(1f, 1f, 1f, 1f)
        val BLACK = Color4(0f, 0f, 0f, 1f)
        val TRANSPARENT = Color4(0f, 0f, 0f, 0f)
        val RED = Color4(1f, 0f, 0f, 1f)
        val GREEN = Color4(0f, 1f, 0f, 1f)
        val BLUE = Color4(0f, 0f, 1f, 1f)
        val YELLOW = Color4(1f, 1f, 0f, 1f)
        val CYAN = Color4(0f, 1f, 1f, 1f)
        val MAGENTA = Color4(1f, 0f, 1f, 1f)
        val ORANGE = Color4(1f, 0.55f, 0f, 1f)
        val GRAY = Color4(0.5f, 0.5f, 0.5f, 1f)
        val DARKGRAY = Color4(0.3f, 0.3f, 0.3f, 1f)
        val LIGHTGRAY = Color4(0.8f, 0.8f, 0.8f, 1f)

        fun rgb(r: Int, g: Int, b: Int, a: Int = 255) =
            Color4(r / 255f, g / 255f, b / 255f, a / 255f)

        fun fromInt(argb: Int): Color4 {
            val a = ((argb shr 24) and 0xff) / 255f
            val r = ((argb shr 16) and 0xff) / 255f
            val g = ((argb shr 8) and 0xff) / 255f
            val b = (argb and 0xff) / 255f
            return Color4(r, g, b, a)
        }
    }
}
