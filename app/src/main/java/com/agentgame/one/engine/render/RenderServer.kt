package com.agentgame.one.engine.render

import android.graphics.Canvas
import android.graphics.Paint
import com.agentgame.one.engine.core.Camera2D
import com.agentgame.one.engine.core.CanvasItem
import com.agentgame.one.engine.core.Node
import com.agentgame.one.engine.core.SceneTree
import com.agentgame.one.engine.core.Transform2D
import com.agentgame.one.engine.core.Vector2

/**
 * Renders a scene tree to an android.graphics.Canvas (Godot's `RenderingServer` analogue).
 *
 * Collects all visible canvas items, sorts them by z-order (parent-before-child for stability),
 * finds the active [Camera2D] and draws each item with its world transform applied. UI controls
 * (screen space) are drawn without the camera transform.
 */
class RenderServer(private val tree: SceneTree) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clearColor = Color4(0.075f, 0.08f, 0.12f, 1f)
    var backgroundColor: Color4 = clearColor

    private class Drawable(val item: CanvasItem, val transform: Transform2D, val order: Int)

    /** Renders the current scene into [canvas] sized [width]x[height]. */
    fun render(canvas: Canvas, width: Int, height: Int, delta: Float) {
        canvas.drawColor(backgroundColor.toArgb())

        val drawables = collect(tree.root)
        if (drawables.isEmpty()) return

        // sort by z-index (stable: original preorder order preserved within same z)
        val sorted = drawables.sortedWith(
            compareBy<Drawable> { it.item.zIndex }.thenBy { it.order }
        )

        val camera = findCamera(tree.root)
        val camPos = camera?.effectivePosition(delta) ?: Vector2.ZERO
        val zoom = camera?.zoom ?: 1f

        for (d in sorted) {
            val item = d.item
            if (!item.visible) continue
            if (item.modulate.a <= 0f) continue
            canvas.save()
            if (!item.screenSpace && camera != null) {
                applyCamera(canvas, camPos, zoom, width, height)
            }
            applyTransform(canvas, d.transform)
            try {
                item.onDraw(canvas, paint)
            } catch (t: Throwable) {
                // never let a bad drawable crash the frame
            }
            canvas.restore()
        }
    }

    private fun collect(root: Node): List<Drawable> {
        val out = mutableListOf<Drawable>()
        var order = 0
        collectNode(root, out, order)
        return out
    }

    private fun collectNode(node: Node, out: MutableList<Drawable>, order: Int) {
        if (node is CanvasItem) {
            out.add(Drawable(node, node.computeGlobalTransform(), order))
        }
        var o = order + 1
        for (c in node._children) {
            collectNode(c, out, o)
            o += 1
        }
    }

    private fun findCamera(root: Node): Camera2D? {
        for (node in collect(root)) {
            val c = node.item as? Camera2D
            if (c != null && c.current && c.visible) return c
        }
        return null
    }

    private fun applyCamera(canvas: Canvas, camPos: Vector2, zoom: Float, width: Int, height: Int) {
        canvas.translate(width / 2f, height / 2f)
        canvas.scale(zoom, zoom)
        canvas.translate(-camPos.x, -camPos.y)
    }

    private fun applyTransform(canvas: Canvas, t: Transform2D) {
        if (t.isIdentity) return
        val angle = kotlin.math.atan2(t.xAxis.y, t.xAxis.x)
        val sx = t.xAxis.length
        val sy = t.yAxis.length
        canvas.translate(t.origin.x, t.origin.y)
        canvas.rotate((angle * 180f / Math.PI.toFloat()))
        if (sx != 1f || sy != 1f) canvas.scale(sx, sy)
    }
}
