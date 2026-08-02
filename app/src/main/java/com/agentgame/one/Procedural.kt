package com.agentgame.one

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Mesh
import com.jme3.scene.Node
import com.jme3.scene.shape.Box
import com.jme3.scene.shape.Cylinder
import com.jme3.scene.shape.Sphere
import com.jme3.texture.Image
import com.jme3.texture.Texture
import com.jme3.texture.Texture2D
import com.jme3.util.BufferUtils
import com.jme3.util.ImageRaster

/**
 * Tiny toolkit for building every 3D object in the game procedurally (no external asset files),
 * plus a small procedural texture painter used for grass, ground, roads and building walls.
 */
object Procedural {

    /** A flat, unlit material in a single color. Cheap and safe for simple parts. */
    fun matUnlit(am: AssetManager, r: Float, g: Float, b: Float): Material {
        val m = Material(am, "Common/MatDefs/Misc/Unshaded.j3md")
        m.setColor("Color", ColorRGBA(r, g, b, 1f))
        return m
    }

    /** A lit (shaded) material in a single diffuse color, looks better for terrain/buildings. */
    fun matLit(am: AssetManager, r: Float, g: Float, b: Float, shininess: Float = 8f): Material {
        val m = Material(am, "Common/MatDefs/Light/Lighting.j3md")
        m.setColor("Diffuse", ColorRGBA(r, g, b, 1f))
        m.setColor("Ambient", ColorRGBA(r * 0.4f, g * 0.4f, b * 0.4f, 1f))
        m.setColor("Specular", ColorRGBA(0.2f, 0.2f, 0.2f, 1f))
        m.setFloat("Shininess", shininess)
        return m
    }

    /** Build a lit material whose color comes from a procedurally painted texture. */
    fun matLitTexture(
        am: AssetManager,
        width: Int,
        height: Int,
        paint: (x: Int, y: Int, ColorRGBA) -> Unit,
        shininess: Float = 4f,
    ): Material {
        val tex = paintTexture(width, height, paint)
        val m = Material(am, "Common/MatDefs/Light/Lighting.j3md")
        m.setTexture("DiffuseMap", tex)
        m.setColor("Ambient", ColorRGBA(0.4f, 0.4f, 0.4f, 1f))
        m.setColor("Specular", ColorRGBA(0.1f, 0.1f, 0.1f, 1f))
        m.setFloat("Shininess", shininess)
        return m
    }

    /** Paint an RGBA texture via a pixel callback. */
    fun paintTexture(
        width: Int,
        height: Int,
        paint: (x: Int, y: Int, ColorRGBA) -> Unit,
    ): Texture2D {
        val buf = BufferUtils.createByteBuffer(width * height * 4)
        val image = Image(Image.Format.RGBA8, width, height, buf)
        val raster = ImageRaster.create(image)
        val c = ColorRGBA()
        for (y in 0 until height) {
            for (x in 0 until width) {
                paint(x, y, c)
                raster.setPixel(x, y, c)
            }
        }
        val tex = Texture2D(image)
        tex.setWrap(Texture.WrapMode.Repeat)
        tex.setMinFilter(Texture.MinFilter.Trilinear)
        tex.setMagFilter(Texture.MagFilter.Bilinear)
        return tex
    }

    /** Box geometry at origin, caller translates/scales/colors it. */
    fun box(am: AssetManager, sx: Float, sy: Float, sz: Float, mat: Material): Geometry {
        val geo = Geometry("box", Box(sx, sy, sz))
        geo.material = mat
        return geo
    }

    /** Cylinder geometry (radius, height). */
    fun cylinder(am: AssetManager, radius: Float, height: Float, mat: Material): Geometry {
        val mesh: Mesh = Cylinder(12, 24, radius, height, true)
        val geo = Geometry("cyl", mesh)
        geo.material = mat
        return geo
    }

    fun sphere(am: AssetManager, radius: Float, mat: Material): Geometry {
        val mesh: Mesh = Sphere(12, 16, radius)
        val geo = Geometry("sphere", mesh)
        geo.material = mat
        return geo
    }

    /** Attach a part to a parent node, positioned at (x,y,z). */
    fun part(parent: Node, g: Geometry, x: Float, y: Float, z: Float): Geometry {
        g.setLocalTranslation(x, y, z)
        parent.attachChild(g)
        return g
    }
}
