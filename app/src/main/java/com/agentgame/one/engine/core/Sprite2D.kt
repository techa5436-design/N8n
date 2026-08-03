package com.agentgame.one.engine.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.render.Texture

/**
 * Draws a bitmap (Godot's `Sprite2D` analogue). The texture can be a shared [Texture] loaded by
 * the asset system, or a plain android.graphics.Bitmap. Supports flip, offset and region.
 */
open class Sprite2D(nodeName: String = "Sprite2D") : Node2D(nodeName) {

    var texture: Texture? = null
    var bitmap: Bitmap? = null

    /** Anchor point within the texture: (0,0)=top-left, (0.5,0.5)=centre. */
    var offset: Vector2 = Vector2.ZERO
    var region: Rect2? = null
    var flipH: Boolean = false
    var flipV: Boolean = false

    var centered: Boolean = true
        set(value) {
            field = value
            if (value) offset = Vector2(0.5f, 0.5f)
        }

    fun setTexture(t: Texture): Sprite2D { texture = t; bitmap = t.bitmap; return this }

    /** Intrinsic texture size in local units (canvas already has scale applied). */
    private fun intrinsicSize(): Vector2 {
        val bmp = bitmap ?: texture?.bitmap ?: return Vector2.ZERO
        val r = region
        val w = (r?.w ?: bmp.width.toFloat())
        val h = (r?.h ?: bmp.height.toFloat())
        return Vector2(w, h)
    }

    override fun computeLocalRect(global: Transform2D): Rect2 {
        val size = intrinsicSize()
        val w = size.x * global.xAxis.length
        val h = size.y * global.yAxis.length
        val o = offset
        return Rect2(
            global.origin.x - w * o.x,
            global.origin.y - h * o.y,
            w, h
        )
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        val bmp = bitmap ?: texture?.bitmap ?: return
        paint.alpha = (modulate.a * 255).toInt()
        paint.color = modulate.toArgb()
        paint.isFilterBitmap = true

        val r = region
        val srcLeft = r?.left?.toInt() ?: 0
        val srcTop = r?.top?.toInt() ?: 0
        val srcRight = r?.right?.toInt() ?: bmp.width
        val srcBottom = r?.bottom?.toInt() ?: bmp.height
        val srcW = srcRight - srcLeft
        val srcH = srcBottom - srcTop

        val w = srcW.toFloat()
        val h = srcH.toFloat()
        val ox = offset.x * w
        val oy = offset.y * h

        val left = -ox
        val top = -oy
        val right = left + w
        val bottom = top + h

        val dst = android.graphics.RectF(left, top, right, bottom)
        val srcRect = android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom)

        if (flipH || flipV) {
            canvas.save()
            canvas.translate((left + right) / 2f, (top + bottom) / 2f)
            canvas.scale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
            canvas.translate(-(left + right) / 2f, -(top + bottom) / 2f)
            canvas.drawBitmap(bmp, srcRect, dst, paint)
            canvas.restore()
        } else {
            canvas.drawBitmap(bmp, srcRect, dst, paint)
        }
    }
}
