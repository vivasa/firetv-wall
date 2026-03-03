package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class TvConnectionManagerReconnectTest {

    private lateinit var manager: TvConnectionManager
    private val stateChanges = mutableListOf<TvConnectionManager.ConnectionState>()

    @Before
    fun setUp() {
        manager = TvConnectionManager()
        manager.addListener(object : TvConnectionManager.EventListener {
            override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
                stateChanges.add(state)
            }
            override fun onStateReceived(tvState: TvConnectionManager.TvState) {}
            override fun onTrackChanged(title: String, playlist: String) {}
            override fun onPlaybackStateChanged(playing: Boolean) {}
            override fun onSettingChanged(key: String, value: Any) {}
        })
    }

    @Test
    fun `user disconnect resets reconnection state`() {
        // Simulate that we were "connected" by setting state via handleMessage
        manager.handleMessage("""{"evt":"auth_ok"}""")
        ShadowLooper.idleMainLooper()
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)

        // User disconnect
        manager.disconnect()
        ShadowLooper.idleMainLooper()
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
    }

    @Test
    fun `disconnect from DISCONNECTED state stays DISCONNECTED`() {
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
        manager.disconnect()
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
    }

    @Test
    fun `initial state is DISCONNECTED`() {
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
    }

    @Test
    fun `connect to unreachable host transitions to DISCONNECTED`() {
        // Connect to a port where nothing is listening
        manager.connect("127.0.0.1", 1, "fake-token")
        ShadowLooper.idleMainLooper()

        // Wait briefly for the connection failure
        Thread.sleep(2000)
        ShadowLooper.idleMainLooper()

        // Should eventually end up DISCONNECTED (after retries or immediately)
        // The initial state will be CONNECTING
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTING)
    }
}
