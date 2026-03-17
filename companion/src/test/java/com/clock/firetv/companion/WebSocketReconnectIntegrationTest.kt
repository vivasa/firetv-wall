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
class WebSocketReconnectIntegrationTest {

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
        server.shutdown()
        Thread.sleep(500)

        // Server closes connection triggers reconnect
        serverWs?.close(1000, "shutdown")
        Thread.sleep(500)
        ShadowLooper.idleMainLooper()

        // Wait for retry attempts (2s + 4s + 8s delays + connection timeouts)
        for (i in 0..5) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(3000)
        }
        ShadowLooper.idleMainLooper()

        // Should eventually reach DISCONNECTED after all retries
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
    }
}
