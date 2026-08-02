package com.agentgame.one

import android.app.Activity
import android.os.Bundle

class CharactersActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)

        val images = intArrayOf(R.drawable.ch_1, R.drawable.ch_2, R.drawable.ch_3, R.drawable.ch_4)
        SectionHelper.show(
            activity = this,
            title = "CHARACTERS",
            items = GameConfig.CHARACTERS,
            imageIds = images,
            current = GameConfig.selectedCharacterId,
        ) { idx ->
            GameConfig.selectedCharacterId = idx
            GameConfig.persist()
        }
    }
}
