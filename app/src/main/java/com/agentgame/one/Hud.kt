package com.agentgame.one

import com.jme3.font.BitmapFont
import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.math.ColorRGBA
import com.jme3.scene.Geometry
import com.jme3.scene.shape.Quad

/**
 * On-screen HUD drawn on the GUI layer: kill counter (top centre), health bar (top left),
 * infinite-ammo badge (top right), survival timer, a crosshair, and a death overlay.
 */
class Hud(private val app: GameApp) {

    private lateinit var font: BitmapFont
    private lateinit var killText: BitmapText
    private lateinit var ammoText: BitmapText
    private lateinit var timeText: BitmapText
    private lateinit var crosshair: BitmapText
    private lateinit var hpBarBg: Geometry
    private lateinit var hpBarFg: Geometry
    private lateinit var deathTitle: BitmapText
    private lateinit var deathInfo: BitmapText
    private var deathShown = false

    fun init() {
        font = app.assetManager.loadFont("Interface/Fonts/Default.fnt")
        val gui = app.guiNode
        val cw = app.cam.getWidth().toFloat()
        val ch = app.cam.getHeight().toFloat()

        // Health bar (background + foreground)
        hpBarBg = quad(360f, 22f, ColorRGBA(0.15f, 0.15f, 0.18f, 1f))
        hpBarBg.setLocalTranslation(24f, ch - 46f, 0f)
        gui.attachChild(hpBarBg)

        hpBarFg = quad(360f, 22f, ColorRGBA(0.2f, 0.9f, 0.3f, 1f))
        hpBarFg.setLocalTranslation(24f, ch - 46f, 0f)
        gui.attachChild(hpBarFg)

        // Kill counter, top centre
        killText = BitmapText(font, false).apply {
            size = 40f
            color = ColorRGBA(1f, 0.95f, 0.85f, 1f)
            alignment = BitmapFont.Align.Center
            setLocalTranslation(cw / 2f - 120f, ch - 60f, 0f)
        }
        gui.attachChild(killText)

        // Infinite ammo badge, top right
        ammoText = BitmapText(font, false).apply {
            size = 30f
            color = ColorRGBA(1f, 0.8f, 0.2f, 1f)
            alignment = BitmapFont.Align.Right
        }
        gui.attachChild(ammoText)

        // Survival timer, under the kill counter
        timeText = BitmapText(font, false).apply {
            size = 26f
            color = ColorRGBA(0.8f, 0.85f, 1f, 1f)
            alignment = BitmapFont.Align.Center
            setLocalTranslation(cw / 2f - 100f, ch - 100f, 0f)
        }
        gui.attachChild(timeText)

        // Crosshair
        crosshair = BitmapText(font, false).apply {
            text = "+"
            size = 40f
            color = ColorRGBA(1f, 1f, 1f, 1f)
            alignment = BitmapFont.Align.Center
            setLocalTranslation(cw / 2f - 20f, ch / 2f - 30f, 0f)
        }
        gui.attachChild(crosshair)

        // Death overlay (hidden until player dies)
        deathTitle = BitmapText(font, false).apply {
            text = "YOU DIED"
            size = 80f
            color = ColorRGBA(1f, 0.2f, 0.2f, 1f)
            alignment = BitmapFont.Align.Center
            setLocalTranslation(cw / 2f - 180f, ch / 2f + 60f, 0f)
        }
        deathTitle.isVisible = false
        gui.attachChild(deathTitle)

        deathInfo = BitmapText(font, false).apply {
            size = 34f
            color = ColorRGBA(1f, 1f, 1f, 1f)
            alignment = BitmapFont.Align.Center
            setLocalTranslation(cw / 2f - 200f, ch / 2f, 0f)
        }
        deathInfo.isVisible = false
        gui.attachChild(deathInfo)
    }

    fun update(tpf: Float) {
        val cw = app.cam.getWidth().toFloat()
        val ch = app.cam.getHeight().toFloat()
        // Health bar colour + scale
        val h = app.player.health / 100f
        hpBarFg.setLocalScale(h.coerceIn(0f, 1f), 1f, 1f)
        val col = ColorRGBA(1f - h, h, 0.2f, 1f)
        (hpBarFg.material as Material).setColor("Color", col)

        killText.text = "KILLS  ${app.zombieManager.kills}"

        val mm = app.elapsed.toInt() / 60
        val ss = app.elapsed.toInt() % 60
        timeText.text = "SURVIVED  %02d:%02d".format(mm, ss)

        val wid = cw
        ammoText.setLocalTranslation(wid - 24f, ch - 46f, 0f)
        ammoText.text = "AMMO ∞  (${GameConfig.WEAPONS[GameConfig.selectedWeaponId].name})"

        // Death overlay
        if (app.player.dead && !deathShown) {
            deathShown = true
            deathTitle.isVisible = true
            deathInfo.isVisible = true
            deathInfo.text = "FINAL SCORE  ${app.zombieManager.kills} kills\nTAP TO EXIT"
        }
    }

    private fun quad(w: Float, h: Float, color: ColorRGBA): Geometry {
        val m = Material(app.assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
        m.setColor("Color", color)
        val g = Geometry("hudquad", Quad(w, h))
        g.material = m
        return g
    }
}
