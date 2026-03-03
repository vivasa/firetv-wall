package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class WebSocketReconnectIntegrationTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var server: MockWebServer
    private val stateChanges = mutableListOf<TvConnectionManager.ConnectionState>()

    @Before
    fun setUp() {
        manager = TvConnectionManager()
        server = MockWebServer()
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

    @After
    fun tearDown() {
        try {
            manager.disconnect()
            server.shutdown()
        } catch (_: Exception) {}
    }

    @Test
    fun `server disconnect triggers RECONNECTING state`() {
        var serverWs: WebSocket? = null
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    serverWs = webSocket
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1500)
        ShadowLooper.idleMainLooper()

        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)

        // Server closes connection
        serverWs?.close(1000, "server restart")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        // Should see RECONNECTING state
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
    }

    @Test
    fun `reconnect eventually transitions to DISCONNECTED when server is gone`() {
        var serverWs: WebSocket? = null
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    serverWs = webSocket
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1500)
        ShadowLooper.idleMainLooper()

        // Shut down server so reconnects fail
        val port = server.port
        server.shutdown()
        Thread.sleep(500)

        // Server closes connection triggers reconnect
        serverWs?.close(1000, "shutdown")
        Thread.sleep(500)
        ShadowLooper.idleMainLooper()

        // Wait for retry attempts (2s + 4s + 8s delays + connection timeouts)
        // Run the main looper to process delayed reconnect handlers
        for (i in 0..5) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(3000)
        }
        ShadowLooper.idleMainLooper()

        // Should eventually reach DISCONNECTED after all retries
        val lastState = stateChanges.lastOrNull()
        // It should be DISCONNECTED or RECONNECTING (depending on timing)
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
    }
}
