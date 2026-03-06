package com.mantle.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolKeys
import com.google.android.material.textfield.TextInputEditText

class TvFragment : Fragment() {

    companion object {
        private const val TAG = "TvFragment"
        private val SERVICE_TYPE = ProtocolConfig.NSD_SERVICE_TYPE
        private const val SCAN_TIMEOUT_MS = 15_000L
    }

    // Connection status views
    private lateinit var connectionDot: View
    private lateinit var connectionStatus: TextView
    private lateinit var btnReconnect: MaterialButton
    private lateinit var reconnectingProgress: LinearProgressIndicator

    // Now playing views
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingPlaylist: TextView

    // Playback controls
    private lateinit var btnSkipPrev: MaterialButton
    private lateinit var btnRewind: MaterialButton
    private lateinit var btnPlayPause: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnSkipNext: MaterialButton

    // Presets
    private lateinit var presetChips: ChipGroup

    // Discovery
    private lateinit var deviceList: RecyclerView
    private lateinit var scanningProgress: LinearProgressIndicator
    private lateinit var emptyDeviceState: View
    private lateinit var btnManualEntry: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private var nsdManager: NsdManager? = null
    private var discovering = false
    private val discoveredDevices = mutableMapOf<String, DeviceAdapter.DeviceItem>()
    private lateinit var deviceAdapter: DeviceAdapter

