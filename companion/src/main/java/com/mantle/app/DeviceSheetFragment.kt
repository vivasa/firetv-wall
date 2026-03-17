package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class DeviceSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_device_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceListContainer: LinearLayout = view.findViewById(R.id.deviceListContainer)
        val btnAddDevice: TextView = view.findViewById(R.id.btnSheetAddDevice)

        btnAddDevice.setOnClickListener {
            dismiss()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, OnboardingFragment())
                .addToBackStack(null)
                .commit()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                renderDevices(deviceListContainer, state)
            }
        }
    }

    private fun renderDevices(container: LinearLayout, state: PlayerUiState) {
        container.removeAllViews()
        val pairedDevices = MantleApp.instance.deviceStore.getPairedDevices()

        pairedDevices.forEach { device ->
            val row = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, container, false)
            val isConnected = state.connectedDeviceId == device.deviceId

            row.findViewById<TextView>(android.R.id.text1).apply {
                text = device.deviceName
                setTextColor(resources.getColor(
                    if (isConnected) R.color.mantle_accent else R.color.mantle_on_surface, null
                ))
            }

            row.findViewById<TextView>(android.R.id.text2).apply {
                if (isConnected) {
                    val activeIdx = state.activePreset
                    val activeName = state.allPlaylists.getOrNull(activeIdx)?.name
                    text = if (activeName != null) "Connected · Playing $activeName" else "Connected"
                    setTextColor(resources.getColor(R.color.mantle_accent, null))
                } else if (device.lastPresetName != null) {
                    text = device.lastPresetName
                    setTextColor(resources.getColor(R.color.mantle_on_surface_muted, null))
                } else if (device.lastConnected > 0) {
                    text = "Last: ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(device.lastConnected))}"
                    setTextColor(resources.getColor(R.color.mantle_on_surface_muted, null))
                } else {
                    text = "Not connected"
                    setTextColor(resources.getColor(R.color.mantle_on_surface_muted, null))
                }
            }

            if (!isConnected) {
                row.setOnClickListener {
                    // Find matching DeviceItem for connection
                    val deviceItem = state.devices.find { it.deviceId == device.deviceId }
                    if (deviceItem != null) {
                        viewModel.connectDevice(deviceItem)
                    } else {
                        // Connect directly using stored info
                        MantleApp.instance.connectionManager.connect(device.host, device.port, device.token)
                        MantleApp.instance.deviceStore.updateLastConnected(device.deviceId)
                    }
                    dismiss()
                }
            }

            container.addView(row)
        }
    }
}
