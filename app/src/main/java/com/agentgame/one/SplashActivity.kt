package com.agentgame.one

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

/**
 * Free Fire-style splash screen: full-bleed cinematic background, the game logo fading in,
 * then automatically proceeds to the Lobby.
 */
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        GameConfig.init(this)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val root = RelativeLayout(this)
        root.setBackgroundResource(R.drawable.splash_bg)

        val logo = ImageView(this)
        logo.setImageResource(R.drawable.game_logo)
        logo.scaleType = ImageView.ScaleType.FIT_CENTER
        val lpLogo = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        lpLogo.addRule(RelativeLayout.CENTER_IN_PARENT)
        root.addView(logo, lpLogo)

        val tagline = TextView(this)
        tagline.text = resources.getString(R.string.splash_tagline)
        tagline.setTextColor(Color.rgb(255, 178, 80))
        tagline.textSize = 22f
        tagline.setLetterSpacing(0.3f)
        tagline.gravity = android.view.Gravity.CENTER
        val lpTag = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        lpTag.addRule(RelativeLayout.CENTER_HORIZONTAL)
        lpTag.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        lpTag.bottomMargin = (resources.displayMetrics.density * 80).toInt()
        root.addView(tagline, lpTag)

        val fade = AlphaAnimation(0f, 1f)
        fade.duration = 1200
        fade.fillAfter = true
        logo.startAnimation(fade)

        setContentView(root)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LobbyActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3200)
    }
}
