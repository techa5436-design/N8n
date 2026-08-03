package com.agentgame.one.engine.ui

import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.core.Rect2
import com.agentgame.one.engine.render.Color4

/**
 * Displays text (Godot's `Label` analogue). Part of the UI system; drawn in screen space.
 */
open class Label(nodeName: String = "Label") : Control(nodeName) {

    var text: String = ""
    var fontSize: Int = 24
    var fontColor: Color4 = Color4.WHITE
    var align: Align = Align.LEFT
    var outlineColor: Color4? = null

    enum class Align { LEFT, CENTER, RIGHT }

    fun text(value: String): Label { text = value; return this }
    fun font(size: Int): Label { fontSize = size; return this }
    fun color(c: Color4): Label { fontColor = c; return this }

    override fun computeLocalRect(global: com.agentgame.one.engine.core.Transform2D): Rect2 =
        Rect2(global.origin.x, global.origin.y, size.x, size.y)

    override fun onDraw(canvas: Canvas, paint: Paint) {
        // background first (from Control)
        super.onDraw(canvas, paint)
        if (text.isEmpty()) return
        paint.style = Paint.Style.FILL
        paint.textSize = fontSize.toFloat()
        paint.isAntiAlias = true
        paint.alpha = (modulate.a * 255).toInt()

        val lines = text.split("\n")
        val lineHeight = paint.fontSpacing
        var y = lineHeight
        for (line in lines) {
            val x = when (align) {
                Align.LEFT -> 4f
                Align.CENTER -> (size.x - paint.measureText(line)) / 2f
                Align.RIGHT -> size.x - paint.measureText(line) - 4f
            }
            outlineColor?.let { oc ->
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 3f
                paint.color = oc.toArgb()
                canvas.drawText(line, x, y, paint)
                paint.style = Paint.Style.FILL
            }
            paint.color = fontColor.toArgb()
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
    }
}
