package com.agentgame.one

import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * A single zombie. Simple seek-toward-player AI with building avoidance and a melee attack.
 */
class Zombie(private val app: GameApp, start: Vector3f, val speed: Float) {

    var alive = true
    var health = 100f
    val radius = 0.6f
    val position = Vector3f(start)

    lateinit var model: Node
    private var walkPhase = 0f
    private var attackCooldown = 0f
    private var hitFlash = 0f

    init {
        val pal = CharacterBuilder.Palette(
            skin = ColorRGBA(0.55f, 0.72f, 0.40f, 1f),   // undead green
            primary = ColorRGBA(0.16f, 0.22f, 0.13f, 1f),
            secondary = ColorRGBA(0.10f, 0.12f, 0.09f, 1f),
            accent = ColorRGBA(0.5f, 0.0f, 0.05f, 1f),
        )
        model = CharacterBuilder.build(app.assetManager, pal, 0)
        // Zombies don't carry guns.
        CharacterBuilder.getWeapon(model)?.removeFromParent()
        model.setLocalScale(1.05f, 1.05f, 1.05f)
        app.world.scene.attachChild(model)
        model.setLocalTranslation(position.x, position.y, position.z)
    }

    /** Apply damage; returns true if this hit killed the zombie. */
    fun damage(d: Float): Boolean {
        if (!alive) return false
        health -= d
        hitFlash = 0.12f
        if (health <= 0f) {
            alive = false
            die()
            return true
        }
        return false
    }

    private fun die() {
        model.removeFromParent()
    }

    fun update(tpf: Float) {
        if (!alive) return
        attackCooldown -= tpf
        hitFlash -= tpf

        val p = app.player.position
        val dx = p.x - position.x
        val dz = p.z - position.z
        val dist = sqrt(dx * dx + dz * dz)

        if (dist > 1.8f) {
            val nx = dx / (dist + 0.0001f)
            val nz = dz / (dist + 0.0001f)
            val step = speed * tpf
            val tx = position.x + nx * step
            val tz = position.z + nz * step
            if (app.world.inBounds(tx, tz) && !app.world.isBlocked(tx, tz)) {
                position.x = tx
                position.z = tz
            }
            position.y = app.world.heightAt(position.x, position.z)
            walkPhase += tpf * 7f
            CharacterBuilder.animateWalk(model, walkPhase, 0.55f)
        } else {
            // attack the player
            if (attackCooldown <= 0f) {
                app.player.takeDamage(9f)
                attackCooldown = 1.0f
            }
            CharacterBuilder.animateWalk(model, walkPhase, 0f)
        }

        val yaw = atan2(dx, dz)
        CharacterBuilder.faceYaw(model, yaw)
        model.setLocalTranslation(position.x, position.y, position.z)
    }
}
