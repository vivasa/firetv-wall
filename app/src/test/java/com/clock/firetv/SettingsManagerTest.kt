package com.clock.firetv

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsManagerTest {

    private lateinit var settings: SettingsManager

    @Before
    fun setUp() {
        settings = SettingsManager(ApplicationProvider.getApplicationContext())
        // Clear prefs for clean state
        settings.prefs.edit().clear().commit()
    }

    // getPlayerDimensions — Classic theme (default)
    @Test
    fun `getPlayerDimensions classic small returns 240x135`() {
        settings.theme = SettingsManager.THEME_CLASSIC
        settings.playerSize = SettingsManager.PLAYER_SMALL
        assertThat(settings.getPlayerDimensions()).isEqualTo(240 to 135)
    }

    @Test
    fun `getPlayerDimensions classic medium returns 320x180`() {
        settings.theme = SettingsManager.THEME_CLASSIC
        settings.playerSize = SettingsManager.PLAYER_MEDIUM
        assertThat(settings.getPlayerDimensions()).isEqualTo(320 to 180)
    }

    @Test
    fun `getPlayerDimensions classic large returns 426x240`() {
        settings.theme = SettingsManager.THEME_CLASSIC
        settings.playerSize = SettingsManager.PLAYER_LARGE
        assertThat(settings.getPlayerDimensions()).isEqualTo(426 to 240)
    }

    // getPlayerDimensions — Gallery theme
    @Test
    fun `getPlayerDimensions gallery small returns 528x297`() {
        settings.theme = SettingsManager.THEME_GALLERY
        settings.playerSize = SettingsManager.PLAYER_SMALL
        assertThat(settings.getPlayerDimensions()).isEqualTo(528 to 297)
    }

    @Test
    fun `getPlayerDimensions gallery medium returns 640x360`() {
        settings.theme = SettingsManager.THEME_GALLERY
        settings.playerSize = SettingsManager.PLAYER_MEDIUM
        assertThat(settings.getPlayerDimensions()).isEqualTo(640 to 360)
    }

    @Test
    fun `getPlayerDimensions gallery large returns 744x418`() {
        settings.theme = SettingsManager.THEME_GALLERY
        settings.playerSize = SettingsManager.PLAYER_LARGE
        assertThat(settings.getPlayerDimensions()).isEqualTo(744 to 418)
    }

    // getPlayerDimensions — Retro theme
    @Test
    fun `getPlayerDimensions retro small returns 384x216`() {
        settings.theme = SettingsManager.THEME_RETRO
        settings.playerSize = SettingsManager.PLAYER_SMALL
        assertThat(settings.getPlayerDimensions()).isEqualTo(384 to 216)
    }

    @Test
    fun `getPlayerDimensions retro medium returns 480x270`() {
        settings.theme = SettingsManager.THEME_RETRO
        settings.playerSize = SettingsManager.PLAYER_MEDIUM
        assertThat(settings.getPlayerDimensions()).isEqualTo(480 to 270)
    }

    @Test
    fun `getPlayerDimensions retro large returns 576x324`() {
        settings.theme = SettingsManager.THEME_RETRO
        settings.playerSize = SettingsManager.PLAYER_LARGE
        assertThat(settings.getPlayerDimensions()).isEqualTo(576 to 324)
    }

    // activeYoutubeUrl
    @Test
    fun `activeYoutubeUrl returns empty when activePreset is -1`() {
        settings.activePreset = -1
        assertThat(settings.activeYoutubeUrl).isEmpty()
    }

    @Test
    fun `activeYoutubeUrl returns empty when activePreset is out of range`() {
        settings.activePreset = 4
        assertThat(settings.activeYoutubeUrl).isEmpty()
    }

    @Test
    fun `activeYoutubeUrl returns URL for valid preset index`() {
        settings.presetCount = 3
        settings.setPresetUrl(2, "https://youtube.com/watch?v=test123")
        settings.activePreset = 2
        assertThat(settings.activeYoutubeUrl).isEqualTo("https://youtube.com/watch?v=test123")
    }

}
