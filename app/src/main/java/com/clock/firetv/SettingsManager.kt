package com.clock.firetv

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("firetv_clock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PRIMARY_TZ = "primary_timezone"
        private const val KEY_SECONDARY_TZ = "secondary_timezone"
        private const val KEY_TIME_FORMAT = "time_format"
        private const val KEY_CHIME_ENABLED = "chime_enabled"
        private const val KEY_WALLPAPER_ENABLED = "wallpaper_enabled"
        private const val KEY_WALLPAPER_INTERVAL = "wallpaper_interval"
        private const val KEY_DRIFT_ENABLED = "drift_enabled"
        private const val KEY_NIGHT_DIM_ENABLED = "night_dim_enabled"
        private const val KEY_YOUTUBE_URL = "youtube_url"
        private const val KEY_PLAYER_SIZE = "player_size"
        private const val KEY_PLAYER_VISIBLE = "player_visible"

        const val FORMAT_12H = 0
        const val FORMAT_24H = 1

        const val PLAYER_SMALL = 0
        const val PLAYER_MEDIUM = 1
        const val PLAYER_LARGE = 2
    }

    var primaryTimezone: String
        get() = prefs.getString(KEY_PRIMARY_TZ, "America/New_York") ?: "America/New_York"
        set(value) = prefs.edit().putString(KEY_PRIMARY_TZ, value).apply()

    var secondaryTimezone: String
        get() = prefs.getString(KEY_SECONDARY_TZ, "Asia/Kolkata") ?: "Asia/Kolkata"
        set(value) = prefs.edit().putString(KEY_SECONDARY_TZ, value).apply()

    var timeFormat: Int
        get() = prefs.getInt(KEY_TIME_FORMAT, FORMAT_12H)
        set(value) = prefs.edit().putInt(KEY_TIME_FORMAT, value).apply()

    var chimeEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHIME_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_CHIME_ENABLED, value).apply()

    var wallpaperEnabled: Boolean
        get() = prefs.getBoolean(KEY_WALLPAPER_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WALLPAPER_ENABLED, value).apply()

    var wallpaperIntervalMinutes: Int
        get() = prefs.getInt(KEY_WALLPAPER_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_WALLPAPER_INTERVAL, value).apply()

    var driftEnabled: Boolean
        get() = prefs.getBoolean(KEY_DRIFT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_DRIFT_ENABLED, value).apply()

    var nightDimEnabled: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_DIM_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_DIM_ENABLED, value).apply()

    var youtubeUrl: String
        get() = prefs.getString(KEY_YOUTUBE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_YOUTUBE_URL, value).apply()

    var playerSize: Int
        get() = prefs.getInt(KEY_PLAYER_SIZE, PLAYER_MEDIUM)
        set(value) = prefs.edit().putInt(KEY_PLAYER_SIZE, value).apply()

    var playerVisible: Boolean
        get() = prefs.getBoolean(KEY_PLAYER_VISIBLE, true)
        set(value) = prefs.edit().putBoolean(KEY_PLAYER_VISIBLE, value).apply()

    fun getPlayerDimensions(): Pair<Int, Int> = when (playerSize) {
        PLAYER_SMALL -> 280 to 158
        PLAYER_LARGE -> 500 to 281
        else -> 380 to 214
    }
}
