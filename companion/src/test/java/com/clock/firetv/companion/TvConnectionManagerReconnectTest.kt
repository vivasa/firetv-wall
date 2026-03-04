package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import com.mantle.app.TvConnectionManager
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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
            override fun onTrackChanged(title: String, playlist: String) {}
            override fun onPlaybackStateChanged(playing: Boolean) {}
            override fun onConfigApplied(version: Int) {}
        })
    }

    @Test
    fun `user disconnect resets reconnection state`() {
        manager.handleMessage("""{"evt":"auth_ok"}""")
        ShadowLooper.idleMainLooper()
        assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)

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
    fun `connect to unreachable host transitions to CONNECTING`() {
        manager.connect("127.0.0.1", 1, "fake-token")
        ShadowLooper.idleMainLooper()
        Thread.sleep(2000)
        ShadowLooper.idleMainLooper()
        assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTING)
    }

    // --- Reconnection backoff tests ---

    /**
     * Connect to MockWebServer, wait for auth_ok to be sent,
     * then idle the looper so wasConnected=true and state=CONNECTED.
     * Returns the server-side WebSocket so the caller can close it.
     */
    private fun connectAndAuthenticate(server: MockWebServer): WebSocket {
        val serverWsLatch = CountDownLatch(1)
        var serverWs: WebSocket? = null

        server.enqueue(MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                serverWs = webSocket
                webSocket.send("""{"evt":"auth_ok"}""")
                serverWsLatch.countDown()
            }
            override fun onMessage(webSocket: WebSocket, text: String) {}
        }))

        manager.connect(server.hostName, server.port, "test-token")

        // Wait for the server to open the WebSocket and send auth_ok
        serverWsLatch.await(5, TimeUnit.SECONDS)
        Thread.sleep(200) // let OkHttp deliver the message

        // Process auth_ok on main looper — sets wasConnected=true, state=CONNECTED
        ShadowLooper.idleMainLooper()

        return serverWs!!
    }

    /**
     * Close the server-side WebSocket and let the manager detect the disconnect.
     */
    private fun triggerServerDisconnect(serverWs: WebSocket) {
        serverWs.close(1001, "test-disconnect")
        Thread.sleep(500) // let OkHttp fire onClosed callback
        ShadowLooper.idleMainLooper() // process handleDisconnect state change
    }

    @Test
    fun `reconnection enters RECONNECTING state after unexpected disconnect`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.CONNECTED)

            triggerServerDisconnect(serverWs)

            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)
            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
        } finally {
            manager.disconnect()
            server.shutdown()
        }
    }

    @Test
    fun `first reconnect attempt occurs after approximately 2 seconds`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            triggerServerDisconnect(serverWs)
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.RECONNECTING)

            // Shut down the server so reconnect attempts fail immediately
            server.shutdown()
            Thread.sleep(200)

            // Clear to observe only reconnection-phase state changes
            stateChanges.clear()

            // Before 2s, the reconnect callback shouldn't have fired
            ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
            assertThat(stateChanges).isEmpty()

            // After 2s total, advance past the first reconnect delay
            ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)
            Thread.sleep(1000) // let OkHttp process the (failing) reconnect attempt
            ShadowLooper.idleMainLooper()

            // The failed reconnect attempt triggers handleDisconnect which produces state changes
            assertThat(stateChanges).isNotEmpty()
        } finally {
            manager.disconnect()
            try { server.shutdown() } catch (_: Exception) {}
        }
    }

    @Test
    fun `max retries exhausted transitions to DISCONNECTED`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            triggerServerDisconnect(serverWs)

            // Shut down the server so reconnect attempts fail immediately
            server.shutdown()
            Thread.sleep(200)

            // Advance through all 3 retry delays (2s + 4s + 8s) + connection failure time
            for (i in 0 until 3) {
                ShadowLooper.idleMainLooper(10, TimeUnit.SECONDS)
                Thread.sleep(2000) // let OkHttp process the (failing) reconnect attempt
                ShadowLooper.idleMainLooper()
            }

            // Extra time for the final failure to propagate
            Thread.sleep(1000)
            ShadowLooper.idleMainLooper()

            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
        } finally {
            manager.disconnect()
            try { server.shutdown() } catch (_: Exception) {}
        }
    }

    @Test
    fun `user disconnect during reconnection cancels retries`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            triggerServerDisconnect(serverWs)
            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)

            // User disconnect while in RECONNECTING state
            manager.disconnect()
            ShadowLooper.idleMainLooper()
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)

            // Advance past all retry delays — should not attempt reconnection
            stateChanges.clear()
            ShadowLooper.idleMainLooper(15, TimeUnit.SECONDS)
            Thread.sleep(500)
            ShadowLooper.idleMainLooper()

            assertThat(stateChanges.filter {
                it == TvConnectionManager.ConnectionState.CONNECTING
            }).isEmpty()
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun `keepalive timeout from server closes connection and triggers reconnect`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.CONNECTED)

            // Server closes connection (simulating keepalive timeout)
            triggerServerDisconnect(serverWs)

            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
        } finally {
            manager.disconnect()
            server.shutdown()
        }
    }
}
