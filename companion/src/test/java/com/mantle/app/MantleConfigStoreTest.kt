package com.mantle.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class MantleConfigStoreTest {

    private lateinit var context: Context
    private lateinit var store: MantleConfigStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("mantle_config", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = MantleConfigStore(context)
    }

    // --- Default initialization ---

    @Test
    fun `fresh store has default config values`() {
        val c = store.config
        assertThat(c.version).isEqualTo(1)
        assertThat(c.clock.theme).isEqualTo(0)
        assertThat(c.clock.primaryTimezone).isEqualTo("America/New_York")
        assertThat(c.clock.secondaryTimezone).isEqualTo("Asia/Kolkata")
        assertThat(c.clock.timeFormat).isEqualTo(0)
        assertThat(c.clock.nightDimEnabled).isTrue()
        assertThat(c.clock.driftEnabled).isTrue()
        assertThat(c.wallpaper.enabled).isTrue()
        assertThat(c.wallpaper.intervalMinutes).isEqualTo(5)
        assertThat(c.chime.enabled).isTrue()
        assertThat(c.player.size).isEqualTo(1)
        assertThat(c.player.visible).isTrue()
        assertThat(c.player.activePreset).isEqualTo(-1)
        assertThat(c.player.presets).isEmpty()
    }

    // --- Version incrementing ---

    @Test
    fun `single mutation increments version from 1 to 2`() {
        store.setTheme(1)
        assertThat(store.config.version).isEqualTo(2)
    }

    @Test
    fun `multiple mutations increment version sequentially`() {
        store.setTheme(1)
        store.setChimeEnabled(false)
        assertThat(store.config.version).isEqualTo(3)
    }

    // --- Persistence round-trip ---

    @Test
    fun `persist and reload preserves config`() {
        store.setTheme(2)
        store.setPrimaryTimezone("Europe/London")
        store.setWallpaperInterval(10)
        store.addPreset(Preset("Test", "http://example.com"))

        val reloaded = MantleConfigStore(context)
        assertThat(reloaded.config.clock.theme).isEqualTo(2)
        assertThat(reloaded.config.clock.primaryTimezone).isEqualTo("Europe/London")
        assertThat(reloaded.config.wallpaper.intervalMinutes).isEqualTo(10)
        assertThat(reloaded.config.player.presets).hasSize(1)
        assertThat(reloaded.config.player.presets[0].name).isEqualTo("Test")
    }

    @Test
    fun `corrupt JSON fallback returns defaults`() {
        context.getSharedPreferences("mantle_config", Context.MODE_PRIVATE)
            .edit().putString("config", "not valid json!!!").commit()
        val freshStore = MantleConfigStore(context)
        assertThat(freshStore.config.version).isEqualTo(1)
        assertThat(freshStore.config.clock.theme).isEqualTo(0)
    }

    // --- Clock settings ---

    @Test
    fun `setTheme updates only theme`() {
        store.setTheme(2)
        assertThat(store.config.clock.theme).isEqualTo(2)
        assertThat(store.config.clock.primaryTimezone).isEqualTo("America/New_York")
        assertThat(store.config.clock.driftEnabled).isTrue()
    }

    @Test
    fun `setPrimaryTimezone updates only primaryTimezone`() {
        store.setPrimaryTimezone("Europe/London")
        assertThat(store.config.clock.primaryTimezone).isEqualTo("Europe/London")
        assertThat(store.config.clock.theme).isEqualTo(0)
    }

    @Test
    fun `setSecondaryTimezone updates only secondaryTimezone`() {
        store.setSecondaryTimezone("Asia/Tokyo")
        assertThat(store.config.clock.secondaryTimezone).isEqualTo("Asia/Tokyo")
        assertThat(store.config.clock.primaryTimezone).isEqualTo("America/New_York")
    }

    @Test
    fun `setTimeFormat updates only timeFormat`() {
        store.setTimeFormat(1)
        assertThat(store.config.clock.timeFormat).isEqualTo(1)
        assertThat(store.config.clock.theme).isEqualTo(0)
    }

    @Test
    fun `setNightDimEnabled updates only nightDimEnabled`() {
        store.setNightDimEnabled(false)
        assertThat(store.config.clock.nightDimEnabled).isFalse()
        assertThat(store.config.clock.driftEnabled).isTrue()
    }

    @Test
    fun `setDriftEnabled updates only driftEnabled`() {
        store.setDriftEnabled(false)
        assertThat(store.config.clock.driftEnabled).isFalse()
        assertThat(store.config.clock.nightDimEnabled).isTrue()
    }

    // --- Preset CRUD ---

    @Test
    fun `addPreset appends to preset list`() {
        store.addPreset(Preset("Test", "http://example.com"))
        assertThat(store.config.player.presets).hasSize(1)
        assertThat(store.config.player.presets[0].name).isEqualTo("Test")
        assertThat(store.config.player.presets[0].url).isEqualTo("http://example.com")
    }

    @Test
    fun `addPreset at max capacity returns false`() {
        repeat(20) { store.addPreset(Preset("P$it", "http://example.com/$it")) }
        val result = store.addPreset(Preset("Extra", "http://extra.com"))
        assertThat(result).isFalse()
        assertThat(store.config.player.presets).hasSize(20)
    }

    @Test
    fun `updatePreset changes name and url`() {
        store.addPreset(Preset("Old", "http://old.com"))
        store.updatePreset(0, Preset("Updated", "http://new.com"))
        assertThat(store.config.player.presets[0].name).isEqualTo("Updated")
        assertThat(store.config.player.presets[0].url).isEqualTo("http://new.com")
    }

    @Test
    fun `updatePreset out of bounds is no-op`() {
        store.addPreset(Preset("Test", "http://test.com"))
        store.updatePreset(-1, Preset("Bad", "http://bad.com"))
        store.updatePreset(100, Preset("Bad", "http://bad.com"))
        assertThat(store.config.player.presets[0].name).isEqualTo("Test")
    }

    // --- Remove preset active index adjustment ---

    @Test
    fun `remove preset before active adjusts active index down`() {
        repeat(3) { store.addPreset(Preset("P$it", "http://p$it.com")) }
        store.setActivePreset(2)
        store.removePreset(1)
        assertThat(store.config.player.activePreset).isEqualTo(1)
    }

    @Test
    fun `remove active preset resets active to -1`() {
        repeat(3) { store.addPreset(Preset("P$it", "http://p$it.com")) }
        store.setActivePreset(2)
        store.removePreset(2)
        assertThat(store.config.player.activePreset).isEqualTo(-1)
    }

    // --- Reorder preset ---

    @Test
    fun `reorder preset updates active index to follow moved item`() {
        repeat(3) { store.addPreset(Preset("P$it", "http://p$it.com")) }
        store.setActivePreset(0)
        store.reorderPreset(0, 2)
        assertThat(store.config.player.activePreset).isEqualTo(2)
        assertThat(store.config.player.presets[2].name).isEqualTo("P0")
    }

    // --- JSON serialization ---

    @Test
    fun `toJson produces complete JSON with all fields`() {
        store.setTheme(1)
        store.addPreset(Preset("Music", "http://music.com"))
        val json = store.toJson()

        assertThat(json.getInt("version")).isGreaterThan(1)
        val clock = json.getJSONObject("clock")
        assertThat(clock.getInt("theme")).isEqualTo(1)
        assertThat(clock.getString("primaryTimezone")).isEqualTo("America/New_York")
        assertThat(clock.getString("secondaryTimezone")).isEqualTo("Asia/Kolkata")
        assertThat(clock.getInt("timeFormat")).isEqualTo(0)
        assertThat(clock.getBoolean("nightDimEnabled")).isTrue()
        assertThat(clock.getBoolean("driftEnabled")).isTrue()

        val wallpaper = json.getJSONObject("wallpaper")
        assertThat(wallpaper.getBoolean("enabled")).isTrue()
        assertThat(wallpaper.getInt("intervalMinutes")).isEqualTo(5)

        val chime = json.getJSONObject("chime")
        assertThat(chime.getBoolean("enabled")).isTrue()

        val player = json.getJSONObject("player")
        assertThat(player.getInt("size")).isEqualTo(1)
        assertThat(player.getBoolean("visible")).isTrue()
        val presets = player.getJSONArray("presets")
        assertThat(presets.length()).isEqualTo(1)
        assertThat(presets.getJSONObject(0).getString("name")).isEqualTo("Music")
        assertThat(presets.getJSONObject(0).getString("url")).isEqualTo("http://music.com")
    }

    // --- Listener notification ---

    @Test
    fun `listener receives onConfigChanged after mutation`() {
        var receivedConfig: MantleConfig? = null
        store.addListener(object : MantleConfigStore.OnConfigChangedListener {
            override fun onConfigChanged(config: MantleConfig) {
                receivedConfig = config
            }
        })
        store.setTheme(1)
        ShadowLooper.idleMainLooper()
        assertThat(receivedConfig).isNotNull()
        assertThat(receivedConfig!!.clock.theme).isEqualTo(1)
    }

    @Test
    fun `removed listener is not notified`() {
        var callCount = 0
        val listener = object : MantleConfigStore.OnConfigChangedListener {
            override fun onConfigChanged(config: MantleConfig) {
                callCount++
            }
        }
        store.addListener(listener)
        store.removeListener(listener)
        store.setTheme(1)
        ShadowLooper.idleMainLooper()
        assertThat(callCount).isEqualTo(0)
    }
}
