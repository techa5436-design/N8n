package com.agentgame.one

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Shown after tapping PLAY in the lobby. Lists the game modes; selecting one enables the big
 * START button which launches the match.
 */
class ModeSelectActivity : Activity() {

    private var selectedMode = 0 // 0 = Infinite Zombie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(10, 13, 18))
        root.setPadding(dp(24), dp(20), dp(24), dp(24))
        setContentView(root)

        val title = TextView(this).apply {
            text = "SELECT MODE"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setLetterSpacing(0.3f)
            setTextColor(Color.WHITE)
            setShadowLayer(8f, 0f, 0f, Color.rgb(255, 90, 40))
        }
        root.addView(title, lpWrap())

        val back = TextView(this).apply {
            text = "‹ BACK"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(140, 160, 200))
            setPadding(0, dp(4), 0, dp(10))
            setOnClickListener { finish() }
        }
        root.addView(back, lpWrap())

        // --- Infinite Zombie (the playable mode) ---
        val zombie = modeCard(
            "INFINITE ZOMBIE",
            "Never-ending waves · Unlimited ammo · Kills counted on screen · Survive as long as you can",
            "PLAYABLE",
        )
        root.addView(zombie, lpWrap())
        zombie.setOnClickListener {
            selectedMode = 0
            zombie.bringToFront()
        }

        // --- Coming soon modes ---
        val br = modeCard("BATTLE ROYALE", "100 players · Last one standing", "COMING SOON")
        root.addView(br, lpWrap())
        val td = modeCard("TEAM DEATHMATCH", "4v4 squad battles", "COMING SOON")
        root.addView(td, lpWrap())

        root.addView(TextView(this).apply {
            text = "EQUIPPED  ${GameConfig.CHARACTERS[GameConfig.selectedCharacterId].name}  •  ${GameConfig.WEAPONS[GameConfig.selectedWeaponId].name}"
            textSize = 14f
            setTextColor(Color.rgb(160, 170, 190))
            setPadding(0, dp(12), 0, dp(6))
        }, lpWrap())

        // --- START ---
        val start = TextView(this).apply {
            text = "START"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setLetterSpacing(0.2f)
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.WHITE)
        }
        val startBg = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.rgb(255, 110, 30))
        }
        start.background = startBg
        start.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        val startLp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(70)
        )
        startLp.topMargin = dp(18)
        root.addView(start, startLp)
    }

    private fun modeCard(title: String, desc: String, badge: String): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        val bg = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(Color.rgb(26, 32, 44))
            setStroke(dp(2), Color.rgb(45, 55, 72))
        }
        card.background = bg
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(12)
        card.layoutParams = lp

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        card.addView(row, lpWrap())

        row.addView(TextView(this).apply {
            text = title
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(this).apply {
            text = badge
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (badge == "PLAYABLE") Color.rgb(120, 255, 150) else Color.rgb(180, 180, 190))
        }, lpWrap())

        card.addView(TextView(this).apply {
            text = desc
            textSize = 14f
            setTextColor(Color.rgb(170, 180, 200))
            setPadding(0, dp(6), 0, 0)
        }, lpWrap())
        return card
    }

    private fun lpWrap(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
