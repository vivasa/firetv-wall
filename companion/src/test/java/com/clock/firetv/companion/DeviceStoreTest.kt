package com.clock.firetv.companion

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.mantle.app.DeviceStore

@RunWith(RobolectricTestRunner::class)
class DeviceStoreTest {

    private lateinit var store: DeviceStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("companion_devices", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = DeviceStore(context)
    }

    @Test
    fun `addDevice and getPairedDevices returns the device`() {
        val device = DeviceStore.PairedDevice("id1", "TV 1", "token1", "192.168.1.1", 8765, 1000)
        store.addDevice(device)
        val devices = store.getPairedDevices()
        assertThat(devices).hasSize(1)
        assertThat(devices[0].deviceId).isEqualTo("id1")
        assertThat(devices[0].deviceName).isEqualTo("TV 1")
        assertThat(devices[0].token).isEqualTo("token1")
        assertThat(devices[0].host).isEqualTo("192.168.1.1")
        assertThat(devices[0].port).isEqualTo(8765)
    }

    @Test
    fun `addDevice with same deviceId upserts`() {
        val device1 = DeviceStore.PairedDevice("id1", "TV 1", "token1", "192.168.1.1", 8765)
        val device2 = DeviceStore.PairedDevice("id1", "TV 1 Updated", "token2", "192.168.1.2", 8765)
        store.addDevice(device1)
        store.addDevice(device2)
        val devices = store.getPairedDevices()
        assertThat(devices).hasSize(1)
        assertThat(devices[0].deviceName).isEqualTo("TV 1 Updated")
        assertThat(devices[0].token).isEqualTo("token2")
    }

    @Test
    fun `removeDevice removes the device`() {
        store.addDevice(DeviceStore.PairedDevice("id1", "TV 1", "t1", "1.1.1.1", 8765))
        store.addDevice(DeviceStore.PairedDevice("id2", "TV 2", "t2", "2.2.2.2", 8765))
        store.removeDevice("id1")
        val devices = store.getPairedDevices()
        assertThat(devices).hasSize(1)
        assertThat(devices[0].deviceId).isEqualTo("id2")
    }

    @Test
    fun `getLastConnectedDevice returns most recent`() {
        store.addDevice(DeviceStore.PairedDevice("id1", "TV 1", "t1", "1.1.1.1", 8765, lastConnected = 100))
        store.addDevice(DeviceStore.PairedDevice("id2", "TV 2", "t2", "2.2.2.2", 8765, lastConnected = 300))
        store.addDevice(DeviceStore.PairedDevice("id3", "TV 3", "t3", "3.3.3.3", 8765, lastConnected = 200))

        val last = store.getLastConnectedDevice()
        assertThat(last).isNotNull()
        assertThat(last!!.deviceId).isEqualTo("id2")
    }

    @Test
    fun `updateLastConnected updates timestamp`() {
        store.addDevice(DeviceStore.PairedDevice("id1", "TV 1", "t1", "1.1.1.1", 8765, lastConnected = 0))
        store.updateLastConnected("id1")
        val device = store.getPairedDevices().first()
        assertThat(device.lastConnected).isGreaterThan(0L)
    }

    @Test
    fun `getPairedDevices with malformed JSON returns empty list`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("companion_devices", Context.MODE_PRIVATE)
            .edit().putString("paired_devices", "not valid json!!!").commit()
        val freshStore = DeviceStore(context)
        assertThat(freshStore.getPairedDevices()).isEmpty()
    }

    @Test
    fun `getPairedDevices with empty JSON array returns empty list`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("companion_devices", Context.MODE_PRIVATE)
            .edit().putString("paired_devices", "[]").commit()
        val freshStore = DeviceStore(context)
        assertThat(freshStore.getPairedDevices()).isEmpty()
    }

    @Test
    fun `updateLastPresetName stores and round-trips`() {
        store.addDevice(DeviceStore.PairedDevice("id1", "TV 1", "t1", "1.1.1.1", 8765))
        store.updateLastPresetName("id1", "Jazz Standards")
        val device = store.getPairedDevices().first()
        assertThat(device.lastPresetName).isEqualTo("Jazz Standards")
    }

    @Test
    fun `lastPresetName is null by default`() {
        store.addDevice(DeviceStore.PairedDevice("id1", "TV 1", "t1", "1.1.1.1", 8765))
        val device = store.getPairedDevices().first()
        assertThat(device.lastPresetName).isNull()
    }
}