    // BLE scanning
    private val bleScanner = BleScanner()
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            startBleScan()
        } else {
            Log.d(TAG, "BLE permissions denied, skipping BLE scan")
        }
    }

    private val connectionManager get() = MantleApp.instance.connectionManager
    private val configStore get() = MantleApp.instance.configStore
    private val deviceStore get() = MantleApp.instance.deviceStore

    private val eventListener = object : TvConnectionManager.EventListener {
        override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
            updateConnectionUI(state)
            updateControlsEnabled(state == TvConnectionManager.ConnectionState.CONNECTED)
            val connectedId = if (state == TvConnectionManager.ConnectionState.CONNECTED)
                connectionManager.tvState.deviceId else null
            deviceAdapter.setConnectedDeviceId(connectedId)
        }

        override fun onTrackChanged(title: String, playlist: String) {
            updateNowPlaying(title, playlist)
        }

        override fun onPlaybackStateChanged(playing: Boolean) {
            btnPlayPause.setIconResource(if (playing) R.drawable.ic_pause else R.drawable.ic_play)
        }

        override fun onConfigApplied(version: Int) {}
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Connection status
        connectionDot = view.findViewById(R.id.connectionDot)
        connectionStatus = view.findViewById(R.id.connectionStatus)
        btnReconnect = view.findViewById(R.id.btnReconnect)
        reconnectingProgress = view.findViewById(R.id.reconnectingProgress)

        // Now playing
        nowPlayingTitle = view.findViewById(R.id.nowPlayingTitle)
        nowPlayingPlaylist = view.findViewById(R.id.nowPlayingPlaylist)

        // Playback controls
        btnSkipPrev = view.findViewById(R.id.btnSkipPrev)
        btnRewind = view.findViewById(R.id.btnRewind)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnForward = view.findViewById(R.id.btnForward)
        btnSkipNext = view.findViewById(R.id.btnSkipNext)

        // Presets
        presetChips = view.findViewById(R.id.presetChips)

        // Discovery
        deviceList = view.findViewById(R.id.deviceList)
        scanningProgress = view.findViewById(R.id.scanningProgress)
        emptyDeviceState = view.findViewById(R.id.emptyDeviceState)
        btnManualEntry = view.findViewById(R.id.btnManualEntry)

        // Connection dot
        val dotBg = GradientDrawable()
        dotBg.shape = GradientDrawable.OVAL
        dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
        connectionDot.background = dotBg

        // Playback controls
        btnSkipPrev.setOnClickListener { connectionManager.sendSkip(-1) }
        btnRewind.setOnClickListener { connectionManager.sendSeek(-30) }
        btnPlayPause.setOnClickListener {
            if (connectionManager.tvState.isPlaying) connectionManager.sendPause() else connectionManager.sendResume()
        }
        btnForward.setOnClickListener { connectionManager.sendSeek(30) }
        btnSkipNext.setOnClickListener { connectionManager.sendSkip(1) }

        // Reconnect
        btnReconnect.setOnClickListener {
            val lastDevice = deviceStore.getLastConnectedDevice()
            if (lastDevice != null) {
                connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
            }
        }

        // Device list
        deviceAdapter = DeviceAdapter(
            onAction = { device -> onDeviceAction(device) },
            onLongPress = { device -> onDeviceLongPress(device) }
        )
        deviceList.layoutManager = LinearLayoutManager(requireContext())
        deviceList.adapter = deviceAdapter

        // Manual entry
        btnManualEntry.setOnClickListener { showManualEntryDialog() }

        // Initial state
        updateConnectionUI(connectionManager.state)
        updateControlsEnabled(connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED)
        updatePresetChips()

        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            val tvState = connectionManager.tvState
            updateNowPlaying(tvState.nowPlayingTitle, tvState.nowPlayingPlaylist)
        }

        loadPairedDevices()
    }

    override fun onResume() {
        super.onResume()
        connectionManager.addListener(eventListener)
        updateConnectionUI(connectionManager.state)
        updatePresetChips()
        startDiscovery()
    }

    override fun onPause() {
        super.onPause()
        connectionManager.removeListener(eventListener)
        stopDiscovery()
    }

    private fun updateConnectionUI(state: TvConnectionManager.ConnectionState) {
        val dotBg = connectionDot.background as? GradientDrawable ?: return
        when (state) {
            TvConnectionManager.ConnectionState.CONNECTED -> {
                dotBg.setColor(ContextCompat.getColor(requireContext(), R.color.connected_green))
                connectionStatus.text = connectionManager.tvState.deviceName.ifEmpty { "Connected" }
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.GONE
            }
            TvConnectionManager.ConnectionState.RECONNECTING -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Reconnecting..."
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.VISIBLE
            }
            TvConnectionManager.ConnectionState.CONNECTING,
            TvConnectionManager.ConnectionState.AUTHENTICATING -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Connecting..."
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.GONE
            }
            TvConnectionManager.ConnectionState.DISCONNECTED -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Disconnected"
                btnReconnect.visibility = if (deviceStore.getLastConnectedDevice() != null) View.VISIBLE else View.GONE
                reconnectingProgress.visibility = View.GONE
                updateNowPlaying("", "")
            }
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val ta = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0x9E9E9E.toInt())
        ta.recycle()
        return color
    }

    private fun updateControlsEnabled(enabled: Boolean) {
        btnSkipPrev.isEnabled = enabled
        btnRewind.isEnabled = enabled
        btnPlayPause.isEnabled = enabled
        btnForward.isEnabled = enabled
        btnSkipNext.isEnabled = enabled
    }

    private fun updateNowPlaying(title: String, playlist: String) {
        if (title.isNotEmpty()) {
            nowPlayingTitle.text = title
            if (playlist.isNotEmpty()) {
                nowPlayingPlaylist.text = playlist
                nowPlayingPlaylist.visibility = View.VISIBLE
            } else {
                nowPlayingPlaylist.visibility = View.GONE
            }
        } else {
            nowPlayingTitle.text = "Not playing"
            nowPlayingPlaylist.visibility = View.GONE
        }
    }

    private fun updatePresetChips() {
        presetChips.removeAllViews()
        val cfg = configStore.config
        cfg.player.presets.forEachIndexed { index, preset ->
            val chip = Chip(requireContext()).apply {
                text = preset.name.ifEmpty { "Preset ${index + 1}" }
                isCheckable = true
                isChecked = index == cfg.player.activePreset
                chipBackgroundColor = resources.getColorStateList(R.color.chip_bg, requireContext().theme)
                setTextColor(resources.getColorStateList(R.color.chip_text, requireContext().theme))
                setOnClickListener {
                    configStore.setActivePreset(index)
                    if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
                        connectionManager.sendPlay(index)
                    }
                    updatePresetChips()
                }
            }
            presetChips.addView(chip)
        }
    }

    // --- Discovery ---

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
        refreshDeviceList()
    }

    private fun startDiscovery() {
        if (discovering) return
        nsdManager = requireContext().getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        scanningProgress.visibility = View.VISIBLE
        emptyDeviceState.visibility = View.GONE

        try {
            nsdManager?.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            discovering = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start NSD discovery", e)
        }

        // Start BLE scan with permission check
        requestBlePermissionsAndScan()

        handler.postDelayed({
            if (discovering && discoveredDevices.isEmpty()) {
                emptyDeviceState.visibility = View.VISIBLE
            }
            stopDiscovery()
        }, SCAN_TIMEOUT_MS)
    }

    private fun stopDiscovery() {
        if (!discovering) return
        try {
            nsdManager?.stopServiceDiscovery(discoveryListener)
        } catch (e: Exception) { /* ignore */ }
        bleScanner.stopScan()
        discovering = false
        scanningProgress.visibility = View.GONE
    }

    private fun requestBlePermissionsAndScan() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startBleScan()
        } else {
            blePermissionLauncher.launch(permissions)
        }
    }

    private fun startBleScan() {
        bleScanner.listener = object : BleScanner.Listener {
            override fun onDeviceFound(bleDevice: BleScanner.BleDevice) {
                handler.post {
                    // De-duplicate: if same deviceId already found via NSD, add BLE info but prefer NSD
                    val existing = discoveredDevices[bleDevice.deviceId]
                    if (existing != null && existing.transportType == DeviceAdapter.TransportType.NSD) {
                        // Already discovered via NSD, skip BLE duplicate
                        return@post
                    }

                    val paired = deviceStore.getPairedDevices().find { it.deviceId == bleDevice.deviceId }
                    discoveredDevices[bleDevice.deviceId] = DeviceAdapter.DeviceItem(
                        deviceId = bleDevice.deviceId,
                        deviceName = bleDevice.deviceName,
                        host = "",
                        port = 0,
                        isPaired = paired != null,
                        storedToken = paired?.token,
                        transportType = DeviceAdapter.TransportType.BLE,
                        bleDevice = bleDevice.device
                    )
                    refreshDeviceList()
                    emptyDeviceState.visibility = View.GONE
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
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val host = serviceInfo.host?.hostAddress ?: return
                    val port = serviceInfo.port
                    val deviceId = getAttributeString(serviceInfo, ProtocolKeys.DEVICE_ID) ?: serviceInfo.serviceName
                    val name = getAttributeString(serviceInfo, "name") ?: serviceInfo.serviceName

                    val paired = deviceStore.getPairedDevices().find { it.deviceId == deviceId }
                    val item = DeviceAdapter.DeviceItem(
                        deviceId = deviceId, deviceName = name, host = host, port = port,
                        isPaired = paired != null, storedToken = paired?.token
                    )
                    handler.post {
                        discoveredDevices[deviceId] = item
                        refreshDeviceList()
                        emptyDeviceState.visibility = View.GONE
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
        } catch (e: Exception) { null }
    }

    private fun refreshDeviceList() {
        val connectedId = if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED)
            connectionManager.tvState.deviceId else null
        deviceAdapter.setConnectedDeviceId(connectedId)
        val items = discoveredDevices.values.sortedWith(
            compareByDescending<DeviceAdapter.DeviceItem> { it.isPaired }.thenBy { it.deviceName }
        )
        deviceAdapter.setItems(items)
    }

    private fun onDeviceAction(device: DeviceAdapter.DeviceItem) {
        if (device.isPaired && device.storedToken != null) {
            if (device.transportType == DeviceAdapter.TransportType.BLE && device.bleDevice != null) {
                connectionManager.connectBle(device.bleDevice, device.storedToken)
            } else {
                connectionManager.connect(device.host, device.port, device.storedToken)
            }
            deviceStore.updateLastConnected(device.deviceId)
        } else {
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
                refreshDeviceList()
            }
            .show()
    }

    private fun startPairing(device: DeviceAdapter.DeviceItem) {
        if (device.transportType == DeviceAdapter.TransportType.BLE && device.bleDevice != null) {
            connectionManager.connectBleForPairing(device.bleDevice)
        } else {
            connectionManager.connectForPairing(device.host, device.port)
        }

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

        handler.postDelayed({ connectionManager.sendPairRequest() }, 500)

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
                    val tvState = connectionManager.tvState
                    val token = connectionManager.lastPairedToken ?: ""
                    if (tvState.deviceId.isNotEmpty()) {
                        deviceStore.addDevice(DeviceStore.PairedDevice(
                            deviceId = tvState.deviceId, deviceName = tvState.deviceName,
                            token = token, host = device.host, port = device.port,
                            lastConnected = System.currentTimeMillis()
                        ))
                        handler.post {
                            discoveredDevices[tvState.deviceId] = DeviceAdapter.DeviceItem(
                                deviceId = tvState.deviceId, deviceName = tvState.deviceName,
                                host = device.host, port = device.port,
                                isPaired = true, storedToken = token
                            )
                            refreshDeviceList()
                            dialog.dismiss()
                            connectionManager.removeListener(this)
                        }
                    }
                } else if (state == TvConnectionManager.ConnectionState.DISCONNECTED) {
                    handler.removeCallbacks(timeoutRunnable)
                    handler.post {
                        pairError.text = "Connection lost"
                        pairError.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTrackChanged(title: String, playlist: String) {}
            override fun onPlaybackStateChanged(playing: Boolean) {}
            override fun onConfigApplied(version: Int) {}
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
                        deviceId = "manual_$ip", deviceName = ip,
                        host = ip, port = ProtocolConfig.DEFAULT_PORT, isPaired = false
                    )
                    startPairing(manualDevice)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
