package com.agentgame.one.engine.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import java.io.InputStream

/**
 * A shared, reference-counted image resource (Godot's `Texture2D` analogue). Loading is cached so
 * the same asset is decoded only once; nodes reference the same bitmap cheaply.
 */
class Texture internal constructor(
    val bitmap: Bitmap,
    val resourcePath: String,
) {
    var filterEnabled: Boolean = true

    val width: Int get() = bitmap.width
    val height: Int get() = bitmap.height

    companion object {
        private val cache = HashMap<String, Texture>()

        /** Loads and caches a bitmap from the app's bundled assets. */
        fun load(context: Context, assetPath: String): Texture {
            val cached = cache[assetPath]
            if (cached != null) return cached
            val stream: InputStream = context.assets.open(assetPath)
            val bmp = BitmapFactory.decodeStream(stream)!!
            stream.close()
            val tex = Texture(bmp, "res://$assetPath")
            cache[assetPath] = tex
            return tex
        }

        fun fromBitmap(bmp: Bitmap, path: String = "generated"): Texture {
            val tex = Texture(bmp, path)
            return tex
        }

        /** Generates a solid-colour texture (for primitives / placeholders). */
        fun solidColor(width: Int, height: Int, color: Color4): Texture {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = color.toArgb()
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), p)
            return fromBitmap(bmp, "res://color:$width x $height")
        }

        /** Builds a checkerboard / gradient texture for testing. */
        fun gradient(width: Int, height: Int, from: Color4, to: Color4): Texture {
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint()
            for (y in 0 until height) {
                val t = y.toFloat() / height
                p.color = Color4(
                    from.r + (to.r - from.r) * t,
                    from.g + (to.g - from.g) * t,
                    from.b + (to.b - from.b) * t, 1f
                ).toArgb()
                c.drawRect(0f, y.toFloat(), width.toFloat(), (y + 1).toFloat(), p)
            }
            return fromBitmap(bmp, "res://gradient")
        }

        fun clearCache() {
            cache.clear()
        }
    }
}
