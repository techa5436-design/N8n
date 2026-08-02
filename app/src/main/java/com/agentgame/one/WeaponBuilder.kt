package com.agentgame.one

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.scene.Geometry
import com.jme3.scene.Node

/**
 * Builds a small procedural gun model held by the character. The visual differs per weapon type
 * so you can see what is equipped. 0=SMG, 1=Rifle, 2=Shotgun, 3=Sniper.
 */
object WeaponBuilder {

    fun build(
        am: AssetManager,
        weaponType: Int,
        bodyMat: Material,
        accentMat: Material,
    ): Node {
        val gun = Node("weapon")
        val grip = Procedural.matLit(am, 0.10f, 0.09f, 0.07f, 6f)

        when (weaponType) {
            0 -> { // SMG — short, compact
                body(gun, am, 0.06f, 0.10f, 0.55f, bodyMat, 0f, 0f, 0f)
                barrel(gun, am, 0.03f, 0.04f, 0.22f, bodyMat, 0f, 0f, -0.38f)
                mag(gun, am, 0.05f, 0.16f, 0.06f, accentMat, 0f, -0.13f, -0.05f)
                gripHandle(gun, am, grip, 0f, -0.08f, 0.08f)
            }
            1 -> { // Rifle — longer with scope
                body(gun, am, 0.06f, 0.11f, 0.90f, bodyMat, 0f, 0f, -0.05f)
                barrel(gun, am, 0.028f, 0.03f, 0.30f, bodyMat, 0f, -0.01f, -0.65f)
                mag(gun, am, 0.05f, 0.18f, 0.06f, accentMat, 0f, -0.14f, -0.15f)
                scope(gun, am, 0.045f, 0.05f, 0.14f, bodyMat, 0f, 0.14f, -0.05f)
                gripHandle(gun, am, grip, 0f, -0.08f, 0.12f)
            }
            2 -> { // Shotgun — wide barrel
                body(gun, am, 0.07f, 0.12f, 0.72f, bodyMat, 0f, 0f, -0.05f)
                barrel(gun, am, 0.05f, 0.05f, 0.40f, bodyMat, 0f, 0f, -0.58f)
                pump(gun, am, 0.06f, 0.09f, 0.22f, accentMat, 0f, 0f, -0.30f)
                gripHandle(gun, am, grip, 0f, -0.08f, 0.14f)
            }
            else -> { // Sniper — very long with big scope
                body(gun, am, 0.05f, 0.10f, 1.20f, bodyMat, 0f, 0f, -0.10f)
                barrel(gun, am, 0.02f, 0.02f, 0.50f, bodyMat, 0f, 0f, -0.90f)
                scope(gun, am, 0.05f, 0.06f, 0.22f, bodyMat, 0f, 0.16f, -0.10f)
                mag(gun, am, 0.04f, 0.16f, 0.05f, accentMat, 0f, -0.13f, -0.20f)
                gripHandle(gun, am, grip, 0f, -0.08f, 0.16f)
            }
        }
        return gun
    }

    private fun box(am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material): Geometry =
        Procedural.box(am, sx, sy, sz, m)

    private fun body(n: Node, am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, sx, sy, sz, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }

    private fun barrel(n: Node, am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, sx, sy, sz, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }

    private fun mag(n: Node, am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, sx, sy, sz, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }

    private fun scope(n: Node, am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, sx, sy, sz, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }

    private fun pump(n: Node, am: AssetManager, sx: Float, sy: Float, sz: Float, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, sx, sy, sz, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }

    private fun gripHandle(n: Node, am: AssetManager, m: Material, x: Float, y: Float, z: Float) {
        val g = box(am, 0.04f, 0.12f, 0.05f, m); n.attachChild(g); g.setLocalTranslation(x, y, z)
    }
}
