package com.mantle.app

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ConfigSyncManagerTest {

    private lateinit var testScope: TestScope
    private lateinit var configStore: MantleConfigStore
    private val sentMessages = mutableListOf<JSONObject>()
    private lateinit var syncManager: ConfigSyncManager

    @Before
    fun setUp() {
        testScope = TestScope()
        configStore = MantleConfigStore(ApplicationProvider.getApplicationContext(), testScope)
        sentMessages.clear()
        syncManager = ConfigSyncManager(configStore, testScope) { json ->
            sentMessages.add(json)
        }
    }

    @Test
    fun `setConnected true triggers immediate sync`() = testScope.runTest {
        syncManager.setConnected(true)
        runCurrent()

        assertEquals(1, sentMessages.size)
        assertEquals("sync_config", sentMessages[0].getString("cmd"))
    }

    @Test
    fun `no sync when disconnected`() = testScope.runTest {
        syncManager.onConfigChanged(configStore.config)
        advanceTimeBy(1000)
        runCurrent()

        assertEquals(0, sentMessages.size)
    }

    @Test
    fun `config change triggers debounced sync when connected`() = testScope.runTest {
        syncManager.setConnected(true)
        runCurrent()
        sentMessages.clear()

        syncManager.onConfigChanged(configStore.config)
        runCurrent()

        // Not yet — debounce hasn't elapsed
        assertEquals(0, sentMessages.size)

        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, sentMessages.size)
        assertEquals("sync_config", sentMessages[0].getString("cmd"))
    }

    @Test
    fun `rapid config changes coalesce into single sync`() = testScope.runTest {
        syncManager.setConnected(true)
        runCurrent()
        sentMessages.clear()

        syncManager.onConfigChanged(configStore.config)
        advanceTimeBy(200)
        syncManager.onConfigChanged(configStore.config)
        advanceTimeBy(200)
        syncManager.onConfigChanged(configStore.config)
        runCurrent()

        // Still within debounce window of last change
        assertEquals(0, sentMessages.size)

        advanceTimeBy(500)
        runCurrent()

        // Only one sync fires
        assertEquals(1, sentMessages.size)
    }

    @Test
    fun `setConnected false stops pending syncs`() = testScope.runTest {
        syncManager.setConnected(true)
        runCurrent()
        sentMessages.clear()

        syncManager.onConfigChanged(configStore.config)
        advanceTimeBy(200)

        syncManager.setConnected(false)
        advanceTimeBy(500)
        runCurrent()

        assertEquals(0, sentMessages.size)
    }

    @Test
    fun `flushSync sends immediately and cancels debounce`() = testScope.runTest {
        syncManager.setConnected(true)
        runCurrent()
        sentMessages.clear()

        // Trigger a debounced change
        syncManager.onConfigChanged(configStore.config)
        runCurrent()
        assertEquals(0, sentMessages.size) // Still debouncing

        // Flush immediately
        syncManager.flushSync()
        assertEquals(1, sentMessages.size)
        assertEquals("sync_config", sentMessages[0].getString("cmd"))

        // Debounce should have been cancelled — no duplicate after delay
        advanceTimeBy(1000)
        runCurrent()
        assertEquals(1, sentMessages.size) // Still just 1
    }

    @Test
    fun `flushSync does nothing when disconnected`() = testScope.runTest {
        syncManager.flushSync()
        assertEquals(0, sentMessages.size)
    }
}
