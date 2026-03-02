package com.clock.firetv

import android.content.Context
import java.util.UUID

class DeviceIdentity(context: Context) {

    private val prefs = context.getSharedPreferences("firetv_clock_prefs", Context.MODE_PRIVATE)

    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: generateAndStore()

    val deviceName: String
        get() = prefs.getString(KEY_DEVICE_NAME, null) ?: generateName()

    private fun generateAndStore(): String {
        val id = UUID.randomUUID().toString()
        val name = buildName()
        prefs.edit()
            .putString(KEY_DEVICE_ID, id)
            .putString(KEY_DEVICE_NAME, name)
            .apply()
        return id
    }

    private fun generateName(): String {
        val name = buildName()
        prefs.edit().putString(KEY_DEVICE_NAME, name).apply()
        return name
    }

    private fun buildName(): String {
        val adj = ADJECTIVES.random()
        val noun = NOUNS.random()
        return "$adj $noun"
    }

    companion object {
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"

        private val ADJECTIVES = arrayOf(
            "Amber", "Bronze", "Cedar", "Copper", "Coral",
            "Crimson", "Dusty", "Ember", "Golden", "Ivory",
            "Jade", "Misty", "Mossy", "Ochre", "Onyx",
            "Russet", "Sage", "Sienna", "Slate", "Velvet"
        )

        private val NOUNS = arrayOf(
            "Beacon", "Candle", "Chime", "Crest", "Dusk",
            "Flame", "Glen", "Harbor", "Hearth", "Hollow",
            "Lantern", "Lodge", "Mantle", "Meadow", "Nook",
            "Perch", "Ridge", "Spire", "Terrace", "Vale"
        )
    }
}
