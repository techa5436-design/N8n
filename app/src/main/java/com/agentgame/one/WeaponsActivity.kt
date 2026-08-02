package com.agentgame.one

import android.app.Activity
import android.os.Bundle

class WeaponsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)

        val images = intArrayOf(
            R.drawable.wp_smg,
            R.drawable.wp_rifle,
            R.drawable.ic_placeholder_weapon,
            R.drawable.ic_placeholder_weapon,
        )
        SectionHelper.show(
            activity = this,
            title = "WEAPONS",
            items = GameConfig.WEAPONS,
            imageIds = images,
            current = GameConfig.selectedWeaponId,
        ) { idx ->
            GameConfig.selectedWeaponId = idx
            GameConfig.persist()
        }
    }
}
