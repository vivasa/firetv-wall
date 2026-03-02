package com.clock.firetv.companion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.util.TimeZone

class SettingsFragment : Fragment() {

    private lateinit var notConnectedHint: TextView
    private lateinit var settingsControls: LinearLayout
    private lateinit var dropdownTheme: MaterialAutoCompleteTextView
    private lateinit var switchWallpaper: MaterialSwitch
    private lateinit var layoutWallpaperInterval: TextInputLayout
    private lateinit var dropdownWallpaperInterval: MaterialAutoCompleteTextView
    private lateinit var switchNightDim: MaterialSwitch
    private lateinit var dropdownTimeFormat: MaterialAutoCompleteTextView
    private lateinit var tvPrimaryTimezone: TextView
    private lateinit var tvSecondaryTimezone: TextView
    private lateinit var switchDrift: MaterialSwitch
    private lateinit var switchChime: MaterialSwitch
    private lateinit var switchPlayerVisible: MaterialSwitch
    private lateinit var dropdownPlayerSize: MaterialAutoCompleteTextView

    private val connectionManager get() = CompanionApp.instance.connectionManager

    private var suppressSends = false

    private val themeOptions = arrayOf("Classic", "Gallery", "Retro")
    private val timeFormatOptions = arrayOf("12-hour", "24-hour")
    private val wallpaperIntervalOptions = arrayOf("1", "2", "5", "10", "15", "30", "60")
    private val playerSizeOptions = arrayOf("Small", "Medium", "Large")

    private val eventListener = object : TvConnectionManager.EventListener {
        override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
            updateVisibility(state)
            if (state == TvConnectionManager.ConnectionState.CONNECTED) {
                populateFromState(connectionManager.tvState)
            }
        }

        override fun onStateReceived(tvState: TvConnectionManager.TvState) {
            populateFromState(tvState)
        }

        override fun onTrackChanged(title: String, playlist: String) {}
        override fun onPlaybackStateChanged(playing: Boolean) {}

