package com.agentgame.one

import android.app.Activity
import android.os.Bundle

class DressActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)

        val images = intArrayOf(
            R.drawable.ic_placeholder_dress,
            R.drawable.ic_placeholder_dress,
            R.drawable.ic_placeholder_dress,
            R.drawable.ic_placeholder_dress,
        )
        SectionHelper.show(
            activity = this,
            title = "DRESS",
            items = GameConfig.DRESSES,
            imageIds = images,
            current = GameConfig.selectedDressId,
        ) { idx ->
            GameConfig.selectedDressId = idx
            GameConfig.persist()
        }
    }
}
