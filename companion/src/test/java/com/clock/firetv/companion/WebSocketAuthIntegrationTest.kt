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
class WebSocketAuthIntegrationTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var server: MockWebServer
    private val stateChanges = mutableListOf<TvConnectionManager.ConnectionState>()
    private var receivedTvState: TvConnectionManager.TvState? = null

    @Before
    fun setUp() {
        manager = TvConnectionManager()
        server = MockWebServer()
        manager.addListener(object : TvConnectionManager.EventListener {
            override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
                stateChanges.add(state)
            }
            override fun onStateReceived(tvState: TvConnectionManager.TvState) {
                receivedTvState = tvState
            }
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
    fun `valid token authentication results in auth_ok and state dump`() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth" && json.optString("token") == "valid-token") {
                    webSocket.send("""{"evt":"auth_ok","deviceId":"tv-1","deviceName":"My TV"}""")
                    webSocket.send(JSONObject().apply {
                        put("evt", "state")
                        put("data", JSONObject().apply {
                            put("theme", 2)
                            put("deviceId", "tv-1")
                            put("deviceName", "My TV")
                        })
                    }.toString())
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "valid-token")
        Thread.sleep(2000)
        ShadowLooper.idleMainLooper()

        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)
        assertThat(receivedTvState).isNotNull()
        assertThat(receivedTvState!!.theme).isEqualTo(2)
    }

    @Test
    fun `invalid token results in auth_failed and disconnect`() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_failed","reason":"invalid_token"}""")
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "bad-token")
        Thread.sleep(2000)
        ShadowLooper.idleMainLooper()

        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.DISCONNECTED)
    }
}
