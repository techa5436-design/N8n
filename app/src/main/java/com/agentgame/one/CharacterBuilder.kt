package com.agentgame.one

import com.jme3.asset.AssetManager
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.Spatial

/**
 * Builds a fully procedural, articulated humanoid "agent" (player or base body for other
 * characters). The returned Node has its root at the feet so placing it at a world position
 * stands it on the ground. Limbs are exposed as named child nodes (hip, torso, head, armL,
 * armR, legL, legR) so the game can swing them for a walk animation.
 */
object CharacterBuilder {

    data class Palette(
        val skin: ColorRGBA,
        val primary: ColorRGBA,
        val secondary: ColorRGBA,
        val accent: ColorRGBA,
    )

    /** Final look = character palette + dress recolor (dress always wins for armor pieces). */
    fun paletteFor(characterId: Int, dressId: Int): Palette {
        val base = when (characterId) {
            1 -> Palette(skin(0xE8B78A), c(0x0E, 0xBF, 0xC0), c(0x11, 0x1B, 0x2A), c(0x4A, 0xE8, 0xC8)) // VIPER
            2 -> Palette(skin(0xC98A5A), c(0x6B, 0x6E, 0x76), c(0x39, 0x3B, 0x41), c(0xF2, 0xC1, 0x3E)) // TITAN
            3 -> Palette(skin(0xE0AC7C), c(0x14, 0x12, 0x1A), c(0x2A, 0x24, 0x38), c(0x9B, 0x59, 0xFF)) // GHOST
            else -> Palette(skin(0xE0A87C), c(0xFF, 0x8A, 0x00), c(0x20, 0x28, 0x35), c(0xFF, 0x4B, 0x2B)) // STRIKER
        }
        val dress = when (dressId) {
            1 -> c(0x18, 0x1C, 0x22)   // Night Ops
            2 -> c(0x8C, 0x92, 0xA0)   // Titan armor
            3 -> c(0xF2, 0xB8, 0x3C)   // Phoenix gold
            else -> c(0x5A, 0x6B, 0x3A) // Combat camo
        }
        return Palette(base.skin, dress, base.secondary, base.accent)
    }

