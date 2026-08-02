package com.agentgame.one

import android.app.Activity
import android.os.Bundle

class VehiclesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GameConfig.init(this)

        val images = intArrayOf(
            R.drawable.ic_placeholder_vehicle,
            R.drawable.ic_placeholder_vehicle,
            R.drawable.ic_placeholder_vehicle,
        )
        SectionHelper.show(
            activity = this,
            title = "VEHICLES",
            items = GameConfig.VEHICLES,
            imageIds = images,
            current = GameConfig.selectedVehicleId,
        ) { idx ->
            GameConfig.selectedVehicleId = idx
            GameConfig.persist()
        }
    }
}
