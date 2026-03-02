package com.clock.firetv.companion

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.progressindicator.LinearProgressIndicator
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class DevicesFragment : Fragment() {

    companion object {
        private const val TAG = "DevicesFragment"
        private const val SERVICE_TYPE = "_firetvclock._tcp."
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    private lateinit var deviceList: RecyclerView
    private lateinit var scanningProgress: LinearProgressIndicator
    private lateinit var emptyState: View
    private lateinit var btnManualEntry: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var nsdManager: NsdManager? = null
    private var discovering = false
    private val discoveredDevices = mutableMapOf<String, DeviceAdapter.DeviceItem>()
    private lateinit var adapter: DeviceAdapter

    private val connectionManager get() = CompanionApp.instance.connectionManager
    private val deviceStore get() = CompanionApp.instance.deviceStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        deviceList = view.findViewById(R.id.deviceList)
        scanningProgress = view.findViewById(R.id.scanningProgress)
        emptyState = view.findViewById(R.id.emptyState)
        btnManualEntry = view.findViewById(R.id.btnManualEntry)

        adapter = DeviceAdapter(
            onAction = { device -> onDeviceAction(device) },
            onLongPress = { device -> onDeviceLongPress(device) }
        )
        deviceList.layoutManager = LinearLayoutManager(requireContext())
        deviceList.adapter = adapter

        btnManualEntry.setOnClickListener { showManualEntryDialog() }

        loadPairedDevices()
    }

    override fun onResume() {
        super.onResume()
        startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        stopDiscovery()
    }

    private fun loadPairedDevices() {
        val paired = deviceStore.getPairedDevices()
        paired.forEach { pd ->
            discoveredDevices[pd.deviceId] = DeviceAdapter.DeviceItem(
                deviceId = pd.deviceId,
                deviceName = pd.deviceName,
                host = pd.host,
                port = pd.port,
                isPaired = true,
                storedToken = pd.token
            )
        }
        refreshList()
    }

    private fun startDiscovery() {
        if (discovering) return
        nsdManager = requireContext().getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        scanningProgress.visibility = View.VISIBLE
        emptyState.visibility = View.GONE

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            discovering = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NSD discovery", e)
        }

        handler.postDelayed({
            if (discovering && discoveredDevices.isEmpty()) {
                emptyState.visibility = View.VISIBLE
            }
            stopDiscovery()
        }, SCAN_TIMEOUT_MS)
    }

    private fun stopDiscovery() {
        if (!discovering) return
        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) { /* ignore */ }
        discovering = false
        scanningProgress.visibility = View.GONE
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "NSD discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(TAG, "NSD service found: ${service.serviceName}")
            nsdManager?.resolveService(service, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.w(TAG, "NSD resolve failed: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceId = getAttributeString(serviceInfo, "deviceId") ?: serviceInfo.serviceName
                    val name = getAttributeString(serviceInfo, "name") ?: serviceInfo.serviceName

                    val paired = deviceStore.getPairedDevices().find { it.deviceId == deviceId }
                    val item = DeviceAdapter.DeviceItem(
                        deviceId = deviceId,
                        deviceName = name,
                        host = host,
                        port = port,
                        isPaired = paired != null,
                        storedToken = paired?.token
                    )
                    handler.post {
                        discoveredDevices[deviceId] = item
                        refreshList()
                        emptyState.visibility = View.GONE
                    }
                }
            })
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.d(TAG, "NSD service lost: ${service.serviceName}")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "NSD discovery stopped")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD start discovery failed: $errorCode")
            discovering = false
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD stop discovery failed: $errorCode")
        }
    }

    private fun getAttributeString(info: NsdServiceInfo, key: String): String? {
        return try {
            val bytes = info.attributes[key]
            bytes?.let { String(it, Charsets.UTF_8) }
        } catch (e: Exception) {
            null
        }
    }

    private fun refreshList() {
        val items = discoveredDevices.values.sortedWith(
            compareByDescending<DeviceAdapter.DeviceItem> { it.isPaired }
                .thenBy { it.deviceName }
        )
        adapter.setItems(items)
    }

    private fun onDeviceAction(device: DeviceAdapter.DeviceItem) {
        if (device.isPaired && device.storedToken != null) {
            // Connect with saved token
            connectionManager.connect(device.host, device.port, device.storedToken)
            deviceStore.updateLastConnected(device.deviceId)
            (activity as? MainActivity)?.switchToRemote()
        } else {
            // Start pairing
            startPairing(device)
        }
    }

    private fun onDeviceLongPress(device: DeviceAdapter.DeviceItem) {
        if (!device.isPaired) return
        AlertDialog.Builder(requireContext())
            .setTitle(device.deviceName)
            .setItems(arrayOf("Remove pairing")) { _, _ ->
                deviceStore.removeDevice(device.deviceId)
                discoveredDevices[device.deviceId] = device.copy(isPaired = false, storedToken = null)
                refreshList()
            }
            .show()
    }

    private fun startPairing(device: DeviceAdapter.DeviceItem) {
        connectionManager.connectForPairing(device.host, device.port)

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pair, null)
        val pinInput = dialogView.findViewById<TextInputEditText>(R.id.pinInput)
        val pairError = dialogView.findViewById<TextView>(R.id.pairError)
        dialogView.findViewById<TextView>(R.id.pairTitle).text = "Pairing with ${device.deviceName}"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { d, _ ->
                connectionManager.disconnect()
                d.dismiss()
            }
            .create()

        // Send pair_request after a short delay to let WebSocket connect
        handler.postDelayed({ connectionManager.sendPairRequest() }, 500)

        // 10-second timeout for connection
        val timeoutRunnable = Runnable {
            val s = connectionManager.state
            if (s == TvConnectionManager.ConnectionState.CONNECTING ||
                s == TvConnectionManager.ConnectionState.AUTHENTICATING) {
                connectionManager.disconnect()
                pairError.text = "Could not reach TV"
                pairError.visibility = View.VISIBLE
            }
        }
        handler.postDelayed(timeoutRunnable, 10_000)

        val pairListener = object : TvConnectionManager.EventListener {
            override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
                if (state == TvConnectionManager.ConnectionState.CONNECTED) {
                    handler.removeCallbacks(timeoutRunnable)
                } else if (state == TvConnectionManager.ConnectionState.DISCONNECTED) {
                    handler.removeCallbacks(timeoutRunnable)
                    handler.post {
                        pairError.text = "Connection lost"
                        pairError.visibility = View.VISIBLE
                    }
                }
            }

            override fun onStateReceived(tvState: TvConnectionManager.TvState) {
                // State dump means pairing succeeded — save device with token
                val token = connectionManager.lastPairedToken ?: ""
                deviceStore.addDevice(DeviceStore.PairedDevice(
                    deviceId = tvState.deviceId,
                    deviceName = tvState.deviceName,
                    token = token,
                    host = device.host,
                    port = device.port,
                    lastConnected = System.currentTimeMillis()
                ))
                // Update local list to show as paired
                handler.post {
                    discoveredDevices[tvState.deviceId] = DeviceAdapter.DeviceItem(
                        deviceId = tvState.deviceId,
                        deviceName = tvState.deviceName,
                        host = device.host,
                        port = device.port,
                        isPaired = true,
                        storedToken = token
                    )
                    refreshList()
                    dialog.dismiss()
                    connectionManager.removeListener(this)
                    (activity as? MainActivity)?.switchToRemote()
                }
            }

            override fun onTrackChanged(title: String, playlist: String) {}
            override fun onPlaybackStateChanged(playing: Boolean) {}
            override fun onSettingChanged(key: String, value: Any) {}
        }

        connectionManager.addListener(pairListener)

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text?.toString() ?: ""
                if (pin.length == 4) {
                    connectionManager.sendPairConfirm(pin)
                } else {
                    pairError.text = "Enter a 4-digit PIN"
                    pairError.visibility = View.VISIBLE
                }
            }
        }

        dialog.setOnDismissListener {
            handler.removeCallbacks(timeoutRunnable)
            connectionManager.removeListener(pairListener)
        }

        dialog.show()
    }

    private fun showManualEntryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "192.168.1.52"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Enter Fire TV IP address")
            .setView(input)
            .setPositiveButton("Connect") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotEmpty()) {
                    val manualDevice = DeviceAdapter.DeviceItem(
                        deviceId = "manual_$ip",
                        deviceName = ip,
                        host = ip,
                        port = 8765,
                        isPaired = false
                    )
                    startPairing(manualDevice)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
