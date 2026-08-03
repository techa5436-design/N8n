package com.agentgame.one

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.jme3.app.AndroidHarness

/**
 * The Lobby. This is a jMonkeyEngine 3D activity: the 3D selected character is rendered and
 * slowly rotates on a pedestal behind the native Android menu buttons which are overlaid on
 * top of the OpenGL surface. Tapping PLAY goes to the mode select screen; the four category
 * buttons open CHARACTERS / WEAPONS / VEHICLES / DRESS.
 */
class LobbyActivity : AndroidHarness() {

    init {
        appClass = LobbyApp::class.java.name
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        eglBitsPerPixel = 24
        eglAlphaBits = 8
        eglDepthBits = 24
        eglSamples = 4
        eglStencilBits = 0
        exitDialogTitle = "Exit Agent Game 1"
        exitDialogMessage = "Leave the lobby?"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)
        buildOverlay()
    }

    private fun buildOverlay() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val overlay = RelativeLayout(this)
        overlay.isClickable = false

        // Title
        val title = TextView(this)
        title.text = resources.getString(R.string.lobby_title)
        title.setTextColor(Color.WHITE)
        title.textSize = 34f
        title.setTypeface(null, Typeface.BOLD)
        title.setLetterSpacing(0.35f)
        title.setShadowLayer(12f, 0f, 0f, Color.rgb(255, 90, 40))
        val lpTitle = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lpTitle.addRule(RelativeLayout.ALIGN_PARENT_TOP)
        lpTitle.addRule(RelativeLayout.CENTER_HORIZONTAL)
        lpTitle.topMargin = dp(24)
        overlay.addView(title, lpTitle)

        // Buttons column (left side)
        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.gravity = android.view.Gravity.CENTER_HORIZONTAL
        val lpCol = RelativeLayout.LayoutParams(
            dp(260),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lpCol.addRule(RelativeLayout.ALIGN_PARENT_START)
        lpCol.addRule(RelativeLayout.CENTER_VERTICAL)
        lpCol.marginStart = dp(40)
        overlay.addView(column, lpCol)

        val btnStyle = GradientDrawable()
        btnStyle.cornerRadius = dp(14).toFloat()
        btnStyle.setColor(Color.rgb(30, 38, 52))

        fun makeButton(label: String, onClick: () -> Unit): Button {
            val b = Button(this)
            b.text = label
            b.setTextColor(Color.WHITE)
            b.textSize = 20f
            b.setTypeface(null, Typeface.BOLD)
            b.setLetterSpacing(0.15f)
            b.background = btnStyle
            b.setOnClickListener { onClick() }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(62)
            )
            lp.topMargin = dp(12)
            column.addView(b, lp)
            return b
        }

        makeButton(resources.getString(R.string.btn_play)) {
            startActivity(Intent(this, ModeSelectActivity::class.java))
        }
        makeButton(resources.getString(R.string.btn_characters)) {
            startActivity(Intent(this, CharactersActivity::class.java))
        }
        makeButton(resources.getString(R.string.btn_weapons)) {
            startActivity(Intent(this, WeaponsActivity::class.java))
        }
        makeButton(resources.getString(R.string.btn_vehicles)) {
            startActivity(Intent(this, VehiclesActivity::class.java))
        }
        makeButton(resources.getString(R.string.btn_dress)) {
            startActivity(Intent(this, DressActivity::class.java))
        }

        // Engine & AI Agent entries
        makeButton("⚙  GAME ENGINE (Godot-style)") {
            startActivity(Intent(this, EngineActivity::class.java))
        }
        makeButton("🤖  AI AGENT (Cline-style)") {
            startActivity(Intent(this, AgentActivity::class.java))
        }

        // Equipped loadout hint (bottom left)
        val loadout = TextView(this)
        loadout.text = "EQUIPPED   ${GameConfig.CHARACTERS[GameConfig.selectedCharacterId].name}  •  " +
                GameConfig.WEAPONS[GameConfig.selectedWeaponId].name + "  •  " +
                GameConfig.VEHICLES[GameConfig.selectedVehicleId].name + "  •  " +
                GameConfig.DRESSES[GameConfig.selectedDressId].name
        loadout.setTextColor(Color.WHITE)
        loadout.textSize = 14f
        loadout.setTypeface(null, Typeface.BOLD)
        loadout.setShadowLayer(6f, 0f, 0f, Color.BLACK)
        val lpLoad = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lpLoad.addRule(RelativeLayout.ALIGN_PARENT_START)
        lpLoad.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lpLoad.marginStart = dp(24)
        lpLoad.bottomMargin = dp(20)
        overlay.addView(loadout, lpLoad)

        // FPS/controls hint (bottom right)
        val hint = TextView(this)
        hint.text = "LEFT STICK — MOVE    RIGHT DRAG — AIM    FIRE — AUTO SHOOT AT CROSSHAIR"
        hint.setTextColor(Color.rgb(255, 200, 150))
        hint.textSize = 13f
        hint.setShadowLayer(6f, 0f, 0f, Color.BLACK)
        val lpHint = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lpHint.addRule(RelativeLayout.ALIGN_PARENT_END)
        lpHint.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lpHint.marginEnd = dp(24)
        lpHint.bottomMargin = dp(20)
        overlay.addView(hint, lpHint)

        addContentView(overlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}
