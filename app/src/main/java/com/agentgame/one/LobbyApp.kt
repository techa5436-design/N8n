package com.agentgame.one

import com.jme3.app.SimpleApplication
import com.jme3.light.AmbientLight
import com.jme3.light.DirectionalLight
import com.jme3.math.ColorRGBA
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Node
import com.jme3.scene.shape.Sphere

/**
 * The 3D lobby scene. The equipped character (selected in CHARACTERS / DRESS / WEAPONS) is shown
 * standing on a pedestal, slowly rotating, lit with game lighting — with the native Android menu
 * buttons overlaid on top by [LobbyActivity].
 */
class LobbyApp : SimpleApplication() {

    private var charNode: Node? = null
    private var sky: Geometry? = null

    override fun simpleInitApp() {
        setDisplayFps(false)
        setDisplayStatView(false)
        setPauseOnLostFocus(false)
        flyCam.isEnabled = false

        addLights()
        addSky()

        // Lobby floor
        val floorMat = Procedural.matLit(assetManager, 0.16f, 0.17f, 0.2f, 40f)
        val floor = Geometry("floor", com.jme3.scene.shape.Box(60f, 0.5f, 60f))
        floor.material = floorMat
        floor.setLocalTranslation(0f, -1f, 0f)
        rootNode.attachChild(floor)

        // Pedestal
        val pedMat = Procedural.matLit(assetManager, 0.35f, 0.36f, 0.42f, 60f)
        val pedestal = Procedural.cylinder(assetManager, 1.3f, 0.9f, pedMat)
        pedestal.setLocalTranslation(0f, 0.45f, 0f)
        rootNode.attachChild(pedestal)

        val ringMat = Procedural.matLit(assetManager, 1f, 0.54f, 0f, 80f)
        val ring = Procedural.cylinder(assetManager, 1.45f, 0.1f, ringMat)
        ring.setLocalTranslation(0f, 0.95f, 0f)
        rootNode.attachChild(ring)

        // Character on the pedestal
        val pal = CharacterBuilder.paletteFor(
            GameConfig.selectedCharacterId,
            GameConfig.selectedDressId,
        )
        charNode = CharacterBuilder.build(assetManager, pal, GameConfig.selectedWeaponId)
        charNode!!.setLocalTranslation(0f, 0.95f, 0f)
        rootNode.attachChild(charNode)

        // Frame the character
        cam.setLocation(Vector3f(0f, 2.2f, 7f))
        cam.lookAt(Vector3f(0f, 1.9f, 0f), Vector3f.UNIT_Y)
    }

    override fun simpleUpdate(tpf: Float) {
        charNode?.rotate(0f, tpf * 0.7f, 0f)
        sky?.setLocalTranslation(cam.getLocation())
    }

    private fun addLights() {
        rootNode.addLight(DirectionalLight().apply {
            setDirection(Vector3f(-0.5f, -0.8f, -0.4f).normalizeLocal())
            setColor(ColorRGBA(1f, 0.98f, 0.9f, 1f))
        })
        rootNode.addLight(AmbientLight().apply { setColor(ColorRGBA(0.5f, 0.5f, 0.55f, 1f)) })
    }

    private fun addSky() {
        val m = com.jme3.material.Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        val tex = Procedural.paintTexture(128, 128) { x, y, c ->
            val t = y / 127f
            c.interpolateLocal(ColorRGBA(0.7f, 0.45f, 0.28f, 1f), ColorRGBA(0.12f, 0.22f, 0.4f, 1f), t)
        }
        m.setTexture("ColorMap", tex)
        m.additionalRenderState.isDepthWrite = false
        sky = Geometry("sky", Sphere(32, 32, 400f))
        sky!!.material = m
        rootNode.attachChild(sky)
    }
}
