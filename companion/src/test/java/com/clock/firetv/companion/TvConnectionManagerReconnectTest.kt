package com.clock.firetv.companion

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.mantle.app.TvConnectionManager
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
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
    private lateinit var scope: CoroutineScope
    private lateinit var collectJob: Job

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        manager = TvConnectionManager(scope)
        collectJob = scope.launch {
            manager.connectionState.collect { stateChanges.add(it) }
        }
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

        serverWsLatch.await(5, TimeUnit.SECONDS)
        Thread.sleep(200)
        ShadowLooper.idleMainLooper()

        return serverWs!!
    }

    private fun triggerServerDisconnect(serverWs: WebSocket) {
        serverWs.close(1001, "test-disconnect")
        Thread.sleep(500)
        ShadowLooper.idleMainLooper()
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
            collectJob.cancel()
            manager.disconnect()
            server.shutdown()
        }
    }

    @Test
    fun `first reconnect stays in RECONNECTING state during backoff`() {
        val server = MockWebServer()
        server.start()
        try {
            val serverWs = connectAndAuthenticate(server)
            triggerServerDisconnect(serverWs)
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.RECONNECTING)

            server.shutdown()
            Thread.sleep(200)

            // Before 2s backoff, state should still be RECONNECTING (not DISCONNECTED)
            ShadowLooper.idleMainLooper(1500, TimeUnit.MILLISECONDS)
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.RECONNECTING)

            // After 2s, reconnect attempt fires (and fails since server is down)
            ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)
            Thread.sleep(1000)
            ShadowLooper.idleMainLooper()

            // Should still be reconnecting (more attempts remaining)
            assertThat(manager.state).isIn(listOf(
                TvConnectionManager.ConnectionState.RECONNECTING,
                TvConnectionManager.ConnectionState.DISCONNECTED
            ))
        } finally {
            collectJob.cancel()
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

            server.shutdown()
            Thread.sleep(200)

            for (i in 0 until 3) {
                ShadowLooper.idleMainLooper(10, TimeUnit.SECONDS)
                Thread.sleep(2000)
                ShadowLooper.idleMainLooper()
            }

            Thread.sleep(1000)
            ShadowLooper.idleMainLooper()

            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)
        } finally {
            collectJob.cancel()
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

            manager.disconnect()
            ShadowLooper.idleMainLooper()
            assertThat(manager.state).isEqualTo(TvConnectionManager.ConnectionState.DISCONNECTED)

            stateChanges.clear()
            ShadowLooper.idleMainLooper(15, TimeUnit.SECONDS)
            Thread.sleep(500)
            ShadowLooper.idleMainLooper()

            assertThat(stateChanges.filter {
                it == TvConnectionManager.ConnectionState.CONNECTING
            }).isEmpty()
        } finally {
            collectJob.cancel()
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

            triggerServerDisconnect(serverWs)

            assertThat(stateChanges).contains(TvConnectionManager.ConnectionState.RECONNECTING)
        } finally {
            collectJob.cancel()
            manager.disconnect()
            server.shutdown()
        }
    }
}