        override fun onSettingChanged(key: String, value: Any) {
            applySettingToUI(key, value)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        notConnectedHint = view.findViewById(R.id.notConnectedHint)
        settingsControls = view.findViewById(R.id.settingsControls)
        dropdownTheme = view.findViewById(R.id.dropdownTheme)
        switchWallpaper = view.findViewById(R.id.switchWallpaper)
        layoutWallpaperInterval = view.findViewById(R.id.layoutWallpaperInterval)
        dropdownWallpaperInterval = view.findViewById(R.id.dropdownWallpaperInterval)
        switchNightDim = view.findViewById(R.id.switchNightDim)
        dropdownTimeFormat = view.findViewById(R.id.dropdownTimeFormat)
        tvPrimaryTimezone = view.findViewById(R.id.tvPrimaryTimezone)
        tvSecondaryTimezone = view.findViewById(R.id.tvSecondaryTimezone)
        switchDrift = view.findViewById(R.id.switchDrift)
        switchChime = view.findViewById(R.id.switchChime)
        switchPlayerVisible = view.findViewById(R.id.switchPlayerVisible)
        dropdownPlayerSize = view.findViewById(R.id.dropdownPlayerSize)

        setupDropdowns()
        setupSwitches()
        setupTimezonePickers()

        updateVisibility(connectionManager.state)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            populateFromState(connectionManager.tvState)
        }
    }

    override fun onResume() {
        super.onResume()
        connectionManager.addListener(eventListener)
        updateVisibility(connectionManager.state)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            populateFromState(connectionManager.tvState)
        }
    }

    override fun onPause() {
        super.onPause()
        connectionManager.removeListener(eventListener)
    }

    private fun setupDropdowns() {
        dropdownTheme.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, themeOptions))
        dropdownTheme.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSends) connectionManager.sendSet("theme", position)
        }

        dropdownTimeFormat.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timeFormatOptions))
        dropdownTimeFormat.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSends) connectionManager.sendSet("timeFormat", position)
        }

        dropdownWallpaperInterval.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, wallpaperIntervalOptions))
        dropdownWallpaperInterval.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSends) connectionManager.sendSet("wallpaperInterval", wallpaperIntervalOptions[position].toInt())
        }

        dropdownPlayerSize.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, playerSizeOptions))
        dropdownPlayerSize.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSends) connectionManager.sendSet("playerSize", position)
        }
    }

    private fun setupSwitches() {
        switchWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSends) {
                connectionManager.sendSet("wallpaperEnabled", isChecked)
                layoutWallpaperInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        switchNightDim.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSends) connectionManager.sendSet("nightDimEnabled", isChecked)
        }
        switchDrift.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSends) connectionManager.sendSet("driftEnabled", isChecked)
        }
        switchChime.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSends) connectionManager.sendSet("chimeEnabled", isChecked)
        }
        switchPlayerVisible.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSends) connectionManager.sendSet("playerVisible", isChecked)
        }
    }

    private fun setupTimezonePickers() {
        val timezoneIds = TimeZone.getAvailableIDs().sorted().toTypedArray()

        tvPrimaryTimezone.setOnClickListener {
            showTimezonePicker("Primary Timezone", timezoneIds) { tz ->
                tvPrimaryTimezone.text = tz
                connectionManager.sendSet("primaryTimezone", tz)
            }
        }

        tvSecondaryTimezone.setOnClickListener {
            showTimezonePicker("Secondary Timezone", timezoneIds) { tz ->
                tvSecondaryTimezone.text = tz
                connectionManager.sendSet("secondaryTimezone", tz)
            }
        }
    }

    private fun showTimezonePicker(title: String, timezones: Array<String>, onSelected: (String) -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(timezones) { _, which ->
                onSelected(timezones[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateVisibility(state: TvConnectionManager.ConnectionState) {
        if (state == TvConnectionManager.ConnectionState.CONNECTED) {
            notConnectedHint.visibility = View.GONE
            settingsControls.visibility = View.VISIBLE
        } else {
            notConnectedHint.visibility = View.VISIBLE
            settingsControls.visibility = View.GONE
        }
    }

    private fun populateFromState(tvState: TvConnectionManager.TvState) {
        suppressSends = true
        try {
            dropdownTheme.setText(themeOptions[tvState.theme.coerceIn(0, themeOptions.size - 1)], false)
            dropdownTimeFormat.setText(timeFormatOptions[tvState.timeFormat.coerceIn(0, timeFormatOptions.size - 1)], false)

            val intervalIdx = wallpaperIntervalOptions.indexOf(tvState.wallpaperInterval.toString())
            if (intervalIdx >= 0) dropdownWallpaperInterval.setText(wallpaperIntervalOptions[intervalIdx], false)

            dropdownPlayerSize.setText(playerSizeOptions[tvState.playerSize.coerceIn(0, playerSizeOptions.size - 1)], false)

            switchWallpaper.isChecked = tvState.wallpaperEnabled
            layoutWallpaperInterval.visibility = if (tvState.wallpaperEnabled) View.VISIBLE else View.GONE
            switchNightDim.isChecked = tvState.nightDimEnabled
            switchDrift.isChecked = tvState.driftEnabled
            switchChime.isChecked = tvState.chimeEnabled
            switchPlayerVisible.isChecked = tvState.playerVisible

            tvPrimaryTimezone.text = tvState.primaryTimezone.ifEmpty { "Not set" }
            tvSecondaryTimezone.text = tvState.secondaryTimezone.ifEmpty { "Not set" }
        } finally {
            suppressSends = false
        }
    }

    private fun applySettingToUI(key: String, value: Any) {
        suppressSends = true
        try {
            when (key) {
                "theme" -> dropdownTheme.setText(themeOptions[(value as Number).toInt().coerceIn(0, themeOptions.size - 1)], false)
                "timeFormat" -> dropdownTimeFormat.setText(timeFormatOptions[(value as Number).toInt().coerceIn(0, timeFormatOptions.size - 1)], false)
                "wallpaperEnabled" -> {
                    switchWallpaper.isChecked = value as Boolean
                    layoutWallpaperInterval.visibility = if (value) View.VISIBLE else View.GONE
                }
                "wallpaperInterval" -> {
                    val idx = wallpaperIntervalOptions.indexOf((value as Number).toInt().toString())
                    if (idx >= 0) dropdownWallpaperInterval.setText(wallpaperIntervalOptions[idx], false)
                }
                "nightDimEnabled" -> switchNightDim.isChecked = value as Boolean
                "driftEnabled" -> switchDrift.isChecked = value as Boolean
                "chimeEnabled" -> switchChime.isChecked = value as Boolean
                "playerVisible" -> switchPlayerVisible.isChecked = value as Boolean
                "playerSize" -> dropdownPlayerSize.setText(playerSizeOptions[(value as Number).toInt().coerceIn(0, playerSizeOptions.size - 1)], false)
                "primaryTimezone" -> tvPrimaryTimezone.text = value.toString().ifEmpty { "Not set" }
                "secondaryTimezone" -> tvSecondaryTimezone.text = value.toString().ifEmpty { "Not set" }
            }
        } finally {
            suppressSends = false
        }
    }
}
