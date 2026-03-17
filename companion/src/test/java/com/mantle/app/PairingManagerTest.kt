package com.mantle.app

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PairingManagerTest {

    private lateinit var testScope: TestScope
    private lateinit var connectionManager: TvConnectionManager
    private lateinit var deviceStore: DeviceStore
    private lateinit var pairingManager: PairingManager

    private val testDevice = DeviceItem(
        deviceId = "tv-1",
        deviceName = "Test TV",
        host = "192.168.1.10",
        port = 8899,
        isPaired = false
    )

    @Before
    fun setUp() {
        testScope = TestScope()
        connectionManager = TvConnectionManager(testScope)
        deviceStore = DeviceStore(ApplicationProvider.getApplicationContext())
        pairingManager = PairingManager(connectionManager, deviceStore, testScope)
    }

    @After
    fun tearDown() {
        pairingManager.resetState()
        testScope.cancel()
    }

    @Test
    fun `initial state is Idle`() {
        assertThat(pairingManager.pairingState.value).isEqualTo(PairingState.Idle)
    }

    @Test
    fun `startPairing transitions to AwaitingPin`() {
        pairingManager.startPairing(testDevice)
        testScope.runCurrent()
        assertThat(pairingManager.pairingState.value).isEqualTo(PairingState.AwaitingPin("Test TV"))
    }

    @Test
    fun `confirmPin transitions to Confirming`() {
        pairingManager.startPairing(testDevice)
        testScope.runCurrent()

        pairingManager.confirmPin("1234")
        testScope.runCurrent()

        assertThat(pairingManager.pairingState.value).isEqualTo(PairingState.Confirming("Test TV"))
    }

    @Test
    fun `successful pairing transitions to Paired and persists device`() {
        pairingManager.startPairing(testDevice)
        testScope.runCurrent()

        pairingManager.confirmPin("1234")
        testScope.runCurrent()

        // Simulate server sending paired event
        connectionManager.handleMessage("""{"evt":"paired","token":"new-token","deviceId":"tv-1","deviceName":"Test TV"}""")
        ShadowLooper.idleMainLooper()
        testScope.runCurrent()

        val state = pairingManager.pairingState.value
        assertThat(state).isInstanceOf(PairingState.Paired::class.java)
        assertThat((state as PairingState.Paired).token).isEqualTo("new-token")

        // Check device was persisted
        val paired = deviceStore.getPairedDevices()
        assertThat(paired.any { it.deviceId == "tv-1" && it.token == "new-token" }).isTrue()
    }

    @Test
    fun `auth failed transitions to Failed`() {
        pairingManager.startPairing(testDevice)
        testScope.runCurrent()

        pairingManager.confirmPin("0000")
        testScope.runCurrent()

        connectionManager.handleMessage("""{"evt":"auth_failed","reason":"invalid_pin"}""")
        ShadowLooper.idleMainLooper()
        testScope.runCurrent()

        val state = pairingManager.pairingState.value
        assertThat(state).isInstanceOf(PairingState.Failed::class.java)
        assertThat((state as PairingState.Failed).reason).isEqualTo("invalid_pin")
    }

    @Test
    fun `cancelPairing resets to Idle`() {
        pairingManager.startPairing(testDevice)
        testScope.runCurrent()

        pairingManager.cancelPairing()
        testScope.runCurrent()

        assertThat(pairingManager.pairingState.value).isEqualTo(PairingState.Idle)
    }
}
