package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvConnectionManagerParseTest {

    private lateinit var manager: TvConnectionManager

    @Before
    fun setUp() {
        manager = TvConnectionManager()
    }

    @Test
    fun `parseState with full JSON populates all fields`() {
        val data = JSONObject().apply {
            put("deviceId", "dev-123")
            put("deviceName", "Living Room")
            put("theme", 2)
            put("primaryTimezone", "America/New_York")
            put("secondaryTimezone", "Asia/Kolkata")
            put("timeFormat", 1)
            put("chimeEnabled", false)
            put("wallpaperEnabled", false)
            put("wallpaperInterval", 10)
            put("driftEnabled", false)
            put("nightDimEnabled", false)
            put("activePreset", 2)
            put("playerSize", 0)
            put("playerVisible", false)
            put("presets", JSONArray().apply {
                put(JSONObject().apply {
                    put("index", 0)
                    put("url", "https://youtube.com/1")
                    put("name", "Preset 1")
                })
            })
        }

        val state = manager.parseState(data)

        assertThat(state.deviceId).isEqualTo("dev-123")
        assertThat(state.deviceName).isEqualTo("Living Room")
        assertThat(state.theme).isEqualTo(2)
        assertThat(state.primaryTimezone).isEqualTo("America/New_York")
        assertThat(state.secondaryTimezone).isEqualTo("Asia/Kolkata")
        assertThat(state.timeFormat).isEqualTo(1)
        assertThat(state.chimeEnabled).isFalse()
        assertThat(state.wallpaperEnabled).isFalse()
        assertThat(state.wallpaperInterval).isEqualTo(10)
        assertThat(state.driftEnabled).isFalse()
        assertThat(state.nightDimEnabled).isFalse()
        assertThat(state.activePreset).isEqualTo(2)
        assertThat(state.playerSize).isEqualTo(0)
        assertThat(state.playerVisible).isFalse()
        assertThat(state.presets).hasSize(1)
        assertThat(state.presets[0].url).isEqualTo("https://youtube.com/1")
    }

    @Test
    fun `parseState with missing fields applies defaults`() {
        val data = JSONObject() // empty

        val state = manager.parseState(data)

        assertThat(state.theme).isEqualTo(0)
        assertThat(state.chimeEnabled).isTrue()
        assertThat(state.wallpaperEnabled).isTrue()
        assertThat(state.wallpaperInterval).isEqualTo(5)
        assertThat(state.driftEnabled).isTrue()
        assertThat(state.nightDimEnabled).isTrue()
        assertThat(state.activePreset).isEqualTo(-1)
        assertThat(state.playerSize).isEqualTo(1)
        assertThat(state.playerVisible).isTrue()
        assertThat(state.presets).isEmpty()
    }

    @Test
    fun `parseState with preset array populates presets in order`() {
        val data = JSONObject().apply {
            put("presets", JSONArray().apply {
                put(JSONObject().apply { put("index", 0); put("url", "url0"); put("name", "A") })
                put(JSONObject().apply { put("index", 1); put("url", "url1"); put("name", "B") })
                put(JSONObject().apply { put("index", 2); put("url", "url2"); put("name", "C") })
            })
        }

        val state = manager.parseState(data)

        assertThat(state.presets).hasSize(3)
        assertThat(state.presets[0]).isEqualTo(TvConnectionManager.Preset(0, "url0", "A"))
        assertThat(state.presets[1]).isEqualTo(TvConnectionManager.Preset(1, "url1", "B"))
        assertThat(state.presets[2]).isEqualTo(TvConnectionManager.Preset(2, "url2", "C"))
    }

    @Test
    fun `parseState with null presets array results in empty list`() {
        val data = JSONObject().apply { put("theme", 1) } // no presets key

        val state = manager.parseState(data)

        assertThat(state.presets).isEmpty()
    }

    @Test
    fun `parseState preserves empty string defaults for string fields`() {
        val data = JSONObject()

        val state = manager.parseState(data)

        assertThat(state.deviceId).isEmpty()
        assertThat(state.deviceName).isEmpty()
        assertThat(state.primaryTimezone).isEmpty()
        assertThat(state.secondaryTimezone).isEmpty()
    }
}
