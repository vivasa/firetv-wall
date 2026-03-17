package com.firetv.protocol

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class ProtocolMessageTest {

    // -- Ping command round-trip --

    @Test
    fun `ping command round-trip`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.PING)
        }
        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PING)
    }

    // -- Play command with presetIndex --

    @Test
    fun `play command with presetIndex round-trip`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.PLAY)
            put(ProtocolKeys.PRESET_INDEX, 2)
        }
        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.PLAY)
        assertThat(parsed.getInt(ProtocolKeys.PRESET_INDEX)).isEqualTo(2)
    }

    // -- Auth command with token --

    @Test
    fun `auth command with token round-trip`() {
        val token = "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4"
        val json = JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.AUTH)
            put(ProtocolKeys.TOKEN, token)
        }
        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.AUTH)
        assertThat(parsed.getString(ProtocolKeys.TOKEN)).isEqualTo(token)
    }

    // -- Sync config with nested object --

    @Test
    fun `sync_config command with nested config round-trip`() {
        val config = JSONObject().apply {
            put(ProtocolKeys.THEME, "dark")
            put(ProtocolKeys.CHIME_ENABLED, true)
            put(ProtocolKeys.WALLPAPER_INTERVAL, 30)
        }
        val json = JSONObject().apply {
            put(ProtocolKeys.CMD, ProtocolCommands.SYNC_CONFIG)
            put(ProtocolKeys.CONFIG, config)
        }
        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.CMD)).isEqualTo(ProtocolCommands.SYNC_CONFIG)

        val parsedConfig = parsed.getJSONObject(ProtocolKeys.CONFIG)
        assertThat(parsedConfig.getString(ProtocolKeys.THEME)).isEqualTo("dark")
        assertThat(parsedConfig.getBoolean(ProtocolKeys.CHIME_ENABLED)).isTrue()
        assertThat(parsedConfig.getInt(ProtocolKeys.WALLPAPER_INTERVAL)).isEqualTo(30)
    }

    // -- State event with full state dump --

    @Test
    fun `state event with full state dump round-trip`() {
        val preset = JSONObject().apply {
            put(ProtocolKeys.INDEX, 0)
            put(ProtocolKeys.URL, "https://example.com/stream")
            put(ProtocolKeys.NAME, "Lo-Fi Radio")
        }
        val state = JSONObject().apply {
            put(ProtocolKeys.DEVICE_ID, "abc123")
            put(ProtocolKeys.DEVICE_NAME, "Living Room TV")
            put(ProtocolKeys.THEME, "retro")
            put(ProtocolKeys.PRIMARY_TIMEZONE, "America/New_York")
            put(ProtocolKeys.SECONDARY_TIMEZONE, "Europe/London")
            put(ProtocolKeys.TIME_FORMAT, "12h")
            put(ProtocolKeys.CHIME_ENABLED, false)
            put(ProtocolKeys.WALLPAPER_ENABLED, true)
            put(ProtocolKeys.WALLPAPER_INTERVAL, 15)
            put(ProtocolKeys.DRIFT_ENABLED, true)
            put(ProtocolKeys.NIGHT_DIM_ENABLED, false)
            put(ProtocolKeys.ACTIVE_PRESET, 0)
            put(ProtocolKeys.PLAYER_SIZE, "small")
            put(ProtocolKeys.PLAYER_VISIBLE, true)
            put(ProtocolKeys.PRESETS, JSONArray().apply { put(preset) })
        }
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.STATE)
            put(ProtocolKeys.DATA, state)
        }

        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.EVT)).isEqualTo(ProtocolEvents.STATE)

        val parsedState = parsed.getJSONObject(ProtocolKeys.DATA)
        assertThat(parsedState.getString(ProtocolKeys.DEVICE_ID)).isEqualTo("abc123")
        assertThat(parsedState.getString(ProtocolKeys.DEVICE_NAME)).isEqualTo("Living Room TV")
        assertThat(parsedState.getString(ProtocolKeys.THEME)).isEqualTo("retro")
        assertThat(parsedState.getString(ProtocolKeys.PRIMARY_TIMEZONE)).isEqualTo("America/New_York")
        assertThat(parsedState.getString(ProtocolKeys.SECONDARY_TIMEZONE)).isEqualTo("Europe/London")
        assertThat(parsedState.getString(ProtocolKeys.TIME_FORMAT)).isEqualTo("12h")
        assertThat(parsedState.getBoolean(ProtocolKeys.CHIME_ENABLED)).isFalse()
        assertThat(parsedState.getBoolean(ProtocolKeys.WALLPAPER_ENABLED)).isTrue()
        assertThat(parsedState.getInt(ProtocolKeys.WALLPAPER_INTERVAL)).isEqualTo(15)
        assertThat(parsedState.getBoolean(ProtocolKeys.DRIFT_ENABLED)).isTrue()
        assertThat(parsedState.getBoolean(ProtocolKeys.NIGHT_DIM_ENABLED)).isFalse()
        assertThat(parsedState.getInt(ProtocolKeys.ACTIVE_PRESET)).isEqualTo(0)
        assertThat(parsedState.getString(ProtocolKeys.PLAYER_SIZE)).isEqualTo("small")
        assertThat(parsedState.getBoolean(ProtocolKeys.PLAYER_VISIBLE)).isTrue()

        val presets = parsedState.getJSONArray(ProtocolKeys.PRESETS)
        assertThat(presets.length()).isEqualTo(1)
        assertThat(presets.getJSONObject(0).getString(ProtocolKeys.NAME)).isEqualTo("Lo-Fi Radio")
    }

    // -- Track changed event --

    @Test
    fun `track_changed event round-trip`() {
        val json = JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.TRACK_CHANGED)
            put(ProtocolKeys.TITLE, "Midnight City")
            put(ProtocolKeys.PLAYLIST, "Synthwave Mix")
            put(ProtocolKeys.IS_PLAYING, true)
        }
        val parsed = JSONObject(json.toString())
        assertThat(parsed.getString(ProtocolKeys.EVT)).isEqualTo(ProtocolEvents.TRACK_CHANGED)
        assertThat(parsed.getString(ProtocolKeys.TITLE)).isEqualTo("Midnight City")
        assertThat(parsed.getString(ProtocolKeys.PLAYLIST)).isEqualTo("Synthwave Mix")
        assertThat(parsed.getBoolean(ProtocolKeys.IS_PLAYING)).isTrue()
    }

    // -- Constant uniqueness: commands --

    @Test
    fun `no duplicate command strings in ProtocolCommands`() {
        val values = getStringConstants(ProtocolCommands::class.java)
        assertThat(values.toSet().size).isEqualTo(values.size)
    }

    // -- Constant uniqueness: events --

    @Test
    fun `no duplicate event strings in ProtocolEvents`() {
        val values = getStringConstants(ProtocolEvents::class.java)
        assertThat(values.toSet().size).isEqualTo(values.size)
    }

    // -- Commands and events disjoint --

    @Test
    fun `command and event string sets are disjoint`() {
        val commands = getStringConstants(ProtocolCommands::class.java).toSet()
        val events = getStringConstants(ProtocolEvents::class.java).toSet()
        val overlap = commands.intersect(events)
        assertThat(overlap).isEmpty()
    }

    // -- Helper --

    private fun getStringConstants(clazz: Class<*>): List<String> {
        return clazz.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.isAccessible = true; it.get(null) as String }
    }
}
