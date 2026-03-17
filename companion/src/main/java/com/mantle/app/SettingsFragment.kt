package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private val viewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack: ImageButton = view.findViewById(R.id.btnSettingsBack)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        setupDisplayCard(view)
        setupAmbianceCard(view)
        setupPlayerCard(view)
        setupDevicesCard(view)
    }

    private fun setupDisplayCard(view: View) {
        val configStore = MantleApp.instance.configStore
        val config = configStore.config

        val themeLabels = arrayOf("Classic", "Minimal", "Retro", "Neon")
        val btnTheme: TextView = view.findViewById(R.id.settingTheme)
        btnTheme.text = themeLabels.getOrElse(config.clock.theme) { "Classic" }
        btnTheme.setOnClickListener {
            val items = themeLabels
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Theme")
                .setItems(items) { _, which ->
                    configStore.setTheme(which)
                    btnTheme.text = items[which]
                }
                .show()
        }

        val timeFormatLabels = arrayOf("12-hour", "24-hour")
        val btnTimeFormat: TextView = view.findViewById(R.id.settingTimeFormat)
        btnTimeFormat.text = timeFormatLabels.getOrElse(config.clock.timeFormat) { "12-hour" }
        btnTimeFormat.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Time Format")
                .setItems(timeFormatLabels) { _, which ->
                    configStore.setTimeFormat(which)
                    btnTimeFormat.text = timeFormatLabels[which]
                }
                .show()
        }

        val btnPrimaryTz: TextView = view.findViewById(R.id.settingPrimaryTz)
        btnPrimaryTz.text = config.clock.primaryTimezone
        btnPrimaryTz.setOnClickListener {
            showTimezoneDialog("Primary Timezone") { tz ->
                configStore.setPrimaryTimezone(tz)
                btnPrimaryTz.text = tz
            }
        }

        val btnSecondaryTz: TextView = view.findViewById(R.id.settingSecondaryTz)
        btnSecondaryTz.text = config.clock.secondaryTimezone
        btnSecondaryTz.setOnClickListener {
            showTimezoneDialog("Secondary Timezone") { tz ->
                configStore.setSecondaryTimezone(tz)
                btnSecondaryTz.text = tz
            }
        }
    }

    private fun setupAmbianceCard(view: View) {
        val configStore = MantleApp.instance.configStore
        val config = configStore.config

        val switchWallpaper: Switch = view.findViewById(R.id.switchWallpaper)
        switchWallpaper.isChecked = config.wallpaper.enabled
        switchWallpaper.setOnCheckedChangeListener { _, isChecked -> configStore.setWallpaperEnabled(isChecked) }

        val btnWallpaperInterval: TextView = view.findViewById(R.id.settingWallpaperInterval)
        btnWallpaperInterval.text = "${config.wallpaper.intervalMinutes} min"
        val intervals = arrayOf("1 min", "5 min", "10 min", "30 min", "60 min")
        val intervalValues = intArrayOf(1, 5, 10, 30, 60)
        btnWallpaperInterval.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Wallpaper Interval")
                .setItems(intervals) { _, which ->
                    configStore.setWallpaperInterval(intervalValues[which])
                    btnWallpaperInterval.text = intervals[which]
                }
                .show()
        }

        val switchNightDim: Switch = view.findViewById(R.id.switchNightDim)
        switchNightDim.isChecked = config.clock.nightDimEnabled
        switchNightDim.setOnCheckedChangeListener { _, isChecked -> configStore.setNightDimEnabled(isChecked) }

        val switchDrift: Switch = view.findViewById(R.id.switchDrift)
        switchDrift.isChecked = config.clock.driftEnabled
        switchDrift.setOnCheckedChangeListener { _, isChecked -> configStore.setDriftEnabled(isChecked) }

        val switchChime: Switch = view.findViewById(R.id.switchChime)
        switchChime.isChecked = config.chime.enabled
        switchChime.setOnCheckedChangeListener { _, isChecked -> configStore.setChimeEnabled(isChecked) }
    }

    private fun setupPlayerCard(view: View) {
        val configStore = MantleApp.instance.configStore
        val config = configStore.config

        val switchPlayer: Switch = view.findViewById(R.id.switchPlayerVisible)
        switchPlayer.isChecked = config.player.visible
        switchPlayer.setOnCheckedChangeListener { _, isChecked -> configStore.setPlayerVisible(isChecked) }

        val sizeLabels = arrayOf("Small", "Medium", "Large")
        val btnPlayerSize: TextView = view.findViewById(R.id.settingPlayerSize)
        btnPlayerSize.text = sizeLabels.getOrElse(config.player.size) { "Medium" }
        btnPlayerSize.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Player Size")
                .setItems(sizeLabels) { _, which ->
                    configStore.setPlayerSize(which)
                    btnPlayerSize.text = sizeLabels[which]
                }
                .show()
        }
    }

    private fun setupDevicesCard(view: View) {
        val deviceStore = MantleApp.instance.deviceStore
        val devicesContainer: ViewGroup = view.findViewById(R.id.devicesContainer)
        val btnAddDevice: View = view.findViewById(R.id.btnAddDevice)
        val btnConnectionLog: View = view.findViewById(R.id.btnConnectionLog)

        // Populate paired devices
        refreshDeviceList(devicesContainer)

        btnAddDevice.setOnClickListener {
            viewModel.startDiscovery()
        }

        btnConnectionLog.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ConnectionDiagnosticsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun refreshDeviceList(container: ViewGroup) {
        container.removeAllViews()
        val devices = MantleApp.instance.deviceStore.getPairedDevices()
        devices.forEach { device ->
            val row = LayoutInflater.from(requireContext()).inflate(android.R.layout.simple_list_item_2, container, false)
            row.findViewById<TextView>(android.R.id.text1).apply {
                text = device.deviceName
                setTextColor(resources.getColor(R.color.mantle_on_surface, null))
            }
            row.findViewById<TextView>(android.R.id.text2).apply {
                text = if (device.lastConnected > 0) {
                    "Last connected: ${java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(device.lastConnected))}"
                } else "Never connected"
                setTextColor(resources.getColor(R.color.mantle_on_surface_muted, null))
            }
            row.setOnLongClickListener {
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Remove ${device.deviceName}?")
                    .setPositiveButton("Remove") { _, _ ->
                        viewModel.removeDevice(device.deviceId)
                        refreshDeviceList(container)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }
            container.addView(row)
        }
    }

    private fun showTimezoneDialog(title: String, onSelected: (String) -> Unit) {
        val timezones = java.util.TimeZone.getAvailableIDs().sorted().toTypedArray()
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(timezones) { _, which -> onSelected(timezones[which]) }
            .show()
    }
}
