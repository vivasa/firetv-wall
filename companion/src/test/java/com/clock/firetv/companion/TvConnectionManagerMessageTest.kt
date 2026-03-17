package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.mantle.app.TvConnectionManager

@RunWith(RobolectricTestRunner::class)
class TvConnectionManagerMessageTest {

    private lateinit var manager: TvConnectionManager

    @Before
    fun setUp() {
        manager = TvConnectionManager(CoroutineScope(SupervisorJob() + Dispatchers.Main))
    }

    @Test
    fun `handleMessage auth_ok transitions to CONNECTED`() {
        manager.handleMessage("""{"evt":"auth_ok"}""")
        ShadowLooper.idleMainLooper()
        assertThat(manager.connectionState.value).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)
    }

    @Test
    fun `handleMessage paired sets device info and transitions to CONNECTED`() {
        manager.handleMessage("""{"evt":"paired","token":"abc123","deviceId":"dev1","deviceName":"TV"}""")
        ShadowLooper.idleMainLooper()
        assertThat(manager.tvState.deviceId).isEqualTo("dev1")
        assertThat(manager.tvState.deviceName).isEqualTo("TV")
        assertThat(manager.connectionState.value).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)
    }

    @Test
    fun `handleMessage track_changed updates now playing`() {
        manager.handleMessage("""{"evt":"track_changed","title":"My Song","playlist":"Best Hits"}""")
        ShadowLooper.idleMainLooper()

        assertThat(manager.tvState.nowPlayingTitle).isEqualTo("My Song")
        assertThat(manager.tvState.nowPlayingPlaylist).isEqualTo("Best Hits")
    }

    @Test
    fun `handleMessage playback_state updates isPlaying`() {
        manager.handleMessage("""{"evt":"playback_state","isPlaying":true}""")
        ShadowLooper.idleMainLooper()

        assertThat(manager.tvState.isPlaying).isTrue()
    }

    @Test
    fun `handleMessage with invalid JSON does not crash`() {
        manager.handleMessage("not json at all")
        ShadowLooper.idleMainLooper()
    }
}
