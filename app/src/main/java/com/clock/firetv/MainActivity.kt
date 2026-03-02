package com.clock.firetv

import android.graphics.Outline
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import coil.load
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity(), YouTubePlayerManager.OnTrackChangeListener {

    private lateinit var settings: SettingsManager

    // Clock views
    private lateinit var primaryTime: TextView
    private lateinit var primarySeconds: TextView
    private lateinit var primaryAmPm: TextView
    private lateinit var primaryLabel: TextView
    private lateinit var primaryDate: TextView
    private lateinit var secondaryTime: TextView
    private lateinit var secondarySeconds: TextView
    private lateinit var secondaryAmPm: TextView
    private lateinit var secondaryLabel: TextView
    private lateinit var secondaryDate: TextView

    // Containers
    private lateinit var clockContainer: LinearLayout
    private lateinit var youtubeContainer: FrameLayout
    private lateinit var nightDimOverlay: View
    private lateinit var settingsOverlay: FrameLayout
    private lateinit var chimeIndicator: LinearLayout
    private lateinit var chimeDot: View

    // Managers
    private lateinit var driftAnimator: DriftAnimator
    private lateinit var wallpaperMgr: WallpaperManager
    private lateinit var chimeMgr: ChimeManager
    private lateinit var youtubeMgr: YouTubePlayerManager

    // Settings UI
    private lateinit var settingsItems: List<View>
    private var settingsFocusIndex = 0
    private var settingsVisible = false

    // Settings snapshot for detecting player-relevant changes
    private var snapshotTheme = 0
    private var snapshotActivePreset = -1
    private var snapshotPlayerVisible = false
    private var snapshotPlayerSize = 0

    // Now-playing label
    private lateinit var nowPlayingLabel: TextView

    // Companion server
    private var companionServer: CompanionServer? = null

    // Transport controls
    private lateinit var transportControls: LinearLayout
    private lateinit var transportButtons: List<ImageButton>
    private var transportControlsVisible = false
    private var transportFocusIndex = 0
    private val transportAutoHideHandler = Handler(Looper.getMainLooper())
    private val transportAutoHideRunnable = Runnable { hideTransportControls() }

    // Settings value views
    private lateinit var valuePrimaryTz: TextView
    private lateinit var valueSecondaryTz: TextView
    private lateinit var valueTimeFormat: TextView
    private lateinit var valueChime: TextView
    private lateinit var valueWallpaper: TextView
    private lateinit var valueWallpaperInterval: TextView
    private lateinit var valueDrift: TextView
    private lateinit var valueNightDim: TextView
    private lateinit var valueActivePreset: TextView
    private lateinit var valuePlayerSize: TextView
    private lateinit var valueTheme: TextView
    private lateinit var valueShowPlayer: TextView
    private lateinit var qrCodeImage: ImageView
    private lateinit var companionUrlText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var timezoneIds: Array<String>
    private lateinit var timezoneLabels: Array<String>
    private lateinit var wallpaperIntervalOptions: Array<String>
    private lateinit var wallpaperIntervalValues: IntArray
    private lateinit var playerSizeOptions: Array<String>

    // Night dim state
    private var currentDimAlpha = 0f
    private var targetDimAlpha = 0f
    private val dimAnimHandler = Handler(Looper.getMainLooper())

    // Clock update runnable
    private val clockUpdateRunnable = object : Runnable {
        override fun run() {
            updateClocks()
            updateNightDim()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen immersive
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        )

        settings = SettingsManager(this)

        val layoutRes = if (settings.theme == SettingsManager.THEME_GALLERY)
            R.layout.activity_main_gallery else R.layout.activity_main
        setContentView(layoutRes)
        settings.migrateFromSingleUrl()
        loadResourceArrays()
        bindViews()
        initManagers()
        loadSettingsToUI()
        applySettings()
        startEntranceAnimation()
        startCompanionServer()

        // Start clock updates
        handler.post(clockUpdateRunnable)
    }

    private fun loadResourceArrays() {
        timezoneIds = resources.getStringArray(R.array.timezone_ids)
        timezoneLabels = resources.getStringArray(R.array.timezone_labels)
        wallpaperIntervalOptions = resources.getStringArray(R.array.wallpaper_interval_options)
        wallpaperIntervalValues = resources.getIntArray(R.array.wallpaper_interval_values)
        playerSizeOptions = resources.getStringArray(R.array.player_size_options)
    }

    private fun bindViews() {
        // Clock
        primaryTime = findViewById(R.id.primaryTime)
        primarySeconds = findViewById(R.id.primarySeconds)
        primaryAmPm = findViewById(R.id.primaryAmPm)
        primaryLabel = findViewById(R.id.primaryLabel)
        primaryDate = findViewById(R.id.primaryDate)
        secondaryTime = findViewById(R.id.secondaryTime)
        secondarySeconds = findViewById(R.id.secondarySeconds)
        secondaryAmPm = findViewById(R.id.secondaryAmPm)
        secondaryLabel = findViewById(R.id.secondaryLabel)
        secondaryDate = findViewById(R.id.secondaryDate)

        // Containers
        clockContainer = findViewById(R.id.clockContainer)
        youtubeContainer = findViewById(R.id.youtubeContainer)
        nightDimOverlay = findViewById(R.id.nightDimOverlay)
        settingsOverlay = findViewById(R.id.settingsOverlay)
        chimeIndicator = findViewById(R.id.chimeIndicator)
        chimeDot = findViewById(R.id.chimeDot)

        // Now-playing label
        nowPlayingLabel = findViewById(R.id.nowPlayingLabel)

        // Settings value views
        valueTheme = findViewById(R.id.valueTheme)
        valuePrimaryTz = findViewById(R.id.valuePrimaryTz)
        valueSecondaryTz = findViewById(R.id.valueSecondaryTz)
        valueTimeFormat = findViewById(R.id.valueTimeFormat)
        valueChime = findViewById(R.id.valueChime)
        valueWallpaper = findViewById(R.id.valueWallpaper)
        valueWallpaperInterval = findViewById(R.id.valueWallpaperInterval)
        valueDrift = findViewById(R.id.valueDrift)
        valueNightDim = findViewById(R.id.valueNightDim)
        valueActivePreset = findViewById(R.id.valueActivePreset)
        valuePlayerSize = findViewById(R.id.valuePlayerSize)
        valueShowPlayer = findViewById(R.id.valueShowPlayer)
        qrCodeImage = findViewById(R.id.qrCodeImage)
        companionUrlText = findViewById(R.id.companionUrlText)

        // Transport controls
        transportControls = findViewById(R.id.transportControls)
        transportButtons = listOf(
            findViewById(R.id.btnSkipPrevious),
            findViewById(R.id.btnRewind),
            findViewById(R.id.btnFastForward),
            findViewById(R.id.btnSkipNext)
        )

        // Collect settings items for D-pad navigation
        settingsItems = listOf(
            findViewById(R.id.settingTheme),
            findViewById(R.id.settingPrimaryTz),
            findViewById(R.id.settingSecondaryTz),
            findViewById(R.id.settingTimeFormat),
            findViewById(R.id.settingChime),
            findViewById(R.id.settingWallpaper),
            findViewById(R.id.settingWallpaperInterval),
            findViewById(R.id.settingDrift),
            findViewById(R.id.settingNightDim),
            findViewById(R.id.settingActivePreset),
            findViewById(R.id.settingPlayerSize),
            findViewById(R.id.settingShowPlayer)
        )
    }

    private fun initManagers() {
        // Initialize NewPipeExtractor for YouTube stream resolution
        NewPipe.init(OkHttpDownloader(), Localization("en", "US"), ContentCountry("US"))

        val wallpaperFront = findViewById<android.widget.ImageView>(R.id.wallpaperFront)
        val wallpaperBack = findViewById<android.widget.ImageView>(R.id.wallpaperBack)
        val playerView = findViewById<PlayerView>(R.id.youtubePlayerView)

        driftAnimator = DriftAnimator(clockContainer)
        wallpaperMgr = WallpaperManager(wallpaperFront, wallpaperBack, scope)
        chimeMgr = ChimeManager(chimeIndicator, chimeDot)
        youtubeMgr = YouTubePlayerManager(this, playerView, youtubeContainer, scope)
        youtubeMgr.trackChangeListener = this
        youtubeMgr.initialize()

        // Clip video content to rounded corners
        val cornerRadiusDp = if (settings.theme == SettingsManager.THEME_GALLERY) 4f else 12f
        val cornerRadius = cornerRadiusDp * resources.displayMetrics.density
        youtubeContainer.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, cornerRadius)
            }
        }
        youtubeContainer.clipToOutline = true
    }

    private fun applySettings() {
        applyNonPlayerSettings()
        applyPlayerSettings()
    }

    private fun applyNonPlayerSettings() {
        // Drift
        if (settings.driftEnabled) driftAnimator.start() else driftAnimator.stop()

        // Wallpaper
        if (settings.wallpaperEnabled) {
            wallpaperMgr.start(settings.wallpaperIntervalMinutes)
        } else {
            wallpaperMgr.stop()
        }

        // Chime
        if (settings.chimeEnabled) chimeMgr.scheduleNextChime() else chimeMgr.stop()
    }

    private fun applyPlayerSettings() {
        val dims = settings.getPlayerDimensions()
        youtubeMgr.updateSize(dims.first, dims.second)

        val url = settings.activeYoutubeUrl
        if (settings.playerVisible && url.isNotBlank()) {
            youtubeContainer.visibility = View.VISIBLE
            youtubeMgr.loadVideo(url)
        } else {
            youtubeContainer.visibility = View.GONE
            nowPlayingLabel.visibility = View.GONE
            youtubeMgr.stop()
        }
    }

    private fun updateClocks() {
        val now = Date()
        val is24h = settings.timeFormat == SettingsManager.FORMAT_24H

        updateClock(
            now, settings.primaryTimezone, is24h,
            primaryTime, primarySeconds, primaryAmPm, primaryLabel, primaryDate
        )
        updateClock(
            now, settings.secondaryTimezone, is24h,
            secondaryTime, secondarySeconds, secondaryAmPm, secondaryLabel, secondaryDate
        )
    }

    private fun updateClock(
        now: Date,
        timezoneId: String,
        is24h: Boolean,
        timeView: TextView,
        secondsView: TextView,
        amPmView: TextView,
        labelView: TextView,
        dateView: TextView
    ) {
        val tz = TimeZone.getTimeZone(timezoneId)

        val timePattern = if (is24h) "HH:mm" else "hh:mm"
        val timeFmt = SimpleDateFormat(timePattern, Locale.US).apply { timeZone = tz }
        val secFmt = SimpleDateFormat("ss", Locale.US).apply { timeZone = tz }
        val dateFmt = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).apply { timeZone = tz }

        timeView.text = timeFmt.format(now)
        secondsView.text = secFmt.format(now)

        if (is24h) {
            amPmView.visibility = View.GONE
        } else {
            amPmView.visibility = View.VISIBLE
            val amPmFmt = SimpleDateFormat("a", Locale.US).apply { timeZone = tz }
            amPmView.text = amPmFmt.format(now)
        }

        // Timezone abbreviation only (e.g. EST, IST)
        val abbrev = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT, Locale.US)
        labelView.text = abbrev.uppercase()

        dateView.text = dateFmt.format(now)
    }

    // ---- Night Auto-Dim (uses primary timezone) ----

    private fun updateNightDim() {
        if (!settings.nightDimEnabled) {
            if (currentDimAlpha > 0f) {
                animateDimTo(0f)
            }
            return
        }

        val tz = TimeZone.getTimeZone(settings.primaryTimezone)
        val cal = Calendar.getInstance(tz)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // Night hours: 23:00 to 05:59 in primary timezone
        val isNight = hour >= 23 || hour < 6
        val newTarget = if (isNight) 0.45f else 0f

        if (newTarget != targetDimAlpha) {
            targetDimAlpha = newTarget
            animateDimTo(targetDimAlpha)
        }
    }

    private fun animateDimTo(target: Float) {
        // Gradual 60-second fade
        val steps = 120 // steps over 60 seconds
        val intervalMs = 500L
        val delta = (target - currentDimAlpha) / steps

        dimAnimHandler.removeCallbacksAndMessages(null)

        var step = 0
        val fadeRunnable = object : Runnable {
            override fun run() {
                if (step >= steps) {
                    currentDimAlpha = target
                    nightDimOverlay.alpha = target
                    return
                }
                currentDimAlpha += delta
                nightDimOverlay.alpha = currentDimAlpha.coerceIn(0f, 1f)
                step++
                dimAnimHandler.postDelayed(this, intervalMs)
            }
        }
        dimAnimHandler.post(fadeRunnable)
    }

    // ---- Input Navigation ----

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Settings mode takes priority
            if (settingsVisible) {
                return handleSettingsKey(event.keyCode)
            }
            // Transport controls mode
            if (transportControlsVisible) {
                return handleTransportKey(event.keyCode)
            }
            // Normal mode
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    showSettings()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (youtubeContainer.visibility == View.VISIBLE) {
                        showTransportControls()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    youtubeMgr.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    // Let onBackPressed handle this
                }
            }
        }
        if (!settingsVisible && !transportControlsVisible && event.keyCode != KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (settingsVisible) {
            hideSettings()
        } else if (transportControlsVisible) {
            hideTransportControls()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun showSettings() {
        // Dismiss transport controls if visible
        if (transportControlsVisible) {
            hideTransportControls()
        }

        settingsVisible = true
        settingsFocusIndex = 0

        // Snapshot settings before opening
        snapshotTheme = settings.theme
        snapshotActivePreset = settings.activePreset
        snapshotPlayerVisible = settings.playerVisible
        snapshotPlayerSize = settings.playerSize

        settingsOverlay.visibility = View.VISIBLE
        settingsOverlay.alpha = 0f
        settingsOverlay.animate().alpha(1f).setDuration(300).start()
        updateSettingsFocus()
        loadQrCode()
    }

    private fun hideSettings() {
        settingsVisible = false

        // If theme changed, recreate the Activity with the new layout
        if (settings.theme != snapshotTheme) {
            recreate()
            return
        }

        settingsOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                settingsOverlay.visibility = View.GONE
            }
            .start()

        // Apply non-player settings unconditionally
        applyNonPlayerSettings()

        // Only reload player if player-relevant settings changed
        val presetChanged = settings.activePreset != snapshotActivePreset
        val visibilityChanged = settings.playerVisible != snapshotPlayerVisible
        val sizeChanged = settings.playerSize != snapshotPlayerSize

        if (presetChanged || visibilityChanged) {
            applyPlayerSettings()
        } else if (sizeChanged) {
            val dims = settings.getPlayerDimensions()
            youtubeMgr.updateSize(dims.first, dims.second)
        }
    }

    private fun handleSettingsKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                hideSettings()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (settingsFocusIndex > 0) {
                    settingsFocusIndex--
                    updateSettingsFocus()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (settingsFocusIndex < settingsItems.size - 1) {
                    settingsFocusIndex++
                    updateSettingsFocus()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                adjustSettingValue(-1)
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                adjustSettingValue(1)
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                toggleOrConfirmSetting()
                true
            }
            else -> false
        }
    }

    private fun updateSettingsFocus() {
        settingsItems.forEachIndexed { index, view ->
            if (index == settingsFocusIndex) {
                view.setBackgroundResource(R.drawable.settings_item_focused_bg)
                view.requestFocus()
                // Scroll into view if needed
                view.parent?.let { parent ->
                    if (parent is View) {
                        ((parent as View).parent as? android.widget.ScrollView)?.smoothScrollTo(0, view.top - 40)
                    }
                }
            } else {
                view.setBackgroundResource(R.drawable.settings_item_bg)
            }
        }
    }

    private fun adjustSettingValue(direction: Int) {
        when (settingsFocusIndex) {
            0 -> { // Theme
                val newTheme = if (settings.theme == SettingsManager.THEME_CLASSIC)
                    SettingsManager.THEME_GALLERY else SettingsManager.THEME_CLASSIC
                settings.theme = newTheme
                valueTheme.text = if (newTheme == SettingsManager.THEME_GALLERY) "Gallery" else "Classic"
            }
            1 -> { // Primary timezone
                val idx = (timezoneIds.indexOf(settings.primaryTimezone) + direction)
                    .coerceIn(0, timezoneIds.size - 1)
                settings.primaryTimezone = timezoneIds[idx]
                valuePrimaryTz.text = timezoneLabels[idx]
            }
            2 -> { // Secondary timezone
                val idx = (timezoneIds.indexOf(settings.secondaryTimezone) + direction)
                    .coerceIn(0, timezoneIds.size - 1)
                settings.secondaryTimezone = timezoneIds[idx]
                valueSecondaryTz.text = timezoneLabels[idx]
            }
            3 -> { // Time format
                val fmt = if (direction > 0) SettingsManager.FORMAT_24H else SettingsManager.FORMAT_12H
                settings.timeFormat = fmt
                valueTimeFormat.text = if (fmt == SettingsManager.FORMAT_24H) "24-hour" else "12-hour"
            }
            4 -> { // Chime toggle
                settings.chimeEnabled = !settings.chimeEnabled
                valueChime.text = if (settings.chimeEnabled) "ON" else "OFF"
            }
            5 -> { // Wallpaper toggle
                settings.wallpaperEnabled = !settings.wallpaperEnabled
                valueWallpaper.text = if (settings.wallpaperEnabled) "ON" else "OFF"
            }
            6 -> { // Wallpaper interval
                val currentIdx = wallpaperIntervalValues.indexOf(settings.wallpaperIntervalMinutes)
                val newIdx = (currentIdx + direction).coerceIn(0, wallpaperIntervalValues.size - 1)
                settings.wallpaperIntervalMinutes = wallpaperIntervalValues[newIdx]
                valueWallpaperInterval.text = wallpaperIntervalOptions[newIdx]
                wallpaperMgr.updateInterval(settings.wallpaperIntervalMinutes)
            }
            7 -> { // Drift toggle
                settings.driftEnabled = !settings.driftEnabled
                valueDrift.text = if (settings.driftEnabled) "ON" else "OFF"
            }
            8 -> { // Night dim toggle
                settings.nightDimEnabled = !settings.nightDimEnabled
                valueNightDim.text = if (settings.nightDimEnabled) "ON" else "OFF"
            }
            9 -> { // Active preset — cycle through None, Preset 1–4
                val current = settings.activePreset
                // Options: -1 (None), 0, 1, 2, 3
                val newPreset = if (direction > 0) {
                    if (current >= 3) -1 else current + 1
                } else {
                    if (current < 0) 3 else current - 1
                }
                settings.activePreset = newPreset
                valueActivePreset.text = getPresetDisplayLabel(newPreset)
            }
            10 -> { // Player size
                val newSize = (settings.playerSize + direction).coerceIn(0, 2)
                settings.playerSize = newSize
                valuePlayerSize.text = playerSizeOptions[newSize]
                val dims = settings.getPlayerDimensions()
                youtubeMgr.updateSize(dims.first, dims.second)
            }
            11 -> { // Show player toggle
                settings.playerVisible = !settings.playerVisible
                valueShowPlayer.text = if (settings.playerVisible) "ON" else "OFF"
            }
        }
    }

    private fun toggleOrConfirmSetting() {
        when (settingsFocusIndex) {
            0 -> adjustSettingValue(1)  // Theme toggle
            4 -> adjustSettingValue(0)  // Chime toggle
            5 -> adjustSettingValue(0)  // Wallpaper toggle
            7 -> adjustSettingValue(0)  // Drift toggle
            8 -> adjustSettingValue(0)  // Night dim toggle
            9 -> adjustSettingValue(1)  // Cycle preset forward
            11 -> adjustSettingValue(0) // Show player toggle
            else -> adjustSettingValue(1) // Cycle forward for dropdowns
        }
    }

    // ---- Transport Controls ----

    private fun showTransportControls() {
        transportControlsVisible = true
        transportFocusIndex = 1 // Start on rewind button
        transportControls.visibility = View.VISIBLE
        transportControls.animate().alpha(1f).setDuration(200).start()
        updateTransportFocus()
        resetTransportAutoHide()
    }

    private fun hideTransportControls() {
        transportControlsVisible = false
        transportAutoHideHandler.removeCallbacks(transportAutoHideRunnable)
        transportControls.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                transportControls.visibility = View.GONE
            }
            .start()
    }

    private fun resetTransportAutoHide() {
        transportAutoHideHandler.removeCallbacks(transportAutoHideRunnable)
        transportAutoHideHandler.postDelayed(transportAutoHideRunnable, 5000)
    }

    private fun handleTransportKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                hideTransportControls()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (transportFocusIndex > 0) {
                    transportFocusIndex--
                    updateTransportFocus()
                }
                resetTransportAutoHide()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (transportFocusIndex < transportButtons.size - 1) {
                    transportFocusIndex++
                    updateTransportFocus()
                }
                resetTransportAutoHide()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                activateTransportButton()
                resetTransportAutoHide()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                resetTransportAutoHide()
                true
            }
            else -> {
                hideTransportControls()
                true
            }
        }
    }

    private fun updateTransportFocus() {
        transportButtons.forEachIndexed { index, button ->
            if (index == transportFocusIndex) {
                button.setBackgroundResource(R.drawable.transport_button_focused_bg)
            } else {
                button.setBackgroundColor(0x00000000)
            }
        }
    }

    private fun activateTransportButton() {
        when (transportFocusIndex) {
            0 -> youtubeMgr.playPrevious()     // Skip previous
            1 -> youtubeMgr.seekBackward()     // Rewind 10s
            2 -> youtubeMgr.seekForward()      // Fast-forward 10s
            3 -> youtubeMgr.playNext()         // Skip next
        }
    }

    private fun loadSettingsToUI() {
        valueTheme.text = if (settings.theme == SettingsManager.THEME_GALLERY) "Gallery" else "Classic"
        val primaryIdx = timezoneIds.indexOf(settings.primaryTimezone).coerceAtLeast(0)
        val secondaryIdx = timezoneIds.indexOf(settings.secondaryTimezone).coerceAtLeast(0)

        valuePrimaryTz.text = timezoneLabels[primaryIdx]
        valueSecondaryTz.text = timezoneLabels[secondaryIdx]
        valueTimeFormat.text = if (settings.timeFormat == SettingsManager.FORMAT_24H) "24-hour" else "12-hour"
        valueChime.text = if (settings.chimeEnabled) "ON" else "OFF"
        valueWallpaper.text = if (settings.wallpaperEnabled) "ON" else "OFF"

        val intervalIdx = wallpaperIntervalValues.indexOf(settings.wallpaperIntervalMinutes).coerceAtLeast(0)
        valueWallpaperInterval.text = wallpaperIntervalOptions[intervalIdx]

        valueDrift.text = if (settings.driftEnabled) "ON" else "OFF"
        valueNightDim.text = if (settings.nightDimEnabled) "ON" else "OFF"
        valueActivePreset.text = getPresetDisplayLabel(settings.activePreset)
        valuePlayerSize.text = playerSizeOptions[settings.playerSize.coerceIn(0, 2)]
        valueShowPlayer.text = if (settings.playerVisible) "ON" else "OFF"
    }

    // ---- Entrance Animation ----

    private fun startEntranceAnimation() {
        val primaryLayout = findViewById<View>(R.id.primaryClockLayout)
        val secondaryCard = findViewById<View>(R.id.secondaryClockCard)

        // Fade-up entrance
        animateFadeUp(primaryLayout, 0L)
        animateFadeUp(secondaryCard, 200L)
    }

    private fun animateFadeUp(view: View, delay: Long) {
        view.alpha = 0f
        view.translationY = 30f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(delay)
            .start()
    }

    // ---- Track Change Listener ----

    override fun onTrackChanged(videoTitle: String?, playlistTitle: String?) {
        if (videoTitle == null) {
            nowPlayingLabel.visibility = View.GONE
            return
        }
        val text = if (playlistTitle != null) "$videoTitle — $playlistTitle" else videoTitle
        nowPlayingLabel.text = text
        nowPlayingLabel.visibility = if (youtubeContainer.visibility == View.VISIBLE) View.VISIBLE else View.GONE
    }

    // ---- Preset Helpers ----

    private fun getPresetDisplayLabel(index: Int): String {
        if (index < 0 || index >= SettingsManager.PRESET_COUNT) return "None"
        val name = settings.getPresetName(index)
        val defaultName = "Preset ${index + 1}"
        return if (name != defaultName && name.isNotBlank()) {
            "${index + 1}: $name"
        } else {
            defaultName
        }
    }

    // ---- Companion Server ----

    private fun startCompanionServer() {
        val server = CompanionServer(settings, 8080)
        server.presetChangeListener = object : CompanionServer.OnPresetChangeListener {
            override fun onActivePresetChanged() {
                handler.post {
                    valueActivePreset.text = getPresetDisplayLabel(settings.activePreset)
                    applyPlayerSettings()
                }
            }
        }
        try {
            server.start()
            companionServer = server
        } catch (e: Exception) {
            // Port 8080 failed, try 8081
            try {
                val fallback = CompanionServer(settings, 8081)
                fallback.presetChangeListener = server.presetChangeListener
                fallback.start()
                companionServer = fallback
            } catch (e2: Exception) {
                android.util.Log.e("MainActivity", "Failed to start companion server", e2)
            }
        }
    }

    private fun getDeviceIpAddress(): String? {
        return try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as? WifiManager ?: return null
            @Suppress("DEPRECATION")
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip == 0) return null
            "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
        } catch (e: SecurityException) {
            android.util.Log.w("MainActivity", "Cannot get WiFi IP: missing permission", e)
            null
        }
    }

    private fun loadQrCode() {
        val server = companionServer ?: return
        val ip = getDeviceIpAddress() ?: return
        val url = "http://$ip:${server.actualPort}"
        companionUrlText.text = url
        val qrApiUrl = "https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${java.net.URLEncoder.encode(url, "UTF-8")}"
        qrCodeImage.load(qrApiUrl) {
            crossfade(true)
        }
    }

    // ---- Lifecycle ----

    override fun onResume() {
        super.onResume()
        handler.post(clockUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockUpdateRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        companionServer?.stop()
        handler.removeCallbacksAndMessages(null)
        dimAnimHandler.removeCallbacksAndMessages(null)
        transportAutoHideHandler.removeCallbacksAndMessages(null)
        driftAnimator.stop()
        wallpaperMgr.stop()
        chimeMgr.stop()
        youtubeMgr.destroy()
        scope.cancel()
    }
}
