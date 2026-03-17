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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.mantle.app.TvConnectionManager

@RunWith(RobolectricTestRunner::class)
class WebSocketAuthIntegrationTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var server: MockWebServer
    private val stateChanges = mutableListOf<TvConnectionManager.ConnectionState>()
    private lateinit var scope: CoroutineScope
    private lateinit var collectJob: Job

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        manager = TvConnectionManager(scope)
        server = MockWebServer()
        collectJob = scope.launch {
            manager.connectionState.collect { stateChanges.add(it) }
        }
    }

    @After
    fun tearDown() {
        try {
            collectJob.cancel()
            manager.disconnect()
            server.shutdown()
        } catch (_: Exception) {}
    }

    @Test
    fun `valid token authentication results in auth_ok and connected state`() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth" && json.optString("token") == "valid-token") {
                    webSocket.send("""{"evt":"auth_ok","deviceId":"tv-1","deviceName":"My TV"}""")
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "valid-token")
        Thread.sleep(2000)
        ShadowLooper.idleMainLooper()

        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)
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
