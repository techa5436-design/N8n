package com.agentgame.one

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.terrain.geomipmap.TerrainQuad
import com.jme3.terrain.heightmap.AbstractHeightMap
import java.util.Random

/**
 * Builds the entire ~1km x 1km battlefield procedurally:
 * rolling terrain, a city district, houses, trees, dense grass and roads.
 * Everything is generated in code — no model/texture files required.
 */
class WorldBuilder(private val am: AssetManager, private val seed: Long = 1337L) {

    data class Rect(val xmin: Float, val xmax: Float, val zmin: Float, val zmax: Float) {
        fun contains(x: Float, z: Float): Boolean =
            x in xmin..xmax && z in zmin..zmax
    }

    val scene = Node("world")

    val TERRAIN_SIZE = 513
    val SCALE = 2f                     // metres per terrain cell -> ~1026m x 1026m
    val WORLD_M = TERRAIN_SIZE * SCALE

    private val rng = Random(seed)
    private lateinit var heights: FloatArray
    val footprints = ArrayList<Rect>()   // building ground boxes (for collision/blocking)
    val collisionBoxes = ArrayList<Rect>() // taller buildings player can't walk through

    /** Ground height in world metres at a world (x,z) position. */
    fun heightAt(x: Float, z: Float): Float {
        // Terrain domain: [-WORLD_M/2, WORLD_M/2] in world space, index 0..TERRAIN_SIZE.
        val fx = (x + WORLD_M / 2f) / SCALE
        val fz = (z + WORLD_M / 2f) / SCALE
        return sample(heights, fx, fz, TERRAIN_SIZE) * SCALE
    }

    /** Is this world (x,z) inside a blocking building footprint (player/zombie can't enter)? */
    fun isBlocked(x: Float, z: Float): Boolean {
        for (r in collisionBoxes) if (r.contains(x, z)) return true
        return false
    }

    /** True when inside the outer playable boundary (keeps everyone on the map). */
    fun inBounds(x: Float, z: Float): Boolean {
        val h = WORLD_M / 2f - 6f
        return x > -h && x < h && z > -h && z < h
    }

    fun build(): Node {
        generateHeights()
        val terrain = buildTerrain()
        scene.attachChild(terrain)

        buildCity()
        buildHouses()
        buildTrees()
        buildGrass()
        buildRoads()

        return scene
    }

    // ------------------------------------------------------------------ terrain

    private fun generateHeights() {
        val n = TERRAIN_SIZE * TERRAIN_SIZE
        heights = FloatArray(n)
        for (i in 0 until TERRAIN_SIZE) {
            for (j in 0 until TERRAIN_SIZE) {
                val nx = i / TERRAIN_SIZE.toFloat()
                val ny = j / TERRAIN_SIZE.toFloat()
                var h = 0f
                h += 5f * fbm(nx * 1.6f, ny * 1.6f, 3)
                h += 2f * fbm(nx * 6f + 17f, ny * 6f + 31f, 2)
                // Flatten a big central plateau so the city sits nicely.
                val cx = (nx - 0.5f) * 2f
                val cy = (ny - 0.5f) * 2f
                val dist = Math.sqrt((cx * cx + cy * cy).toDouble()).toFloat()
                val plateau = Math.max(0f, 1f - dist * 0.7f)
                h = h * (1f - plateau * 0.75f) + 1.0f
                heights[j * TERRAIN_SIZE + i] = h
            }
        }
    }

    private fun fbm(x: Float, y: Float, octaves: Int): Float {
        var v = 0f
        var amp = 0.5f
        var fx = x
        var fy = y
        var norm = 0f
        for (k in 0 until octaves) {
            v += amp * valueNoise(fx, fy)
            norm += amp
            amp *= 0.5f
            fx *= 2.1f
            fy *= 2.1f
        }
        return v / norm
    }

    private fun valueNoise(x: Float, y: Float): Float {
        val xi = Math.floor(x.toDouble()).toInt()
        val yi = Math.floor(y.toDouble()).toInt()
        val xf = x - xi
        val yf = y - yi
        fun hash(a: Int, b: Int): Float {
            var h = a * 374761393 + b * 668265263
            h = (h xor (h shr 13)) * 1274126177
            h = h xor (h shr 16)
            return (h and 0x7fffffff) / 2147483648f - 0.5f
        }
        val sx = xf * xf * (3f - 2f * xf)
        val sy = yf * yf * (3f - 2f * yf)
        val a = hash(xi, yi)
        val b = hash(xi + 1, yi)
        val c = hash(xi, yi + 1)
        val d = hash(xi + 1, yi + 1)
        return lerp(sx, lerp(sy, a, c), lerp(sy, b, d))
    }

    private fun lerp(t: Float, a: Float, b: Float): Float = a + (b - a) * t

