package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
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
class TvConnectionManagerCommandTest {

    private lateinit var manager: TvConnectionManager
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        manager = TvConnectionManager(CoroutineScope(SupervisorJob() + Dispatchers.Main))
        server = MockWebServer()
    }

    @After
    fun tearDown() {
        try {
            manager.disconnect()
            server.shutdown()
        } catch (_: Exception) {}
    }

    private fun connectAndAuth() {
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()
        manager.connect("127.0.0.1", server.port, "valid-token")

        // Wait for connection
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun `sendPlay serializes presetIndex correctly`() {
        val messages = mutableListOf<String>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                messages.add(text)
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()
        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        manager.sendPlay(2)
        Thread.sleep(500)

        val playMsg = messages.find { JSONObject(it).optString("cmd") == "play" }
        assertThat(playMsg).isNotNull()
        val json = JSONObject(playMsg!!)
        assertThat(json.getString("cmd")).isEqualTo("play")
        assertThat(json.getInt("presetIndex")).isEqualTo(2)
    }

    @Test
    fun `sendStop serializes correctly`() {
        val messages = mutableListOf<String>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                messages.add(text)
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()
        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        manager.sendStop()
        Thread.sleep(500)

        val stopMsg = messages.find { JSONObject(it).optString("cmd") == "stop" }
        assertThat(stopMsg).isNotNull()
    }

    @Test
    fun `sendSeek serializes offsetSec correctly`() {
        val messages = mutableListOf<String>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                messages.add(text)
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()
        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        manager.sendSeek(30)
        Thread.sleep(500)

        val seekMsg = messages.find { JSONObject(it).optString("cmd") == "seek" }
        assertThat(seekMsg).isNotNull()
        assertThat(JSONObject(seekMsg!!).getInt("offsetSec")).isEqualTo(30)
    }

    @Test
    fun `sendSkip serializes direction correctly`() {
        val messages = mutableListOf<String>()
        server.enqueue(MockResponse().withWebSocketUpgrade(object : okhttp3.WebSocketListener() {
            override fun onMessage(webSocket: okhttp3.WebSocket, text: String) {
                messages.add(text)
                val json = JSONObject(text)
                if (json.optString("cmd") == "auth") {
                    webSocket.send("""{"evt":"auth_ok"}""")
                }
            }
        }))
        server.start()
        manager.connect("127.0.0.1", server.port, "token")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        manager.sendSkip(-1)
        Thread.sleep(500)

        val skipMsg = messages.find { JSONObject(it).optString("cmd") == "skip" }
        assertThat(skipMsg).isNotNull()
        assertThat(JSONObject(skipMsg!!).getInt("direction")).isEqualTo(-1)
    }
}
