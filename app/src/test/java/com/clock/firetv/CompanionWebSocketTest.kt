package com.clock.firetv

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class CompanionWebSocketTest {

    // --- Static utility tests (existing) ---

    @Test
    fun `generatePin returns 4-digit string`() {
        val pin = CompanionCommandHandler.generatePin()
        assertThat(pin).hasLength(4)
        assertThat(pin.toIntOrNull()).isNotNull()
    }

    @Test
    fun `generatePin is in range 1000-9999`() {
        repeat(100) {
            val pin = CompanionCommandHandler.generatePin().toInt()
            assertThat(pin).isAtLeast(1000)
            assertThat(pin).isAtMost(9999)
        }
    }

    @Test
    fun `generateToken returns 32 character hex string`() {
        val token = CompanionCommandHandler.generateToken()
        assertThat(token).hasLength(32)
        assertThat(token).matches("[0-9a-f]{32}")
    }

    @Test
    fun `generateToken produces unique values`() {
        val tokens = (1..10).map { CompanionCommandHandler.generateToken() }.toSet()
        assertThat(tokens).hasSize(10)
    }

    @Test
    fun `generatePin produces varying values`() {
        val pins = (1..20).map { CompanionCommandHandler.generatePin() }.toSet()
        assertThat(pins.size).isGreaterThan(1)
    }

    // --- Pairing state machine tests ---

    private lateinit var server: CompanionWebSocket
    private lateinit var commandHandler: CompanionCommandHandler
    private lateinit var settings: SettingsManager
    private lateinit var identity: DeviceIdentity
    private lateinit var okClient: OkHttpClient
    private var capturedPin: String? = null
    private var pinDismissed = false
    private var companionConnected = false

    private val testListener = object : CompanionCommandHandler.Listener {
        override fun onCompanionConnected() { companionConnected = true }
        override fun onCompanionDisconnected() { companionConnected = false }
        override fun onShowPin(pin: String) { capturedPin = pin }
        override fun onDismissPin() { pinDismissed = true }
        override fun onPlayPreset(index: Int) {}
        override fun onStopPlayback() {}
        override fun onPausePlayback() {}
        override fun onResumePlayback() {}
        override fun onSeek(offsetSec: Int) {}
        override fun onSkip(direction: Int) {}
        override fun onSyncConfig(config: JSONObject) {}
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("firetv_clock_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
        settings = SettingsManager(context)
        identity = DeviceIdentity(context)
        okClient = OkHttpClient.Builder()
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        commandHandler = CompanionCommandHandler(settings, identity)
        commandHandler.listener = testListener
        server = CompanionWebSocket(commandHandler, 0)
        server.startServer()
    }

    @After
    fun tearDown() {
        try { server.stop() } catch (_: Exception) {}
        okClient.dispatcher.executorService.shutdown()
    }

    private fun connectClient(): Pair<WebSocket, CopyOnWriteArrayList<String>> {
        val messages = CopyOnWriteArrayList<String>()
        val connected = CountDownLatch(1)
        val ws = okClient.newWebSocket(
            Request.Builder().url("ws://127.0.0.1:${server.actualPort}").build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    connected.countDown()
                }
                override fun onMessage(webSocket: WebSocket, text: String) {
                    messages.add(text)
                }
            }
        )
        connected.await(3, TimeUnit.SECONDS)
        return ws to messages
    }

    private fun sendAndWait(ws: WebSocket, json: JSONObject, messages: List<String>, expectedSize: Int) {
        ws.send(json.toString())
        val deadline = System.currentTimeMillis() + 3000
        while (messages.size < expectedSize && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
            ShadowLooper.idleMainLooper()
        }
    }

    private fun doPairing(ws: WebSocket, messages: CopyOnWriteArrayList<String>): String {
        // Send pair_request
        sendAndWait(ws, JSONObject().apply { put("cmd", "pair_request") }, messages, 0)
        ShadowLooper.idleMainLooper()
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()
        val pin = capturedPin ?: throw AssertionError("PIN not captured")

        // Send pair_confirm with correct PIN
        val beforeSize = messages.size
        sendAndWait(ws, JSONObject().apply {
            put("cmd", "pair_confirm")
            put("pin", pin)
        }, messages, beforeSize + 1)
        ShadowLooper.idleMainLooper()
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        // Find the paired event
        val pairedMsg = messages.find { JSONObject(it).optString("evt") == "paired" }
            ?: throw AssertionError("No paired event received. Messages: $messages")
        return JSONObject(pairedMsg).getString("token")
    }

    @Test
    fun `successful pairing flow issues token`() {
        val (ws, messages) = connectClient()
        val token = doPairing(ws, messages)
        assertThat(token).hasLength(32)
        assertThat(token).matches("[0-9a-f]{32}")
        ws.close(1000, "done")
    }

    @Test
    fun `wrong PIN returns auth_failed invalid_pin`() {
        val (ws, messages) = connectClient()

        // Request pairing
        sendAndWait(ws, JSONObject().apply { put("cmd", "pair_request") }, messages, 0)
        ShadowLooper.idleMainLooper()
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        // Send wrong PIN
        val beforeSize = messages.size
        sendAndWait(ws, JSONObject().apply {
            put("cmd", "pair_confirm")
            put("pin", "0000")
        }, messages, beforeSize + 1)

        val failMsg = messages.find { JSONObject(it).optString("evt") == "auth_failed" }
        assertThat(failMsg).isNotNull()
        assertThat(JSONObject(failMsg!!).getString("reason")).isEqualTo("invalid_pin")
        ws.close(1000, "done")
    }

    @Test
    fun `3 wrong PINs triggers rate limiting`() {
        val (ws, messages) = connectClient()

        // Request pairing
        sendAndWait(ws, JSONObject().apply { put("cmd", "pair_request") }, messages, 0)
        ShadowLooper.idleMainLooper()
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        // Send 3 wrong PINs
        repeat(3) {
            val beforeSize = messages.size
            sendAndWait(ws, JSONObject().apply {
                put("cmd", "pair_confirm")
                put("pin", "0000")
            }, messages, beforeSize + 1)
        }

        // The 3rd attempt should return rate_limited
        val lastFail = messages.last { JSONObject(it).optString("evt") == "auth_failed" }
        assertThat(JSONObject(lastFail).getString("reason")).isEqualTo("rate_limited")

        // Subsequent pair_request should also be rate_limited
        val beforeSize = messages.size
        sendAndWait(ws, JSONObject().apply { put("cmd", "pair_request") }, messages, beforeSize + 1)
        val rateLimited = messages.findLast { JSONObject(it).optString("evt") == "auth_failed" }
        assertThat(JSONObject(rateLimited!!).getString("reason")).isEqualTo("rate_limited")

        ws.close(1000, "done")
    }

    @Test
    fun `PIN expiration rejects late confirmation`() {
        val (ws, messages) = connectClient()

        // Request pairing
        sendAndWait(ws, JSONObject().apply { put("cmd", "pair_request") }, messages, 0)
        ShadowLooper.idleMainLooper()
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()
        val pin = capturedPin ?: throw AssertionError("PIN not captured")

        // Advance time past PIN_VALIDITY_MS (60s) - fires the cleanup callback
        ShadowLooper.idleMainLooper(61, TimeUnit.SECONDS)

        // Try to confirm with the expired PIN
        val beforeSize = messages.size
        sendAndWait(ws, JSONObject().apply {
            put("cmd", "pair_confirm")
            put("pin", pin)
        }, messages, beforeSize + 1)

        val failMsg = messages.findLast { JSONObject(it).optString("evt") == "auth_failed" }
        assertThat(failMsg).isNotNull()
        assertThat(JSONObject(failMsg!!).getString("reason")).isEqualTo("pin_expired")
        ws.close(1000, "done")
    }

    @Test
    fun `token storage rotation evicts oldest beyond max 4`() {
        // Do 5 pairings to generate 5 tokens
        val tokens = mutableListOf<String>()
        repeat(5) {
            capturedPin = null
            val (ws, messages) = connectClient()
            val token = doPairing(ws, messages)
            tokens.add(token)
            ws.close(1000, "done")
            Thread.sleep(200)
        }

        // Read stored tokens from SharedPreferences
        val storedJson = settings.prefs.getString("companion_auth_tokens", "[]")
        val storedTokens = JSONArray(storedJson)
        assertThat(storedTokens.length()).isEqualTo(4)

        // First token should have been evicted (FIFO)
        val storedList = (0 until storedTokens.length()).map { storedTokens.getString(it) }
        assertThat(storedList).doesNotContain(tokens[0])
        assertThat(storedList).contains(tokens[4])
    }

    @Test
    fun `token auth with valid token returns auth_ok`() {
        val (ws1, messages1) = connectClient()
        val token = doPairing(ws1, messages1)
        ws1.close(1000, "done")
        Thread.sleep(200)

        // Reconnect with the token
        val (ws2, messages2) = connectClient()
        sendAndWait(ws2, JSONObject().apply {
            put("cmd", "auth")
            put("token", token)
        }, messages2, 1)

        val authOk = messages2.find { JSONObject(it).optString("evt") == "auth_ok" }
        assertThat(authOk).isNotNull()
        ws2.close(1000, "done")
    }

    @Test
    fun `second client replaces first with disconnected event`() {
        val (wsA, messagesA) = connectClient()
        Thread.sleep(200)

        // Connect second client - should replace first
        val (wsB, messagesB) = connectClient()
        Thread.sleep(500)
        ShadowLooper.idleMainLooper()

        val disconnectedMsg = messagesA.find {
            try { JSONObject(it).optString("evt") == "disconnected" } catch (_: Exception) { false }
        }
        assertThat(disconnectedMsg).isNotNull()
        assertThat(JSONObject(disconnectedMsg!!).getString("reason")).isEqualTo("replaced")

        wsA.close(1000, "done")
        wsB.close(1000, "done")
    }
}