    private fun sample(h: FloatArray, fx: Float, fy: Float, size: Int): Float {
        val x = fx.coerceIn(0f, (size - 1).toFloat())
        val y = fy.coerceIn(0f, (size - 1).toFloat())
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = (x0 + 1).coerceAtMost(size - 1)
        val y1 = (y0 + 1).coerceAtMost(size - 1)
        val tx = x - x0
        val ty = y - y0
        val a = h[y0 * size + x0]
        val b = h[y0 * size + x1]
        val c = h[y1 * size + x0]
        val d = h[y1 * size + x1]
        val ab = a + (b - a) * tx
        val cd = c + (d - c) * tx
        return ab + (cd - ab) * ty
    }

    private fun buildTerrain(): TerrainQuad {
        val hm = object : AbstractHeightMap() {
            override fun load(): Boolean {
                size = TERRAIN_SIZE
                heightData = FloatArray(TERRAIN_SIZE * TERRAIN_SIZE)
                for (j in 0 until TERRAIN_SIZE) for (i in 0 until TERRAIN_SIZE) {
                    heightData[j * TERRAIN_SIZE + i] = heights[j * TERRAIN_SIZE + i]
                }
                return true
            }
        }
        hm.load()

        val terrain = TerrainQuad("terrain", 65, TERRAIN_SIZE, hm)
        terrain.setLocalScale(SCALE, SCALE, SCALE)
        terrain.setLocalTranslation(-WORLD_M / 2f, 0f, -WORLD_M / 2f)

        // Splat material: grass / rock / dirt blended by an alpha map.
        val grassTex = Procedural.paintTexture(128, 128) { x, y, c ->
            c.set(0.20f + 0.15f * valueNoise(x * 0.3f, y * 0.3f) + 0.1f,
                0.55f + 0.2f * valueNoise(x * 0.3f + 9f, y * 0.3f),
                0.14f, 1f)
        }
        val rockTex = Procedural.paintTexture(128, 128) { x, y, c ->
            val v = 0.45f + 0.12f * valueNoise(x * 0.2f, y * 0.2f)
            c.set(v, v, v, 1f)
        }
        val dirtTex = Procedural.paintTexture(128, 128) { x, y, c ->
            c.set(0.42f, 0.30f, 0.18f, 1f)
        }

        val tm = com.jme3.terrain.geomipmap.TerrainMaterial(am,
            3f, grassTex, rockTex, dirtTex,
            com.jme3.terrain.geomipmap.TerrainMaterial.Blending.Low)
        tm.setColor("GlobalAmbient", ColorRGBA(0.55f, 0.55f, 0.55f, 1f))
        terrain.material = tm
        return terrain
    }

    // ------------------------------------------------------------------ city

    private fun buildCity() {
        val wallTex = buildingWallTexture()
        val roofMat = Procedural.matLit(am, 0.35f, 0.33f, 0.32f, 30f)
        val glassMat = Procedural.matLit(am, 0.25f, 0.55f, 0.62f, 90f)

        // A dense downtown block grid around the centre.
        val blockCount = 12
        for (i in 0 until blockCount) {
            for (j in 0 until blockCount) {
                // Central ~400m x 400m city patch.
                val cx = (i - blockCount / 2f) * 34f + rngBetween(-6f, 6f)
                val cz = (j - blockCount / 2f) * 34f + rngBetween(-6f, 6f)
                val dist = Math.sqrt(cx * cx + cz * cz).toFloat()
                if (dist > 190f) continue

                val w = rngBetween(8f, 14f)
                val d = rngBetween(8f, 14f)
                val hgt = rngBetween(14f, 40f)
                val base = heightAt(cx, cz)
                building(cx, cz, w, d, hgt, base, wallTex, roofMat, glassMat, tall = true)
                // Leave a few open plazas so the fight can flow.
                if (i % 3 == 0 && j % 3 == 0) continue
            }
        }
    }

    // ------------------------------------------------------------------ houses

    private fun buildHouses() {
        val wallMat = Procedural.matLitTexture(am, 64, 64) { x, y, c ->
            val brick = ((x / 16) + (y / 8)) % 2 == 0
            if (brick) c.set(0.72f, 0.52f, 0.38f, 1f) else c.set(0.62f, 0.44f, 0.32f, 1f)
        }
        val roofMat = Procedural.matLit(am, 0.45f, 0.18f, 0.12f, 20f)

        val count = 55
        for (k in 0 until count) {
            val ang = rng.nextFloat() * 6.283f
            val rad = rngBetween(70f, 430f)
            val cx = Math.cos(ang.toDouble()).toFloat() * rad
            val cz = Math.sin(ang.toDouble()).toFloat() * rad
            if (Math.abs(cx) < 40f && Math.abs(cz) < 40f) continue
            val w = rngBetween(5f, 9f)
            val d = rngBetween(5f, 9f)
            val hgt = rngBetween(3f, 6f)
            val base = heightAt(cx, cz)
            house(cx, cz, w, d, hgt, base, wallMat, roofMat)
        }
    }

