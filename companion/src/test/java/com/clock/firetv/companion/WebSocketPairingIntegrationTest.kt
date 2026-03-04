package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import okhttp3.Response
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
import com.mantle.app.TvConnectionManager

@RunWith(RobolectricTestRunner::class)
class WebSocketPairingIntegrationTest {

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
            override fun onTrackChanged(title: String, playlist: String) {}
            override fun onPlaybackStateChanged(playing: Boolean) {}
            override fun onConfigApplied(version: Int) {}
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
    fun `complete pairing flow issues token and state dump`() {
        val receivedMessages = mutableListOf<String>()

        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                receivedMessages.add(text)
                val json = JSONObject(text)
                when (json.optString("cmd")) {
                    "pair_request" -> {
                        // Server would show PIN and wait for confirmation
                        // In real flow, user enters PIN on phone
                    }
                    "pair_confirm" -> {
                        val pin = json.optString("pin")
                        if (pin == "1234") {
                            webSocket.send(JSONObject().apply {
                                put("evt", "paired")
                                put("token", "test-token-abc")
                                put("deviceId", "tv-001")
                                put("deviceName", "Living Room TV")
                            }.toString())
                            // Send state dump
                            webSocket.send(JSONObject().apply {
                                put("evt", "state")
                                put("data", JSONObject().apply {
                                    put("theme", 1)
                                    put("deviceId", "tv-001")
                                    put("deviceName", "Living Room TV")
                                })
                            }.toString())
                        } else {
                            webSocket.send("""{"evt":"error","message":"invalid_pin"}""")
                        }
                    }
                }
            }
        }))
        server.start()

        manager.connectForPairing("127.0.0.1", server.port)
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        // Send pair request
        manager.sendPairRequest()
        Thread.sleep(500)

        // Send pair confirm with correct PIN
        manager.sendPairConfirm("1234")
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        assertThat(manager.lastPairedToken).isEqualTo("test-token-abc")
        assertThat(manager.tvState.deviceId).isEqualTo("tv-001")
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)
    }

    @Test
    fun `pairing with wrong PIN receives error`() {
        val receivedEvents = mutableListOf<String>()

        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val json = JSONObject(text)
                when (json.optString("cmd")) {
                    "pair_confirm" -> {
                        webSocket.send(JSONObject().apply {
                            put("evt", "auth_failed")
                            put("reason", "invalid_pin")
                        }.toString())
                    }
                }
            }
        }))
        server.start()

        manager.connectForPairing("127.0.0.1", server.port)
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        manager.sendPairConfirm("0000") // wrong PIN
        Thread.sleep(1000)
        ShadowLooper.idleMainLooper()

        // auth_failed triggers disconnect
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.DISCONNECTED)
    }
}
