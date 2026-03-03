package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvConnectionManagerSettingTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var baseState: TvConnectionManager.TvState

    @Before
    fun setUp() {
        manager = TvConnectionManager()
        baseState = TvConnectionManager.TvState(
            theme = 0,
            primaryTimezone = "UTC",
            secondaryTimezone = "UTC",
            timeFormat = 0,
            chimeEnabled = true,
            wallpaperEnabled = true,
            wallpaperInterval = 5,
            driftEnabled = true,
            nightDimEnabled = true,
            activePreset = -1,
            playerSize = 1,
            playerVisible = true
        )
    }

    @Test
    fun `applySettingToState theme updates only theme`() {
        val result = manager.applySettingToState(baseState, "theme", 2)
        assertThat(result.theme).isEqualTo(2)
        assertThat(result.chimeEnabled).isTrue() // unchanged
    }

    @Test
    fun `applySettingToState primaryTimezone updates only primaryTimezone`() {
        val result = manager.applySettingToState(baseState, "primaryTimezone", "America/New_York")
        assertThat(result.primaryTimezone).isEqualTo("America/New_York")
        assertThat(result.secondaryTimezone).isEqualTo("UTC") // unchanged
    }

    @Test
    fun `applySettingToState secondaryTimezone updates only secondaryTimezone`() {
        val result = manager.applySettingToState(baseState, "secondaryTimezone", "Asia/Kolkata")
        assertThat(result.secondaryTimezone).isEqualTo("Asia/Kolkata")
    }

    @Test
    fun `applySettingToState timeFormat updates only timeFormat`() {
        val result = manager.applySettingToState(baseState, "timeFormat", 1)
        assertThat(result.timeFormat).isEqualTo(1)
    }

    @Test
    fun `applySettingToState chimeEnabled updates only chimeEnabled`() {
        val result = manager.applySettingToState(baseState, "chimeEnabled", false)
        assertThat(result.chimeEnabled).isFalse()
        assertThat(result.wallpaperEnabled).isTrue() // unchanged
    }

    @Test
    fun `applySettingToState wallpaperEnabled updates only wallpaperEnabled`() {
        val result = manager.applySettingToState(baseState, "wallpaperEnabled", false)
        assertThat(result.wallpaperEnabled).isFalse()
    }

    @Test
    fun `applySettingToState wallpaperInterval updates only wallpaperInterval`() {
        val result = manager.applySettingToState(baseState, "wallpaperInterval", 15)
        assertThat(result.wallpaperInterval).isEqualTo(15)
    }

    @Test
    fun `applySettingToState driftEnabled updates only driftEnabled`() {
        val result = manager.applySettingToState(baseState, "driftEnabled", false)
        assertThat(result.driftEnabled).isFalse()
    }

    @Test
    fun `applySettingToState nightDimEnabled updates only nightDimEnabled`() {
        val result = manager.applySettingToState(baseState, "nightDimEnabled", false)
        assertThat(result.nightDimEnabled).isFalse()
    }

    @Test
    fun `applySettingToState activePreset updates only activePreset`() {
        val result = manager.applySettingToState(baseState, "activePreset", 3)
        assertThat(result.activePreset).isEqualTo(3)
    }

    @Test
    fun `applySettingToState playerSize updates only playerSize`() {
        val result = manager.applySettingToState(baseState, "playerSize", 2)
        assertThat(result.playerSize).isEqualTo(2)
    }

    @Test
    fun `applySettingToState playerVisible updates only playerVisible`() {
        val result = manager.applySettingToState(baseState, "playerVisible", false)
        assertThat(result.playerVisible).isFalse()
    }

    @Test
    fun `applySettingToState unknown key returns unmodified state`() {
        val result = manager.applySettingToState(baseState, "unknown", "value")
        assertThat(result).isEqualTo(baseState)
    }
}