    // ------------------------------------------------------------------ trees

    private fun buildTrees() {
        val trunkMat = Procedural.matLit(am, 0.36f, 0.24f, 0.13f, 6f)
        val leafMats = arrayOf(
            Procedural.matLit(am, 0.13f, 0.42f, 0.13f, 10f),
            Procedural.matLit(am, 0.16f, 0.50f, 0.16f, 10f),
            Procedural.matLit(am, 0.10f, 0.36f, 0.12f, 10f),
        )

        val count = 320
        for (k in 0 until count) {
            val ang = rng.nextFloat() * 6.283f
            val rad = rngBetween(20f, 500f)
            val cx = Math.cos(ang.toDouble()).toFloat() * rad
            val cz = Math.sin(ang.toDouble()).toFloat() * rad
            if (Math.abs(cx) < 30f && Math.abs(cz) < 30f) continue
            if (isBlocked(cx, cz)) continue
            val s = rngBetween(0.8f, 1.8f)
            val base = heightAt(cx, cz)
            tree(cx, cz, base, s, trunkMat, leafMats[rng.nextInt(leafMats.size)])
        }
    }

    // ------------------------------------------------------------------ grass

    private fun buildGrass() {
        val grassMat = Procedural.matUnlit(am, 0.22f, 0.55f, 0.18f)
        // Scattered 3D grass tufts (crossed blades) on the open plains.
        val count = 260
        val tuftNode = Node("grass")
        for (k in 0 until count) {
            val ang = rng.nextFloat() * 6.283f
            val rad = rngBetween(15f, 500f)
            val cx = Math.cos(ang.toDouble()).toFloat() * rad
            val cz = Math.sin(ang.toDouble()).toFloat() * rad
            if (Math.abs(cx) < 30f && Math.abs(cz) < 30f) continue
            if (isBlocked(cx, cz)) continue
            val base = heightAt(cx, cz)
            val tuft = grassTuft(grassMat, rngBetween(0.4f, 0.8f))
            tuft.setLocalTranslation(cx, base, cz)
            tuftNode.attachChild(tuft)
        }
        scene.attachChild(tuftNode)
    }

    // ------------------------------------------------------------------ roads

    private fun buildRoads() {
        val roadMat = Procedural.matLit(am, 0.12f, 0.12f, 0.13f, 2f)
        val lineMat = Procedural.matUnlit(am, 0.9f, 0.75f, 0.2f)

        // Two main crossing highways + a ring road. Drawn just above the ground.
        road(-WORLD_M / 2f + 6f, WORLD_M / 2f - 6f, 0f, 0f, 10f, roadMat) // E-W
        road(0f, 0f, -WORLD_M / 2f + 6f, WORLD_M / 2f - 6f, 10f, roadMat) // N-S
        ringRoad(roadMat)

        // Dashed centre lines.
        dashLines(0f, -420f, 420f, lineMat, vertical = true)
        dashLines(-420f, 420f, 0f, lineMat, vertical = false)
    }

    private fun road(x0: Float, x1: Float, z0: Float, z1: Float, halfW: Float, m: Material) {
        // Lay short segments following terrain height.
        val seg = 6f
        var t = 0f
        val len = if (x0 != x1) (x1 - x0) else (z1 - z0)
        while (t < Math.abs(len)) {
            val x: Float
            val z: Float
            if (x0 != x1) { x = x0 + Math.signum(x1 - x0) * t; z = z0 } else { x = x0; z = z0 + Math.signum(z1 - z0) * t }
            val h = heightAt(x, z) + 0.04f
            val plane = Geometry("road", com.jme3.scene.shape.Box(if (x0 != x1) halfW else 0.25f, 0.02f, if (x0 != x1) 0.25f else halfW))
            plane.material = m
            plane.setLocalTranslation(x, h, z)
            scene.attachChild(plane)
            t += seg
        }
    }

    private fun ringRoad(m: Material) {
        val r = 460f
        var ang = 0f
        val step = 0.02f
        while (ang < 6.283f) {
            val x = Math.cos(ang.toDouble()).toFloat() * r
            val z = Math.sin(ang.toDouble()).toFloat() * r
            val h = heightAt(x, z) + 0.04f
            val box = com.jme3.scene.shape.Box(0.5f, 0.02f, 8f)
            val g = Geometry("ring", box)
            g.material = m
            g.setLocalTranslation(x, h, z)
            g.rotate(0f, -ang - 1.5708f, 0f)
            scene.attachChild(g)
            ang += step
        }
    }

