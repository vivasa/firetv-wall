package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import java.util.TimeZone

class HomeFragment : Fragment() {

    private lateinit var dropdownTheme: MaterialAutoCompleteTextView
    private lateinit var dropdownTimeFormat: MaterialAutoCompleteTextView
    private lateinit var tvPrimaryTimezone: TextView
    private lateinit var tvSecondaryTimezone: TextView
    private lateinit var switchWallpaper: MaterialSwitch
    private lateinit var layoutWallpaperInterval: TextInputLayout
    private lateinit var dropdownWallpaperInterval: MaterialAutoCompleteTextView
    private lateinit var switchNightDim: MaterialSwitch
    private lateinit var switchDrift: MaterialSwitch
    private lateinit var switchChime: MaterialSwitch
    private lateinit var switchPlayerVisible: MaterialSwitch
    private lateinit var dropdownPlayerSize: MaterialAutoCompleteTextView

    private val configStore get() = MantleApp.instance.configStore

    private var suppressSaves = false

    private val themeOptions = arrayOf("Classic", "Gallery", "Retro")
    private val timeFormatOptions = arrayOf("12-hour", "24-hour")
    private val wallpaperIntervalOptions = arrayOf("1", "2", "5", "10", "15", "30", "60")
    private val playerSizeOptions = arrayOf("Small", "Medium", "Large")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        dropdownTheme = view.findViewById(R.id.dropdownTheme)
        dropdownTimeFormat = view.findViewById(R.id.dropdownTimeFormat)
        tvPrimaryTimezone = view.findViewById(R.id.tvPrimaryTimezone)
        tvSecondaryTimezone = view.findViewById(R.id.tvSecondaryTimezone)
        switchWallpaper = view.findViewById(R.id.switchWallpaper)
        layoutWallpaperInterval = view.findViewById(R.id.layoutWallpaperInterval)
        dropdownWallpaperInterval = view.findViewById(R.id.dropdownWallpaperInterval)
        switchNightDim = view.findViewById(R.id.switchNightDim)
        switchDrift = view.findViewById(R.id.switchDrift)
        switchChime = view.findViewById(R.id.switchChime)
        switchPlayerVisible = view.findViewById(R.id.switchPlayerVisible)
        dropdownPlayerSize = view.findViewById(R.id.dropdownPlayerSize)

        view.findViewById<View>(R.id.btnConnectionLog).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ConnectionDiagnosticsFragment())
                .addToBackStack(null)
                .commit()
        }

        setupDropdowns()
        setupSwitches()
        setupTimezonePickers()
        populateFromConfig()
    }

    private fun setupDropdowns() {
        dropdownTheme.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, themeOptions))
        dropdownTheme.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSaves) configStore.setTheme(position)
        }

        dropdownTimeFormat.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, timeFormatOptions))
        dropdownTimeFormat.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSaves) configStore.setTimeFormat(position)
        }

        dropdownWallpaperInterval.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, wallpaperIntervalOptions))
        dropdownWallpaperInterval.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSaves) configStore.setWallpaperInterval(wallpaperIntervalOptions[position].toInt())
        }

        dropdownPlayerSize.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, playerSizeOptions))
        dropdownPlayerSize.setOnItemClickListener { _, _, position, _ ->
            if (!suppressSaves) configStore.setPlayerSize(position)
        }
    }

    private fun setupSwitches() {
        switchWallpaper.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSaves) {
                configStore.setWallpaperEnabled(isChecked)
                layoutWallpaperInterval.visibility = if (isChecked) View.VISIBLE else View.GONE
            }
        }
        switchNightDim.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSaves) configStore.setNightDimEnabled(isChecked)
        }
        switchDrift.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSaves) configStore.setDriftEnabled(isChecked)
        }
        switchChime.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSaves) configStore.setChimeEnabled(isChecked)
        }
        switchPlayerVisible.setOnCheckedChangeListener { _, isChecked ->
            if (!suppressSaves) configStore.setPlayerVisible(isChecked)
        }
    }

    private fun setupTimezonePickers() {
        val timezoneIds = TimeZone.getAvailableIDs().sorted().toTypedArray()

        tvPrimaryTimezone.setOnClickListener {
            showTimezonePicker("Primary Timezone", timezoneIds) { tz ->
                tvPrimaryTimezone.text = tz
                configStore.setPrimaryTimezone(tz)
            }
        }

        tvSecondaryTimezone.setOnClickListener {
            showTimezonePicker("Secondary Timezone", timezoneIds) { tz ->
                tvSecondaryTimezone.text = tz
                configStore.setSecondaryTimezone(tz)
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

    private fun populateFromConfig() {
        suppressSaves = true
        try {
            val cfg = configStore.config
            dropdownTheme.setText(themeOptions[cfg.clock.theme.coerceIn(0, themeOptions.size - 1)], false)
            dropdownTimeFormat.setText(timeFormatOptions[cfg.clock.timeFormat.coerceIn(0, timeFormatOptions.size - 1)], false)

            tvPrimaryTimezone.text = cfg.clock.primaryTimezone.ifEmpty { "Not set" }
            tvSecondaryTimezone.text = cfg.clock.secondaryTimezone.ifEmpty { "Not set" }

            switchWallpaper.isChecked = cfg.wallpaper.enabled
            layoutWallpaperInterval.visibility = if (cfg.wallpaper.enabled) View.VISIBLE else View.GONE
            val intervalIdx = wallpaperIntervalOptions.indexOf(cfg.wallpaper.intervalMinutes.toString())
            if (intervalIdx >= 0) dropdownWallpaperInterval.setText(wallpaperIntervalOptions[intervalIdx], false)

            switchNightDim.isChecked = cfg.clock.nightDimEnabled
            switchDrift.isChecked = cfg.clock.driftEnabled
            switchChime.isChecked = cfg.chime.enabled
            switchPlayerVisible.isChecked = cfg.player.visible

            dropdownPlayerSize.setText(playerSizeOptions[cfg.player.size.coerceIn(0, playerSizeOptions.size - 1)], false)
        } finally {
            suppressSaves = false
        }
    }
}
