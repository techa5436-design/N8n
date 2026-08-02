package com.agentgame.one

import com.jme3.math.Vector3f
import java.util.Random

/**
 * Handles the infinite zombie horde. Spawns waves forever (the mode never ends unless the player
 * dies), ramps up the horde size and speed over time, tracks the kill counter, and resolves
 * weapon hits against the horde.
 */
class ZombieManager(private val app: GameApp) {

    private val rng = Random(4242L)

    val zombies = ArrayList<Zombie>()
    var kills = 0

    private var spawnTimer = 0.8f
    private var activeCap = 22
    private var spawnInterval = 1.3f
    private var elapsed = 0f

    /** Called every frame. */
    fun update(tpf: Float) {
        elapsed += tpf

        // Difficulty ramp: the longer you survive, the more and faster they come.
        when {
            elapsed > 150f -> { activeCap = 38; spawnInterval = 0.6f }
            elapsed > 90f  -> { activeCap = 34; spawnInterval = 0.8f }
            elapsed > 45f  -> { activeCap = 30; spawnInterval = 1.0f }
        }

        if (!app.player.dead) {
            spawnTimer -= tpf
            if (spawnTimer <= 0f && zombies.size < activeCap) {
                spawnZombie()
                spawnTimer = spawnInterval
            }
        }

        val it = zombies.iterator()
        while (it.hasNext()) {
            val z = it.next()
            z.update(tpf)
            if (!z.alive) it.remove()
        }
    }

    private fun spawnZombie() {
        val p = app.player.position
        for (attempt in 0 until 24) {
            val ang = rng.nextFloat() * 6.28318f
            val r = 50f + rng.nextFloat() * 30f
            val x = p.x + Math.cos(ang.toDouble()).toFloat() * r
            val z = p.z + Math.sin(ang.toDouble()).toFloat() * r
            if (app.world.inBounds(x, z) && !app.world.isBlocked(x, z)) {
                val spd = (2.2f + rng.nextFloat() * 1.6f)
                zombies.add(Zombie(app, Vector3f(x, app.world.heightAt(x, z), z), spd))
                return
            }
        }
    }

    /** Fire one weapon shot along baseDir; returns true if anything was hit. */
    fun shoot(from: Vector3f, baseDir: Vector3f, stats: GameConfig.WeaponStats): Boolean {
        var anyHit = false
        repeat(stats.pellets) {
            val dir = spreadDir(baseDir, stats.spread)
            val hit = raycastZombie(from, dir, stats.range)
            if (hit != null) {
                anyHit = true
                if (hit.damage(stats.damage.toFloat())) kills++
            }
        }
        return anyHit
    }

    private fun raycastZombie(from: Vector3f, dir: Vector3f, range: Float): Zombie? {
        var best: Zombie? = null
        var bestT = Float.MAX_VALUE
        for (z in zombies) {
            if (!z.alive) continue
            val oc = Vector3f(z.position).subtractLocal(from)
            val proj = oc.dot(dir)
            if (proj < -0.5f) continue
            val perpSq = oc.dot(oc) - proj * proj
            if (perpSq > z.radius * z.radius) continue
            val t = proj - Math.sqrt((z.radius * z.radius - perpSq).toDouble()).toFloat()
            if (t in 0f..range && t < bestT) {
                bestT = t
                best = z
            }
        }
        return best
    }

    /** Perturb a direction slightly to fake bullet spread. */
    private fun spreadDir(base: Vector3f, spread: Float): Vector3f {
        if (spread <= 0f) return base.clone()
        // build an arbitrary perpendicular basis
        val up = Vector3f.UNIT_Y
        val perp1 = up.cross(base).normalizeLocal()
        val perp2 = base.cross(perp1).normalizeLocal()
        val dx = (rng.nextFloat() - 0.5f) * 2f * spread
        val dy = (rng.nextFloat() - 0.5f) * 2f * spread
        return base.clone().addLocal(perp1.mult(dx)).addLocal(perp2.mult(dy)).normalizeLocal()
    }
}
