package com.mantle.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.firetv.protocol.ProtocolConfig
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class TvFragment : Fragment() {

    companion object {
        private const val TAG = "TvFragment"
    }

    private val viewModel: TvViewModel by viewModels()

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
    private lateinit var deviceAdapter: DeviceAdapter

    // Pairing dialog (held for dismiss)
    private var pairingDialog: AlertDialog? = null

    // BLE permission launcher (must live in Fragment)
    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startDiscovery()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_tv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindViews(view)
        setupClickListeners()

        deviceAdapter = DeviceAdapter(
            onAction = { device -> viewModel.connectDevice(device) },
            onLongPress = { device -> onDeviceLongPress(device) }
        )
        deviceList.layoutManager = LinearLayoutManager(requireContext())
        deviceList.adapter = deviceAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state -> render(state) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pairingManager.pairingState.collect { state -> handlePairingState(state) }
        }
    }

    override fun onResume() {
        super.onResume()
        requestBlePermissionsAndStartDiscovery()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopDiscovery()
    }

    // --- Rendering ---

    private fun render(state: TvUiState) {
        renderConnectionStatus(state)
        renderNowPlaying(state)
        renderPlaybackControls(state)
        renderPresetChips(state)
        renderDeviceList(state)
    }

    private fun renderConnectionStatus(state: TvUiState) {
        val dotBg = connectionDot.background as? GradientDrawable ?: return
        when (state.connectionState) {
            TvConnectionManager.ConnectionState.CONNECTED -> {
                dotBg.setColor(ContextCompat.getColor(requireContext(), R.color.connected_green))
                connectionStatus.text = state.deviceName ?: "Connected"
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
                btnReconnect.visibility = if (state.devices.any { it.isPaired }) View.VISIBLE else View.GONE
                reconnectingProgress.visibility = View.GONE
            }
        }
    }

    private fun renderNowPlaying(state: TvUiState) {
        val title = state.nowPlayingTitle
        val playlist = state.nowPlayingPlaylist
        if (title != null) {
            nowPlayingTitle.text = title
            if (playlist != null) {
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

    private fun renderPlaybackControls(state: TvUiState) {
        val enabled = state.connectionState == TvConnectionManager.ConnectionState.CONNECTED
        btnSkipPrev.isEnabled = enabled
        btnRewind.isEnabled = enabled
        btnPlayPause.isEnabled = enabled
        btnForward.isEnabled = enabled
        btnSkipNext.isEnabled = enabled
        btnPlayPause.setIconResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
    }

    private fun renderPresetChips(state: TvUiState) {
        presetChips.removeAllViews()
        state.presets.forEachIndexed { index, preset ->
            val chip = Chip(requireContext()).apply {
                text = preset.name.ifEmpty { "Preset ${index + 1}" }
                isCheckable = true
                isChecked = index == state.activePreset
                chipBackgroundColor = resources.getColorStateList(R.color.chip_bg, requireContext().theme)
                setTextColor(resources.getColorStateList(R.color.chip_text, requireContext().theme))
                setOnClickListener { viewModel.selectPreset(index) }
            }
            presetChips.addView(chip)
        }
    }

    private fun renderDeviceList(state: TvUiState) {
        deviceAdapter.setConnectedDeviceId(state.connectedDeviceId)
        deviceAdapter.setItems(state.devices)
        scanningProgress.visibility = if (state.isScanning) View.VISIBLE else View.GONE
        emptyDeviceState.visibility = if (!state.isScanning && state.devices.isEmpty()) View.VISIBLE else View.GONE
    }

    // --- Pairing ---

    private fun handlePairingState(state: PairingState) {
        when (state) {
            is PairingState.Idle -> {
                pairingDialog?.dismiss()
                pairingDialog = null
            }
            is PairingState.AwaitingPin -> {
                if (pairingDialog == null) showPairingDialog(state.deviceName)
            }
            is PairingState.Confirming -> {
                // Dialog stays open while confirming
            }
            is PairingState.Paired -> {
                pairingDialog?.dismiss()
                pairingDialog = null
                viewModel.discoveryManager.refreshPairedDevices()
            }
            is PairingState.Failed -> {
                showPairingError(state.reason)
            }
            is PairingState.TimedOut -> {
                showPairingError("Could not reach TV")
            }
        }
    }

    private fun showPairingDialog(deviceName: String) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pair, null)
        val pinInput = dialogView.findViewById<TextInputEditText>(R.id.pinInput)
        val pairError = dialogView.findViewById<TextView>(R.id.pairError)
        dialogView.findViewById<TextView>(R.id.pairTitle).text = "Pairing with $deviceName"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Confirm", null)
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.cancelPairing()
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text?.toString() ?: ""
                if (pin.length == 4) {
                    pairError.visibility = View.GONE
                    viewModel.confirmPin(pin)
                } else {
                    pairError.text = "Enter a 4-digit PIN"
                    pairError.visibility = View.VISIBLE
                }
            }
        }

        dialog.setOnDismissListener {
            if (pairingDialog === dialog) {
                pairingDialog = null
                viewModel.cancelPairing()
            }
        }

        pairingDialog = dialog
        dialog.show()
    }

    private fun showPairingError(reason: String) {
        val dialog = pairingDialog ?: return
        val pairError = dialog.findViewById<TextView>(R.id.pairError) ?: return
        pairError.text = reason
        pairError.visibility = View.VISIBLE
    }

    // --- Device actions ---

    private fun onDeviceLongPress(device: DeviceItem) {
        if (!device.isPaired) return
        AlertDialog.Builder(requireContext())
            .setTitle(device.deviceName)
            .setItems(arrayOf("Remove pairing")) { _, _ ->
                viewModel.removeDevice(device.deviceId)
            }
            .show()
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
                    val manualDevice = DeviceItem(
                        deviceId = "manual_$ip", deviceName = ip,
                        host = ip, port = ProtocolConfig.DEFAULT_PORT, isPaired = false
                    )
                    viewModel.startPairing(manualDevice)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- Helpers ---

    private fun bindViews(view: View) {
        connectionDot = view.findViewById(R.id.connectionDot)
        connectionStatus = view.findViewById(R.id.connectionStatus)
        btnReconnect = view.findViewById(R.id.btnReconnect)
        reconnectingProgress = view.findViewById(R.id.reconnectingProgress)
        nowPlayingTitle = view.findViewById(R.id.nowPlayingTitle)
        nowPlayingPlaylist = view.findViewById(R.id.nowPlayingPlaylist)
        btnSkipPrev = view.findViewById(R.id.btnSkipPrev)
        btnRewind = view.findViewById(R.id.btnRewind)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnForward = view.findViewById(R.id.btnForward)
        btnSkipNext = view.findViewById(R.id.btnSkipNext)
        presetChips = view.findViewById(R.id.presetChips)
        deviceList = view.findViewById(R.id.deviceList)
        scanningProgress = view.findViewById(R.id.scanningProgress)
        emptyDeviceState = view.findViewById(R.id.emptyDeviceState)
        btnManualEntry = view.findViewById(R.id.btnManualEntry)

        val dotBg = GradientDrawable()
        dotBg.shape = GradientDrawable.OVAL
        dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
        connectionDot.background = dotBg
    }

    private fun setupClickListeners() {
        btnSkipPrev.setOnClickListener { viewModel.skipPrevious() }
        btnRewind.setOnClickListener { viewModel.seekBackward() }
        btnPlayPause.setOnClickListener { viewModel.togglePlayPause() }
        btnForward.setOnClickListener { viewModel.seekForward() }
        btnSkipNext.setOnClickListener { viewModel.skipNext() }
        btnReconnect.setOnClickListener { viewModel.reconnect() }
        btnManualEntry.setOnClickListener { showManualEntryDialog() }
    }

    private fun requestBlePermissionsAndStartDiscovery() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.startDiscovery()
        } else {
            blePermissionLauncher.launch(permissions)
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val ta = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0x9E9E9E.toInt())
        ta.recycle()
        return color
    }
}
