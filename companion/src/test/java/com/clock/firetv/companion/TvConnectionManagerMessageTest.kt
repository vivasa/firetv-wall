package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import com.mantle.app.TvConnectionManager

@RunWith(RobolectricTestRunner::class)
class TvConnectionManagerMessageTest {

    private lateinit var manager: TvConnectionManager
    private var lastConnectionState: TvConnectionManager.ConnectionState? = null
    private var lastTrackTitle: String? = null
    private var lastTrackPlaylist: String? = null
    private var lastPlayingState: Boolean? = null
    private var lastConfigVersion: Int? = null

    @Before
    fun setUp() {
        manager = TvConnectionManager()
        manager.addListener(object : TvConnectionManager.EventListener {
            override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
                lastConnectionState = state
            }
            override fun onTrackChanged(title: String, playlist: String) {
                lastTrackTitle = title
                lastTrackPlaylist = playlist
            }
            override fun onPlaybackStateChanged(playing: Boolean) {
                lastPlayingState = playing
            }
            override fun onConfigApplied(version: Int) {
                lastConfigVersion = version
            }
        })
    }

    @Test
    fun `handleMessage auth_ok transitions to CONNECTED`() {
        manager.handleMessage("""{"evt":"auth_ok"}""")
        ShadowLooper.idleMainLooper()
        assertThat(lastConnectionState).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)
    }

    @Test
    fun `handleMessage paired sets token and transitions to CONNECTED`() {
        manager.handleMessage("""{"evt":"paired","token":"abc123","deviceId":"dev1","deviceName":"TV"}""")
        ShadowLooper.idleMainLooper()
        assertThat(manager.lastPairedToken).isEqualTo("abc123")
        assertThat(manager.tvState.deviceId).isEqualTo("dev1")
        assertThat(manager.tvState.deviceName).isEqualTo("TV")
        assertThat(lastConnectionState).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)
    }

    @Test
    fun `handleMessage track_changed updates now playing`() {
        manager.handleMessage("""{"evt":"track_changed","title":"My Song","playlist":"Best Hits"}""")
        ShadowLooper.idleMainLooper()

        assertThat(manager.tvState.nowPlayingTitle).isEqualTo("My Song")
        assertThat(manager.tvState.nowPlayingPlaylist).isEqualTo("Best Hits")
        assertThat(lastTrackTitle).isEqualTo("My Song")
        assertThat(lastTrackPlaylist).isEqualTo("Best Hits")
    }

    @Test
    fun `handleMessage playback_state updates isPlaying`() {
        manager.handleMessage("""{"evt":"playback_state","isPlaying":true}""")
        ShadowLooper.idleMainLooper()

        assertThat(manager.tvState.isPlaying).isTrue()
        assertThat(lastPlayingState).isTrue()
    }

    @Test
    fun `handleMessage with invalid JSON does not crash`() {
        // Should log error but not throw
        manager.handleMessage("not json at all")
        ShadowLooper.idleMainLooper()
        // No assertion needed — just verify no exception
    }
}
