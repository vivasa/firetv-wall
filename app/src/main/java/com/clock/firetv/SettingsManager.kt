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
        private const val KEY_PRESET_URL_PREFIX = "preset_url_"
        private const val KEY_PRESET_NAME_PREFIX = "preset_name_"
        private const val KEY_ACTIVE_PRESET = "active_preset"
        private const val KEY_PRESETS_MIGRATED = "presets_migrated"

        const val FORMAT_12H = 0
        const val FORMAT_24H = 1

        const val PLAYER_SMALL = 0
        const val PLAYER_MEDIUM = 1
        const val PLAYER_LARGE = 2

        const val PRESET_COUNT = 4

        const val THEME_CLASSIC = 0
        const val THEME_GALLERY = 1
        const val THEME_RETRO = 2
        const val THEME_COUNT = 3

        private const val KEY_THEME = "theme"
    }

    var theme: Int
        get() = prefs.getInt(KEY_THEME, THEME_CLASSIC)
        set(value) = prefs.edit().putInt(KEY_THEME, value).apply()

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

    // Preset management
    var activePreset: Int
        get() = prefs.getInt(KEY_ACTIVE_PRESET, -1)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_PRESET, value).apply()

    fun getPresetUrl(index: Int): String =
        prefs.getString("${KEY_PRESET_URL_PREFIX}$index", "") ?: ""

    fun setPresetUrl(index: Int, url: String) =
        prefs.edit().putString("${KEY_PRESET_URL_PREFIX}$index", url).apply()

    fun getPresetName(index: Int): String =
        prefs.getString("${KEY_PRESET_NAME_PREFIX}$index", "Preset ${index + 1}") ?: "Preset ${index + 1}"

    fun setPresetName(index: Int, name: String) =
        prefs.edit().putString("${KEY_PRESET_NAME_PREFIX}$index", name).apply()

    val activeYoutubeUrl: String
        get() {
            val idx = activePreset
            if (idx < 0 || idx >= PRESET_COUNT) return ""
            return getPresetUrl(idx)
        }

    fun migrateFromSingleUrl() {
        if (prefs.getBoolean(KEY_PRESETS_MIGRATED, false)) return
        val oldUrl = prefs.getString(KEY_YOUTUBE_URL, "") ?: ""
        if (oldUrl.isNotBlank()) {
            setPresetUrl(0, oldUrl)
            activePreset = 0
            prefs.edit().remove(KEY_YOUTUBE_URL).apply()
        }
        prefs.edit().putBoolean(KEY_PRESETS_MIGRATED, true).apply()
    }

    fun getPlayerDimensions(): Pair<Int, Int> = when {
        theme == THEME_GALLERY && playerSize == PLAYER_SMALL -> 528 to 297
        theme == THEME_GALLERY && playerSize == PLAYER_MEDIUM -> 640 to 360
        theme == THEME_GALLERY && playerSize == PLAYER_LARGE -> 744 to 418
        theme == THEME_RETRO && playerSize == PLAYER_SMALL -> 384 to 216
        theme == THEME_RETRO && playerSize == PLAYER_MEDIUM -> 480 to 270
        theme == THEME_RETRO && playerSize == PLAYER_LARGE -> 576 to 324
        playerSize == PLAYER_SMALL -> 240 to 135
        playerSize == PLAYER_LARGE -> 426 to 240
        else -> 320 to 180
    }
}
