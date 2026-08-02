package com.agentgame.one

import android.content.Context
import android.content.SharedPreferences

/**
 * Holds the player's selections (character, weapon, vehicle, dress) in memory so they can be
 * shared between the Android menu screens and the jMonkeyEngine game, and persisted so the
 * choices survive restarts.
 */
object GameConfig {

    // ---- Selections (mirror of what the player picked in the menus) ----
    var selectedCharacterId: Int = 0          // index into CHARACTERS
    var selectedWeaponId: Int = 0             // index into WEAPONS
    var selectedVehicleId: Int = 0            // index into VEHICLES
    var selectedDressId: Int = 0              // index into DRESSES

    // ---- Catalogues shown in the menus ----
    data class Item(val id: Int, val name: String, val desc: String)

    val CHARACTERS = listOf(
        Item(0, "STRIKER", "Balanced assault soldier"),
        Item(1, "VIPER", "Fast female specialist"),
        Item(2, "TITAN", "Heavy armor defender"),
        Item(3, "GHOST", "Stealth close-range agent"),
    )

    val WEAPONS = listOf(
        Item(0, "RAPTOR SMG", "High fire rate, medium damage"),
        Item(1, "PHANTOM RIFLE", "Balanced automatic rifle"),
        Item(2, "THUNDER SHOTGUN", "Heavy burst, close range"),
        Item(3, "VIPER SNIPER", "High damage, long range"),
    )

    val VEHICLES = listOf(
        Item(0, "OFFROAD BUGGY", "Fast agile buggy"),
        Item(1, "PANZER TRUCK", "Heavy armored truck"),
        Item(2, "RAPTOR BIKE", "Ultra fast motorcycle"),
    )

    val DRESSES = listOf(
        Item(0, "COMBAT CAMO", "Classic jungle camo"),
        Item(1, "NIGHT OPS", "Black tactical gear"),
        Item(2, "TITAN ARMOR", "Reinforced plate armor"),
        Item(3, "PHOENIX GOLD", "Golden battle gear"),
    )

    /** Weapon stats used inside the game. Order matches WEAPONS above. */
    data class WeaponStats(
        val damage: Int,
        val fireRate: Float,   // shots per second
        val range: Float,      // metres
        val spread: Float,     // radians
        val pellets: Int,      // bullets fired per shot
    )

    val WEAPON_STATS = listOf(
        WeaponStats(damage = 34, fireRate = 12f, range = 220f, spread = 0.010f, pellets = 1),
        WeaponStats(damage = 48, fireRate = 8f,  range = 300f, spread = 0.006f, pellets = 1),
        WeaponStats(damage = 90, fireRate = 1.4f, range = 60f, spread = 0.045f, pellets = 6),
        WeaponStats(damage = 160, fireRate = 0.9f, range = 500f, spread = 0.002f, pellets = 1),
    )

    /** Movement speed boost per vehicle (index into VEHICLES). */
    val VEHICLE_SPEED_MULT = listOf(1.0f, 0.85f, 1.15f)

    // ---- SharedPreferences persistence ----
    private const val PREFS = "agent_game_1_prefs"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            selectedCharacterId = prefs.getInt("character", 0)
            selectedWeaponId = prefs.getInt("weapon", 0)
            selectedVehicleId = prefs.getInt("vehicle", 0)
            selectedDressId = prefs.getInt("dress", 0)
        }
    }

    fun persist() {
        if (::prefs.isInitialized) {
            prefs.edit()
                .putInt("character", selectedCharacterId)
                .putInt("weapon", selectedWeaponId)
                .putInt("vehicle", selectedVehicleId)
                .putInt("dress", selectedDressId)
                .apply()
        }
    }
}
