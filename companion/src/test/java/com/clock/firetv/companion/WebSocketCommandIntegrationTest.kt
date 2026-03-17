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
import kotlinx.coroutines.SupervisorJob
import com.mantle.app.TvConnectionManager

@RunWith(RobolectricTestRunner::class)
class WebSocketCommandIntegrationTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var server: MockWebServer
    private val receivedCommands = mutableListOf<JSONObject>()
    private var serverSocket: WebSocket? = null

    @Before
    fun setUp() {
        manager = TvConnectionManager(CoroutineScope(SupervisorJob() + Dispatchers.Main))
        server = MockWebServer()

        // Set up server that auto-authenticates and captures commands
        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    serverSocket = webSocket
                    webSocket.send("""{"evt":"auth_ok"}""")
                } else {
                    receivedCommands.add(json)
                }
            }
        }))
        server.start()

        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1500)
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        try {
            manager.disconnect()
            server.shutdown()
        } catch (_: Exception) {}
    }

    @Test
    fun `play command is received by server with correct structure`() {
        manager.sendPlay(0)
        Thread.sleep(500)
        val cmd = receivedCommands.find { it.optString("cmd") == "play" }
        assertThat(cmd).isNotNull()
        assertThat(cmd!!.getInt("presetIndex")).isEqualTo(0)
    }

    @Test
    fun `stop command is received by server`() {
        manager.sendStop()
        Thread.sleep(500)
        val cmd = receivedCommands.find { it.optString("cmd") == "stop" }
        assertThat(cmd).isNotNull()
    }

    @Test
    fun `seek command is received with correct offset`() {
        manager.sendSeek(30)
        Thread.sleep(500)
        val cmd = receivedCommands.find { it.optString("cmd") == "seek" }
        assertThat(cmd).isNotNull()
        assertThat(cmd!!.getInt("offsetSec")).isEqualTo(30)
    }

    @Test
    fun `server track_changed event is received by client`() {
        serverSocket?.send("""{"evt":"track_changed","title":"New Song","playlist":"My List"}""")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        assertThat(manager.tvState.nowPlayingTitle).isEqualTo("New Song")
        assertThat(manager.tvState.nowPlayingPlaylist).isEqualTo("My List")
    }

}