    private fun dashLines(cx: Float, z0: Float, z1: Float, m: Material, vertical: Boolean) {
        var z = z0
        while (z < z1) {
            val h = heightAt(cx, z) + 0.06f
            val g = Geometry("line", com.jme3.scene.shape.Box(if (vertical) 0.08f else 1.2f, 0.01f, if (vertical) 1.2f else 0.08f))
            g.material = m
            if (vertical) g.setLocalTranslation(cx, h, z) else g.setLocalTranslation(z, h, cx)
            scene.attachChild(g)
            z += 7f
        }
    }

    // ------------------------------------------------------------------ building primitives

    private fun building(cx: Float, cz: Float, w: Float, d: Float, hgt: Float,
                         base: Float, wallMat: Material, roofMat: Material,
                         glassMat: Material, tall: Boolean) {
        val b = Procedural.box(am, w / 2f, hgt / 2f, d / 2f, wallMat)
        b.setLocalTranslation(cx, base + hgt / 2f, cz)
        scene.attachChild(b)

        val roof = Procedural.box(am, w / 2f + 0.3f, 0.4f, d / 2f + 0.3f, roofMat)
        roof.setLocalTranslation(cx, base + hgt, cz)
        scene.attachChild(roof)

        if (tall) {
            val box = Rect(cx - w / 2f, cx + w / 2f, cz - d / 2f, cz + d / 2f)
            footprints.add(box)
            collisionBoxes.add(box)
        }
    }

    private fun house(cx: Float, cz: Float, w: Float, d: Float, hgt: Float,
                      base: Float, wallMat: Material, roofMat: Material) {
        val b = Procedural.box(am, w / 2f, hgt / 2f, d / 2f, wallMat)
        b.setLocalTranslation(cx, base + hgt / 2f, cz)
        scene.attachChild(b)

        // simple pitched roof
        val roof = Procedural.box(am, w / 2f + 0.4f, 0.3f, d / 2f + 0.4f, roofMat)
        roof.setLocalTranslation(cx, base + hgt, cz)
        scene.attachChild(roof)

        // door
        val door = Procedural.box(am, 0.4f, 0.9f, 0.05f, Procedural.matLit(am, 0.3f, 0.2f, 0.1f, 8f))
        door.setLocalTranslation(cx, base + 0.45f, cz + d / 2f + 0.05f)
        scene.attachChild(door)

        val box = Rect(cx - w / 2f, cx + w / 2f, cz - d / 2f, cz + d / 2f)
        footprints.add(box)
        collisionBoxes.add(box)
    }

    private fun tree(cx: Float, cz: Float, base: Float, s: Float, trunkMat: Material, leafMat: Material) {
        val hgt = 4f * s
        val trunk = Procedural.cylinder(am, 0.16f * s, hgt, trunkMat)
        trunk.setLocalTranslation(cx, base + hgt / 2f, cz)
        scene.attachChild(trunk)

        val leaf = Procedural.sphere(am, 1.5f * s, leafMat)
        leaf.setLocalTranslation(cx, base + hgt + 0.6f * s, cz)
        leaf.setLocalScale(1f, 0.8f, 1f)
        scene.attachChild(leaf)

        val leaf2 = Procedural.sphere(am, 1.0f * s, leafMat)
        leaf2.setLocalTranslation(cx + 0.5f * s, base + hgt + 0.2f * s, cz - 0.3f * s)
        scene.attachChild(leaf2)
    }

    private fun grassTuft(m: Material, s: Float): Node {
        val n = Node("tuft")
        // crossed blades
        for (i in 0..3) {
            val blade = Geometry("blade", com.jme3.scene.shape.Box(0.03f * s, 0.45f * s, 0.03f * s))
            blade.material = m
            blade.setLocalTranslation(0f, 0.22f * s, 0f)
            blade.rotate(0f, i * 1.57f, 0f)
            blade.rotate(0.35f, 0f, 0f)
            n.attachChild(blade)
        }
        return n
    }

    private fun buildingWallTexture(): Material =
        Procedural.matLitTexture(am, 128, 128) { x, y, c ->
            // grid of windows on a concrete tower face
            val cell = 16
            val wall = 0.48f + 0.06f * valueNoise(x * 0.2f, y * 0.2f)
            val inX = x % cell < 6
            val inY = y % cell < 8
            if (inX && inY) {
                val glow = 0.5f + 0.5f * valueNoise(x, y)
                c.set(0.5f + 0.4f * glow, 0.75f + 0.2f * glow, 0.85f, 1f) // lit glass
            } else {
                c.set(wall, wall, wall, 1f) // concrete
            }
        }

    private fun rngBetween(a: Float, b: Float): Float =
        a + rng.nextFloat() * (b - a)
}
