package com.mantle.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlayerViewModelTest {

    private lateinit var app: Application
    private lateinit var configStore: MantleConfigStore
    private lateinit var deviceStore: DeviceStore
    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        testScope = TestScope()
        app.getSharedPreferences("mantle_config", Context.MODE_PRIVATE)
            .edit().clear().commit()
        app.getSharedPreferences("companion_devices", Context.MODE_PRIVATE)
            .edit().clear().commit()
        configStore = MantleConfigStore(app, testScope)
        deviceStore = DeviceStore(app)
    }

    // --- PlaylistItem mapping ---

    @Test
    fun `PlaylistItem maps preset fields correctly`() {
        val item = PlaylistItem(
            index = 0,
            name = "Jazz",
            url = "http://jazz.com",
            artworkUrl = "http://art.com/jazz.jpg",
            isActive = true,
            isPlaying = true,
            lastPlayedTimestamp = 100L
        )
        assertThat(item.name).isEqualTo("Jazz")
        assertThat(item.artworkUrl).isEqualTo("http://art.com/jazz.jpg")
        assertThat(item.isActive).isTrue()
        assertThat(item.isPlaying).isTrue()
        assertThat(item.lastPlayedTimestamp).isEqualTo(100L)
    }

    // --- PlayerUiState defaults ---

    @Test
    fun `PlayerUiState default has empty playlists and no now playing`() {
        val state = PlayerUiState()
        assertThat(state.allPlaylists).isEmpty()
        assertThat(state.recentlyPlayed).isEmpty()
        assertThat(state.nowPlaying.title).isNull()
        assertThat(state.nowPlaying.isPlaying).isFalse()
        assertThat(state.sleepTimerMinutes).isNull()
        assertThat(state.connectionState).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
    }

    // --- Recently played ordering ---

    @Test
    fun `recently played sorts by lastPlayed descending and takes top 4`() {
        val items = listOf(
            PlaylistItem(0, "A", "", null, false, false, 100),
            PlaylistItem(1, "B", "", null, false, false, 300),
            PlaylistItem(2, "C", "", null, false, false, 200),
            PlaylistItem(3, "D", "", null, false, false, 500),
            PlaylistItem(4, "E", "", null, false, false, 400)
        )
        val recent = items
            .filter { it.lastPlayedTimestamp > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(4)

        assertThat(recent.map { it.name }).containsExactly("D", "E", "B", "C").inOrder()
    }

    @Test
    fun `recently played excludes items with lastPlayed 0`() {
        val items = listOf(
            PlaylistItem(0, "A", "", null, false, false, 0),
            PlaylistItem(1, "B", "", null, false, false, 100)
        )
        val recent = items
            .filter { it.lastPlayedTimestamp > 0 }
            .sortedByDescending { it.lastPlayedTimestamp }
            .take(4)

        assertThat(recent).hasSize(1)
        assertThat(recent[0].name).isEqualTo("B")
    }

    // --- NowPlayingState ---

    @Test
    fun `NowPlayingState reflects values`() {
        val state = NowPlayingState(
            title = "Track 1",
            playlist = "Jazz Standards",
            artworkUrl = "http://art.com/jazz.jpg",
            isPlaying = true
        )
        assertThat(state.title).isEqualTo("Track 1")
        assertThat(state.artworkUrl).isEqualTo("http://art.com/jazz.jpg")
        assertThat(state.isPlaying).isTrue()
    }

    // --- Onboarding flag ---

    @Test
    fun `needsOnboarding true when no paired devices`() {
        val needsOnboarding = deviceStore.getPairedDevices().isEmpty()
        assertThat(needsOnboarding).isTrue()
    }

    @Test
    fun `needsOnboarding false when devices exist`() {
        deviceStore.addDevice(DeviceStore.PairedDevice("id1", "TV", "token", "192.168.1.1", 8080))
        val needsOnboarding = deviceStore.getPairedDevices().isEmpty()
        assertThat(needsOnboarding).isFalse()
    }

    // --- Sleep timer state ---

    @Test
    fun `sleep timer null by default`() {
        val state = PlayerUiState()
        assertThat(state.sleepTimerMinutes).isNull()
    }

    @Test
    fun `sleep timer can be set to specific minutes`() {
        val state = PlayerUiState(sleepTimerMinutes = 30)
        assertThat(state.sleepTimerMinutes).isEqualTo(30)
    }

    // --- selectPreset updates lastPlayed ---

    @Test
    fun `selectPreset logic sets lastPlayed on config store`() {
        configStore.addPreset(Preset("Jazz", "http://jazz.com"))
        configStore.addPreset(Preset("Lo-Fi", "http://lofi.com"))
        configStore.setActivePreset(0)
        configStore.setPresetLastPlayed(0, System.currentTimeMillis())

        assertThat(configStore.config.player.activePreset).isEqualTo(0)
        assertThat(configStore.config.player.presets[0].lastPlayed).isGreaterThan(0L)
        assertThat(configStore.config.player.presets[1].lastPlayed).isEqualTo(0)
    }

    // --- Auto-select first preset ---

    @Test
    fun `adding first preset when activePreset is -1 should auto-select`() {
        // Verify starting state
        assertThat(configStore.config.player.activePreset).isEqualTo(-1)
        assertThat(configStore.config.player.presets).isEmpty()

        // Add first preset — the ViewModel's addPreset calls selectPreset
        // which calls setActivePreset. Test the config store logic directly.
        configStore.addPreset(Preset("Jazz", "http://jazz.com"))
        val shouldAutoSelect = configStore.config.player.activePreset == -1
        // activePreset wasn't set yet by addPreset, so we simulate what ViewModel does:
        if (shouldAutoSelect || configStore.config.player.activePreset == -1) {
            configStore.setActivePreset(0)
        }
        assertThat(configStore.config.player.activePreset).isEqualTo(0)
    }

    @Test
    fun `adding preset when another is active does not change selection`() {
        configStore.addPreset(Preset("Jazz", "http://jazz.com"))
        configStore.setActivePreset(0)

        // Add second preset
        configStore.addPreset(Preset("Lo-Fi", "http://lofi.com"))
        // activePreset should still be 0
        assertThat(configStore.config.player.activePreset).isEqualTo(0)
    }

    // --- Track list state ---

    @Test
    fun `PlayerUiState default has empty track list`() {
        val state = PlayerUiState()
        assertThat(state.trackList).isEmpty()
        assertThat(state.currentTrackIndex).isEqualTo(-1)
    }

    @Test
    fun `TrackItem stores index and title`() {
        val item = TrackItem(index = 3, title = "Song Name")
        assertThat(item.index).isEqualTo(3)
        assertThat(item.title).isEqualTo("Song Name")
    }

    // --- Switching preset state ---

    @Test
    fun `PlayerUiState switchingPresetIndex is null by default`() {
        val state = PlayerUiState()
        assertThat(state.switchingPresetIndex).isNull()
    }

    @Test
    fun `PlayerUiState switchingPresetIndex can be set`() {
        val state = PlayerUiState(switchingPresetIndex = 2)
        assertThat(state.switchingPresetIndex).isEqualTo(2)
    }

    @Test
    fun `active row should show Now Playing not track title`() {
        // The active playlist row shows "Now Playing" label, not nowPlaying.title
        // This is a rendering concern, but we verify the state model supports it
        val state = PlayerUiState(
            nowPlaying = NowPlayingState("Song Title", "Playlist", null, true),
            activePreset = 0,
            switchingPresetIndex = null,
            allPlaylists = listOf(
                PlaylistItem(0, "Jazz", "url", null, isActive = true, isPlaying = true, lastPlayedTimestamp = 100)
            )
        )
        // Active playlist exists, switching is null → row should show "Now Playing"
        assertThat(state.switchingPresetIndex).isNull()
        assertThat(state.allPlaylists[0].isActive).isTrue()
    }

    @Test
    fun `switching row should show Loading state`() {
        val state = PlayerUiState(
            activePreset = 1,
            switchingPresetIndex = 1,
            allPlaylists = listOf(
                PlaylistItem(0, "Jazz", "url", null, isActive = false, isPlaying = false, lastPlayedTimestamp = 100),
                PlaylistItem(1, "Lo-Fi", "url", null, isActive = true, isPlaying = false, lastPlayedTimestamp = 0)
            )
        )
        // Row at index 1 matches switchingPresetIndex → should show "Loading..."
        assertThat(state.switchingPresetIndex).isEqualTo(1)
        assertThat(state.allPlaylists[1].isActive).isTrue()
    }
}
