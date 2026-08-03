package com.agentgame.one.engine.core

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.agentgame.one.engine.render.Color4

/**
 * Filled / outlined polygon in local coordinates (Godot's `Polygon2D` analogue). A versatile
 * building block for procedural art: rects, triangles, platforms, particles, hit flashes, etc.
 */
open class Polygon2D(nodeName: String = "Polygon2D") : Node2D(nodeName) {

    var points: List<Vector2> = emptyList()
    var color: Color4 = Color4.WHITE
    var borderColor: Color4? = null
    var borderWidth: Float = 2f

    fun rect(width: Float, height: Float): Polygon2D {
        points = listOf(
            Vector2.ZERO, Vector2(width, 0f), Vector2(width, height), Vector2(0f, height)
        )
        return this
    }

    fun centeredRect(width: Float, height: Float): Polygon2D {
        points = listOf(
            Vector2(-width / 2f, -height / 2f),
            Vector2(width / 2f, -height / 2f),
            Vector2(width / 2f, height / 2f),
            Vector2(-width / 2f, height / 2f),
        )
        return this
    }

    fun circle(radius: Float, segments: Int = 32): Polygon2D {
        val pts = mutableListOf<Vector2>()
        for (i in 0 until segments) {
            val a = (i.toFloat() / segments) * Math.PI.toFloat() * 2f
            pts.add(Vector2(kotlin.math.cos(a), kotlin.math.sin(a)) * radius)
        }
        points = pts
        return this
    }

    fun triangle(base: Float, height: Float): Polygon2D {
        points = listOf(
            Vector2(0f, -height / 2f), Vector2(base / 2f, height / 2f), Vector2(-base / 2f, height / 2f)
        )
        return this
    }

    fun color(c: Color4): Polygon2D { color = c; return this }
    fun border(c: Color4, w: Float = 2f): Polygon2D { borderColor = c; borderWidth = w; return this }

    override fun computeLocalRect(global: Transform2D): Rect2 {
        if (points.isEmpty()) return Rect2(global.origin.x, global.origin.y, 0f, 0f)
        var r = Rect2(points[0].x, points[0].y, 0f, 0f)
        for (p in points) {
            r = r.expanded(Rect2(p.x, p.y, 0f, 0f))
        }
        // apply global transform to corners roughly via basis length
        val sx = global.xAxis.length
        val sy = global.yAxis.length
        return Rect2(
            global.origin.x + r.x * sx,
            global.origin.y + r.y * sy,
            r.w * sx, r.h * sy
        )
    }

    override fun onDraw(canvas: Canvas, paint: Paint) {
        if (points.size < 2) return
        paint.style = Paint.Style.FILL
        paint.alpha = (modulate.a * 255).toInt()
        paint.color = modulate.times(color).toArgb()
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) path.lineTo(points[i].x, points[i].y)
        path.close()
        canvas.drawPath(path, paint)
        borderColor?.let { bc ->
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = borderWidth
            paint.color = modulate.times(bc).toArgb()
            canvas.drawPath(path, paint)
        }
    }
}
