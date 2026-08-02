package com.agentgame.one

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Renders one of the "select" screens (CHARACTERS / WEAPONS / VEHICLES / DRESS) as a vertical
 * list of cards: a thumbnail, the name/description, and an EQUIP button.
 */
object SectionHelper {

    fun show(
        activity: Activity,
        title: String,
        items: List<GameConfig.Item>,
        imageIds: IntArray,
        current: Int,
        onSelect: (Int) -> Unit,
    ) {
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        val root = LinearLayout(activity)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(10, 13, 18))
        root.setPadding(dp(activity, 20), dp(activity, 16), dp(activity, 20), dp(activity, 20))

        // Header
        val header = TextView(activity).apply {
            text = title
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setLetterSpacing(0.3f)
            setTextColor(Color.WHITE)
            setShadowLayer(8f, 0f, 0f, Color.rgb(255, 90, 40))
        }
        root.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        val back = TextView(activity).apply {
            text = "‹ BACK"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.rgb(140, 160, 200))
            setPadding(0, dp(activity, 4), 0, dp(activity, 8))
            setOnClickListener { activity.finish() }
        }
        root.addView(back, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        // Scrollable item list
        val scroll = ScrollView(activity)
        val list = LinearLayout(activity)
        list.orientation = LinearLayout.VERTICAL
        scroll.addView(list, ScrollView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f
        ))

        for ((idx, item) in items.withIndex()) {
            val equipped = idx == current
            list.addView(itemCard(activity, item, imageIds[idx], equipped) {
                onSelect(idx)
                Toast.makeText(activity, "${item.name} equipped", Toast.LENGTH_SHORT).show()
            })
        }

        activity.setContentView(root)
    }

    private fun itemCard(
        activity: Activity,
        item: GameConfig.Item,
        imageId: Int,
        equipped: Boolean,
        onEquip: () -> Unit,
    ): View {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = android.view.Gravity.CENTER_VERTICAL
        card.setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 10), dp(activity, 10))
        val bg = GradientDrawable().apply {
            cornerRadius = dp(activity, 14).toFloat()
            setColor(if (equipped) Color.rgb(40, 48, 64) else Color.rgb(26, 32, 44))
            setStroke(dp(activity, 2), if (equipped) Color.rgb(255, 138, 0) else Color.rgb(45, 55, 72))
        }
        card.background = bg
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 96)
        )
        lp.topMargin = dp(activity, 10)
        card.layoutParams = lp

        val img = ImageView(activity)
        img.setImageResource(imageId)
        img.scaleType = ImageView.ScaleType.FIT_CENTER
        card.addView(img, LinearLayout.LayoutParams(dp(activity, 84), dp(activity, 84)))

        val textCol = LinearLayout(activity)
        textCol.orientation = LinearLayout.VERTICAL
        textCol.gravity = android.view.Gravity.CENTER_VERTICAL
        val textLp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
        textLp.marginStart = dp(activity, 12)
        card.addView(textCol, textLp)

        textCol.addView(TextView(activity).apply {
            text = item.name
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        textCol.addView(TextView(activity).apply {
            text = item.desc
            textSize = 14f
            setTextColor(Color.rgb(170, 180, 200))
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val btn = TextView(activity).apply {
            text = if (equipped) "EQUIPPED" else "EQUIP"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setTextColor(if (equipped) Color.rgb(255, 200, 120) else Color.WHITE)
        }
        val btnBg = GradientDrawable().apply {
            cornerRadius = dp(activity, 12).toFloat()
            setColor(if (equipped) Color.rgb(80, 60, 20) else Color.rgb(255, 110, 30))
        }
        btn.background = btnBg
        if (!equipped) btn.setOnClickListener { onEquip() }
        card.addView(btn, LinearLayout.LayoutParams(dp(activity, 130), dp(activity, 56)))

        return card
    }

    private fun dp(a: Activity, v: Int): Int =
        (v * a.resources.displayMetrics.density).toInt()
}
