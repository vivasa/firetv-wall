package com.mantle.app

import android.bluetooth.BluetoothDevice
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceItem(
    val deviceId: String,
    val deviceName: String,
    val host: String,
    val port: Int,
    val isPaired: Boolean,
    val storedToken: String? = null,
    val transportType: TransportType = TransportType.NSD,
    val bleDevice: BluetoothDevice? = null
)

enum class TransportType { NSD, BLE }

class DeviceDiscoveryManager(
    private val nsdManager: NsdManager?,
    private val bleScanner: BleScanner,
    private val deviceStore: DeviceStore,
    private val scope: CoroutineScope
) {

    companion object {
        private const val TAG = "DeviceDiscoveryMgr"
        private val SERVICE_TYPE = ProtocolConfig.NSD_SERVICE_TYPE
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private val discoveredDevices = mutableMapOf<String, DeviceItem>()
    private var discovering = false
    private var scanTimeoutJob: Job? = null

    private val _devices = MutableStateFlow<List<DeviceItem>>(emptyList())
    val devices: StateFlow<List<DeviceItem>> = _devices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    init {
        loadPairedDevices()
    }

    private fun loadPairedDevices() {
        val paired = deviceStore.getPairedDevices()
        paired.forEach { pd ->
            discoveredDevices[pd.deviceId] = DeviceItem(
                deviceId = pd.deviceId,
                deviceName = pd.deviceName,
                host = pd.host,
                port = pd.port,
                isPaired = true,
                storedToken = pd.token
            )
        }
        emitSorted()
    }

    fun startDiscovery() {
        if (discovering) return
        _isScanning.value = true

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            discovering = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NSD discovery", e)
        }

        startBleScan()

        scanTimeoutJob?.cancel()
        scanTimeoutJob = scope.launch {
            delay(SCAN_TIMEOUT_MS)
            stopDiscovery()
        }
    }

    fun stopDiscovery() {
        if (!discovering) return
        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (_: Exception) {}
        bleScanner.stopScan()
        discovering = false
        _isScanning.value = false
        scanTimeoutJob?.cancel()
    }

    fun refreshPairedDevices() {
        val paired = deviceStore.getPairedDevices()
        val pairedIds = paired.map { it.deviceId }.toSet()

        // Update existing devices' paired status
        discoveredDevices.replaceAll { id, item ->
            val pd = paired.find { it.deviceId == id }
            if (pd != null) {
                item.copy(isPaired = true, storedToken = pd.token)
            } else {
                item.copy(isPaired = false, storedToken = null)
            }
        }

        // Add any paired devices not yet in discovered list
        paired.forEach { pd ->
            if (!discoveredDevices.containsKey(pd.deviceId)) {
                discoveredDevices[pd.deviceId] = DeviceItem(
                    deviceId = pd.deviceId,
                    deviceName = pd.deviceName,
                    host = pd.host,
                    port = pd.port,
                    isPaired = true,
                    storedToken = pd.token
                )
            }
        }

        emitSorted()
    }

    private fun startBleScan() {
        bleScanner.listener = object : BleScanner.Listener {
            override fun onDeviceFound(bleDevice: BleScanner.BleDevice) {
                scope.launch {
                    val existing = discoveredDevices[bleDevice.deviceId]
                    if (existing != null && existing.transportType == TransportType.NSD) {
                        return@launch // Prefer NSD over BLE
                    }

                    val paired = deviceStore.getPairedDevices().find { it.deviceId == bleDevice.deviceId }
                    discoveredDevices[bleDevice.deviceId] = DeviceItem(
                        deviceId = bleDevice.deviceId,
                        deviceName = bleDevice.deviceName,
                        host = "",
                        port = 0,
                        isPaired = paired != null,
                        storedToken = paired?.token,
                        transportType = TransportType.BLE,
                        bleDevice = bleDevice.device
                    )
                    emitSorted()
                }
            }
        }
        bleScanner.startScan()
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {}
        override fun onServiceFound(service: NsdServiceInfo) {
            nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                @Suppress("DEPRECATION")
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceId = getAttributeString(serviceInfo, ProtocolKeys.DEVICE_ID) ?: serviceInfo.serviceName
                    val name = getAttributeString(serviceInfo, "name") ?: serviceInfo.serviceName

                    val paired = deviceStore.getPairedDevices().find { it.deviceId == deviceId }
                    val item = DeviceItem(
                        deviceId = deviceId, deviceName = name, host = host, port = port,
                        isPaired = paired != null, storedToken = paired?.token
                    )
                    scope.launch {
                        discoveredDevices[deviceId] = item
                        emitSorted()
                    }
                }
            })
        }
        override fun onServiceLost(service: NsdServiceInfo) {}
        override fun onDiscoveryStopped(serviceType: String) {}
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) { discovering = false }
        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
    }

    private fun getAttributeString(info: NsdServiceInfo, key: String): String? {
        return try {
            info.attributes[key]?.let { String(it, Charsets.UTF_8) }
        } catch (_: Exception) { null }
    }

    private fun emitSorted() {
        _devices.value = discoveredDevices.values
            .sortedWith(compareByDescending<DeviceItem> { it.isPaired }.thenBy { it.deviceName })
            .toList()
    }
}