    /**
     * Builds the humanoid. Returns a Node containing all parts and a reference to the held weapon.
     */
    fun build(am: AssetManager, p: Palette, weaponType: Int): Node {
        val model = Node("agent")
        val pM = { r: Float, g: Float, b: Float -> Procedural.matLit(am, r, g, b, 24f) }

        // Body proportions (half-sizes for Box) in metres.
        val matSkin = Procedural.matLit(am, p.skin.r, p.skin.g, p.skin.b, 12f)
        val matPrimary = Procedural.matLit(am, p.primary.r, p.primary.g, p.primary.b, 20f)
        val matSecondary = Procedural.matLit(am, p.secondary.r, p.secondary.g, p.secondary.b, 16f)
        val matAccent = Procedural.matLit(am, p.accent.r, p.accent.g, p.accent.b, 40f)
        val matGun = pM(0.12f, 0.13f, 0.15f)
        val matGunAccent = Procedural.matLit(am, 1f, 0.54f, 0f, 40f)

        // --- hips (root) ---
        val hips = Node("hips")
        hips.setLocalTranslation(0f, 0.95f, 0f)
        model.attachChild(hips)

        // --- legs (pivot at hip) ---
        val legL = Node("legL"); hips.attachChild(legL)
        val legR = Node("legR"); hips.attachChild(legR)
        legL.setLocalTranslation(-0.15f, -0.05f, 0f)
        legR.setLocalTranslation(0.15f, -0.05f, 0f)
        val legBox = Procedural.box(am, 0.11f, 0.42f, 0.13f, matSecondary)
        legBox.setLocalTranslation(0f, -0.42f, 0f)
        legL.attachChild(legBox)
        val legBox2 = Procedural.box(am, 0.11f, 0.42f, 0.13f, matSecondary)
        legBox2.setLocalTranslation(0f, -0.42f, 0f)
        legR.attachChild(legBox2)
        // boots
        val boot = Procedural.box(am, 0.12f, 0.12f, 0.20f, matAccent)
        boot.setLocalTranslation(0f, -0.82f, 0.04f)
        legL.attachChild(boot)
        val boot2 = Procedural.box(am, 0.12f, 0.12f, 0.20f, matAccent)
        boot2.setLocalTranslation(0f, -0.82f, 0.04f)
        legR.attachChild(boot2)

        // --- torso (pivot at hip) ---
        val torso = Node("torso"); hips.attachChild(torso)
        val chest = Procedural.box(am, 0.34f, 0.55f, 0.22f, matPrimary)
        chest.setLocalTranslation(0f, 0.33f, 0f)
        torso.attachChild(chest)
        // chest plate / vest accent
        val plate = Procedural.box(am, 0.36f, 0.34f, 0.24f, matSecondary)
        plate.setLocalTranslation(0f, 0.36f, 0f)
        torso.attachChild(plate)
        // belt
        val belt = Procedural.box(am, 0.36f, 0.09f, 0.24f, matAccent)
        belt.setLocalTranslation(0f, 0.06f, 0f)
        torso.attachChild(belt)

        // --- head (pivot at neck) ---
        val head = Node("head"); torso.attachChild(head)
        head.setLocalTranslation(0f, 0.65f, 0f)
        val skull = Procedural.box(am, 0.19f, 0.24f, 0.20f, matSkin)
        skull.setLocalTranslation(0f, 0.12f, 0f)
        head.attachChild(skull)
        // helmet
        val helmet = Procedural.box(am, 0.20f, 0.14f, 0.21f, matPrimary)
        helmet.setLocalTranslation(0f, 0.20f, 0f)
        head.attachChild(helmet)
        val visor = Procedural.box(am, 0.15f, 0.05f, 0.03f, matAccent)
        visor.setLocalTranslation(0f, 0.16f, 0.10f)
        head.attachChild(visor)

        // --- arms (pivot at shoulder) ---
        val armL = Node("armL"); torso.attachChild(armL)
        val armR = Node("armR"); torso.attachChild(armR)
        armL.setLocalTranslation(-0.44f, 0.55f, 0f)
        armR.setLocalTranslation(0.44f, 0.55f, 0f)
        for (arm in listOf(armL, armR)) {
            val upper = Procedural.box(am, 0.11f, 0.38f, 0.13f, matSecondary)
            upper.setLocalTranslation(0f, -0.19f, 0f)
            arm.attachChild(upper)
            val hand = Procedural.box(am, 0.09f, 0.10f, 0.10f, matSkin)
            hand.setLocalTranslation(0f, -0.42f, 0f)
            arm.attachChild(hand)
        }

        // --- held weapon (attached to right hand) ---
        val weapon = WeaponBuilder.build(am, weaponType, matGun, matGunAccent)
        weapon.setLocalTranslation(0.0f, -0.42f, 0.30f)
        weapon.rotate(0f, 3.14159f, 0f) // flip so the barrel points forward (+Z)
        armR.attachChild(weapon)

        // Store weapon on the node for lookup
        model.setUserData("weapon", weapon)
        return model
    }

    /** Find a spatial by name anywhere in the tree (limbs are nested under hips/torso). */
    private fun findChild(root: Node, name: String): Spatial? {
        if (root.name == name) return root
        for (i in 0 until root.children.size) {
            val c = root.getChild(i) ?: continue
            if (c is Node) {
                val found = findChild(c, name)
                if (found != null) return found
            }
        }
        return null
    }

    /** Swing the legs/arms for a walk cycle given a phase. */
    fun animateWalk(model: Node, phase: Float, swingAmount: Float) {
        val legL = findChild(model, "legL") as? Node
        val legR = findChild(model, "legR") as? Node
        val armL = findChild(model, "armL") as? Node
        val armR = findChild(model, "armR") as? Node
        val sin = Math.sin(phase).toFloat()
        legL?.setLocalRotation(Quaternion().fromAngles(sin * swingAmount, 0f, 0f))
        legR?.setLocalRotation(Quaternion().fromAngles(-sin * swingAmount, 0f, 0f))
        armL?.setLocalRotation(Quaternion().fromAngles(-sin * swingAmount * 0.7f, 0f, 0f))
        armR?.setLocalRotation(Quaternion().fromAngles(sin * swingAmount * 0.7f, 0f, 0f))
    }

    /** Rotate the upper body toward the camera yaw so the character faces where it shoots. */
    fun faceYaw(model: Node, yaw: Float) {
        model.setLocalRotation(Quaternion().fromAngles(0f, yaw, 0f))
    }

    fun getWeapon(model: Node): Spatial? =
        model.getUserData("weapon") as? Spatial

    private fun c(r: Int, g: Int, b: Int): ColorRGBA =
        ColorRGBA(r / 255f, g / 255f, b / 255f, 1f)

    private fun skin(rgb: Int): ColorRGBA =
        ColorRGBA(((rgb shr 16) and 0xFF) / 255f, ((rgb shr 8) and 0xFF) / 255f, (rgb and 0xFF) / 255f, 1f)
}
