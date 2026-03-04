package com.mantle.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject

data class Preset(val name: String, val url: String)

data class ClockConfig(
    val theme: Int = 0,
    val primaryTimezone: String = "America/New_York",
    val secondaryTimezone: String = "Asia/Kolkata",
    val timeFormat: Int = 0,
    val nightDimEnabled: Boolean = true,
    val driftEnabled: Boolean = true
)

data class WallpaperConfig(
    val enabled: Boolean = true,
    val intervalMinutes: Int = 5
)

data class ChimeConfig(
    val enabled: Boolean = true
)

data class PlayerConfig(
    val size: Int = 1,
    val visible: Boolean = true,
    val activePreset: Int = -1,
    val presets: List<Preset> = emptyList()
)

data class MantleConfig(
    val version: Int = 1,
    val clock: ClockConfig = ClockConfig(),
    val wallpaper: WallpaperConfig = WallpaperConfig(),
    val chime: ChimeConfig = ChimeConfig(),
    val player: PlayerConfig = PlayerConfig()
)

class MantleConfigStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "mantle_config"
        private const val KEY_CONFIG = "config"
        private const val MAX_PRESETS = 20
    }

    interface OnConfigChangedListener {
        fun onConfigChanged(config: MantleConfig)
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableListOf<OnConfigChangedListener>()

    var config: MantleConfig = load()
        private set

    fun addListener(listener: OnConfigChangedListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnConfigChangedListener) {
        listeners.remove(listener)
    }

    // --- Clock settings ---

    fun setTheme(theme: Int) = update { it.copy(clock = it.clock.copy(theme = theme)) }
    fun setPrimaryTimezone(tz: String) = update { it.copy(clock = it.clock.copy(primaryTimezone = tz)) }
    fun setSecondaryTimezone(tz: String) = update { it.copy(clock = it.clock.copy(secondaryTimezone = tz)) }
    fun setTimeFormat(format: Int) = update { it.copy(clock = it.clock.copy(timeFormat = format)) }
    fun setNightDimEnabled(enabled: Boolean) = update { it.copy(clock = it.clock.copy(nightDimEnabled = enabled)) }
    fun setDriftEnabled(enabled: Boolean) = update { it.copy(clock = it.clock.copy(driftEnabled = enabled)) }

    // --- Wallpaper settings ---

    fun setWallpaperEnabled(enabled: Boolean) = update { it.copy(wallpaper = it.wallpaper.copy(enabled = enabled)) }
    fun setWallpaperInterval(minutes: Int) = update { it.copy(wallpaper = it.wallpaper.copy(intervalMinutes = minutes)) }

    // --- Chime settings ---

    fun setChimeEnabled(enabled: Boolean) = update { it.copy(chime = it.chime.copy(enabled = enabled)) }

    // --- Player settings ---

    fun setPlayerSize(size: Int) = update { it.copy(player = it.player.copy(size = size)) }
    fun setPlayerVisible(visible: Boolean) = update { it.copy(player = it.player.copy(visible = visible)) }
    fun setActivePreset(index: Int) = update { it.copy(player = it.player.copy(activePreset = index)) }

    // --- Preset CRUD ---

    fun addPreset(preset: Preset): Boolean {
        if (config.player.presets.size >= MAX_PRESETS) return false
        update { it.copy(player = it.player.copy(presets = it.player.presets + preset)) }
        return true
    }

    fun updatePreset(index: Int, preset: Preset) {
        val presets = config.player.presets.toMutableList()
        if (index < 0 || index >= presets.size) return
        presets[index] = preset
        update { it.copy(player = it.player.copy(presets = presets)) }
    }

    fun removePreset(index: Int) {
        val presets = config.player.presets.toMutableList()
        if (index < 0 || index >= presets.size) return
        presets.removeAt(index)
        val activePreset = config.player.activePreset
        val newActive = when {
            activePreset == index -> -1
            activePreset > index -> activePreset - 1
            else -> activePreset
        }
        update { it.copy(player = it.player.copy(presets = presets, activePreset = newActive)) }
    }

    fun reorderPreset(fromIndex: Int, toIndex: Int) {
        val presets = config.player.presets.toMutableList()
        if (fromIndex < 0 || fromIndex >= presets.size || toIndex < 0 || toIndex >= presets.size) return
        val item = presets.removeAt(fromIndex)
        presets.add(toIndex, item)
        val activePreset = config.player.activePreset
        val newActive = when (activePreset) {
            fromIndex -> toIndex
            in (minOf(fromIndex, toIndex)..maxOf(fromIndex, toIndex)) -> {
                if (fromIndex < toIndex) activePreset - 1 else activePreset + 1
            }
            else -> activePreset
        }
        update { it.copy(player = it.player.copy(presets = presets, activePreset = newActive)) }
    }

    // --- Serialization ---

    fun toJson(): JSONObject {
        val c = config
        return JSONObject().apply {
            put("version", c.version)
            put("clock", JSONObject().apply {
                put("theme", c.clock.theme)
                put("primaryTimezone", c.clock.primaryTimezone)
                put("secondaryTimezone", c.clock.secondaryTimezone)
                put("timeFormat", c.clock.timeFormat)
                put("nightDimEnabled", c.clock.nightDimEnabled)
                put("driftEnabled", c.clock.driftEnabled)
            })
            put("wallpaper", JSONObject().apply {
                put("enabled", c.wallpaper.enabled)
                put("intervalMinutes", c.wallpaper.intervalMinutes)
            })
            put("chime", JSONObject().apply {
                put("enabled", c.chime.enabled)
            })
            put("player", JSONObject().apply {
                put("size", c.player.size)
                put("visible", c.player.visible)
                put("activePreset", c.player.activePreset)
                put("presets", JSONArray().apply {
                    c.player.presets.forEach { p ->
                        put(JSONObject().apply {
                            put("name", p.name)
                            put("url", p.url)
                        })
                    }
                })
            })
        }
    }

    // --- Internal ---

    private fun update(transform: (MantleConfig) -> MantleConfig) {
        config = transform(config).copy(version = config.version + 1)
        save()
        notifyListeners()
    }

    private fun save() {
        prefs.edit().putString(KEY_CONFIG, toJson().toString()).apply()
    }

    private fun load(): MantleConfig {
        val json = prefs.getString(KEY_CONFIG, null) ?: return MantleConfig()
        return try {
            fromJson(JSONObject(json))
        } catch (e: Exception) {
            MantleConfig()
        }
    }

    private fun notifyListeners() {
        val cfg = config
        mainHandler.post {
            listeners.forEach { it.onConfigChanged(cfg) }
        }
    }

    private fun fromJson(json: JSONObject): MantleConfig {
        val clock = json.optJSONObject("clock")
        val wallpaper = json.optJSONObject("wallpaper")
        val chime = json.optJSONObject("chime")
        val player = json.optJSONObject("player")

        val presets = mutableListOf<Preset>()
        player?.optJSONArray("presets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                presets.add(Preset(p.optString("name", ""), p.optString("url", "")))
            }
        }

        return MantleConfig(
            version = json.optInt("version", 1),
            clock = ClockConfig(
                theme = clock?.optInt("theme", 0) ?: 0,
                primaryTimezone = clock?.optString("primaryTimezone", "America/New_York") ?: "America/New_York",
                secondaryTimezone = clock?.optString("secondaryTimezone", "Asia/Kolkata") ?: "Asia/Kolkata",
                timeFormat = clock?.optInt("timeFormat", 0) ?: 0,
                nightDimEnabled = clock?.optBoolean("nightDimEnabled", true) ?: true,
                driftEnabled = clock?.optBoolean("driftEnabled", true) ?: true
            ),
            wallpaper = WallpaperConfig(
                enabled = wallpaper?.optBoolean("enabled", true) ?: true,
                intervalMinutes = wallpaper?.optInt("intervalMinutes", 5) ?: 5
            ),
            chime = ChimeConfig(
                enabled = chime?.optBoolean("enabled", true) ?: true
            ),
            player = PlayerConfig(
                size = player?.optInt("size", 1) ?: 1,
                visible = player?.optBoolean("visible", true) ?: true,
                activePreset = player?.optInt("activePreset", -1) ?: -1,
                presets = presets
            )
        )
    }
}
