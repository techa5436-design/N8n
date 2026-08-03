package com.agentgame.one

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import kotlin.math.cos
import kotlin.math.sin

/**
 * Third-person player: holds the equipped character model, moves it with the virtual joystick,
 * and keeps the camera behind the player aiming toward the crosshair.
 */
class Player(private val app: GameApp) {

    val position = Vector3f(0f, 0f, 0f)

    /** Player yaw: forward = (sin(yaw), 0, cos(yaw)). */
    var yaw = 0f

    var health = 100f
    var dead = false

    lateinit var model: Node
    var moving = false
    private var walkPhase = 0f

    private val baseSpeed = 6.5f   // m/s base run speed

    fun buildModel() {
        val pal = CharacterBuilder.paletteFor(
            GameConfig.selectedCharacterId,
            GameConfig.selectedDressId,
        )
        model = CharacterBuilder.build(app.assetManager, pal, GameConfig.selectedWeaponId)
        app.world.scene.attachChild(model)
    }

    /** Place the player at a safe spawn point near the map centre. */
    fun spawn() {
        // Walk outwards from the centre to find a non-blocked open spot on the ground.
        var sx = 0f
        var sz = 0f
        outer@ for (r in 0..80) {
            for (a in 0 until 12) {
                val ang = a * (3.14159f / 6f)
                sx = Math.cos(ang.toDouble()).toFloat() * r
                sz = Math.sin(ang.toDouble()).toFloat() * r
                if (!app.world.isBlocked(sx, sz) && app.world.inBounds(sx, sz)) break@outer
            }
        }
        position.set(sx, 0f, sz)
        yaw = 0.8f
        position.y = app.world.heightAt(position.x, position.z)
        health = 100f
        dead = false
        model.setLocalTranslation(position.x, position.y, position.z)
    }

    /** Advance the player by one frame. Input is in the player's local frame. */
    fun update(tpf: Float, inForward: Float, inRight: Float, yawDelta: Float) {
        yaw += yawDelta

        val speed = baseSpeed * GameConfig.VEHICLE_SPEED_MULT[GameConfig.selectedVehicleId]

        val f = Vector3f(sin(yaw), 0f, cos(yaw))
        val r = Vector3f(cos(yaw), 0f, -sin(yaw))

        val wish = f.mult(inForward).addLocal(r.mult(inRight))
        val len = wish.length()
        if (len > 0.001f) {
            wish.multLocal(1f / len)
            val step = speed * tpf
            val nx = position.x + wish.x * step
            val nz = position.z + wish.z * step
            if (app.world.inBounds(nx, nz) && !app.world.isBlocked(nx, nz)) {
                position.x = nx
                position.z = nz
            }
            position.y = app.world.heightAt(position.x, position.z)
            moving = true
        } else {
            moving = false
        }

        updateCamera()
        updateModel(tpf)
    }

    private fun updateCamera() {
        val f = Vector3f(sin(yaw), 0f, cos(yaw))
        val camPos = position.add(f.mult(-7f)).add(0f, 4.2f, 0f)
        app.camera.setLocation(camPos)
        app.camera.lookAt(position.add(0f, 1.6f, 0f), Vector3f.UNIT_Y)
    }

    private fun updateModel(tpf: Float) {
        model.setLocalTranslation(position.x, position.y, position.z)
        CharacterBuilder.faceYaw(model, yaw)
        if (moving) {
            walkPhase += tpf * 9f
            CharacterBuilder.animateWalk(model, walkPhase, 0.5f)
        } else {
            CharacterBuilder.animateWalk(model, walkPhase, 0f)
        }
    }

    fun takeDamage(dmg: Float) {
        if (dead) return
        health -= dmg
        if (health <= 0f) {
            health = 0f
            dead = true
        }
    }

    /** Reset the whole player node rotation/position helpers. */
    fun faceYaw() {
        model.setLocalRotation(Quaternion().fromAngles(0f, yaw, 0f))
    }
}
