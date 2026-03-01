package com.clock.firetv

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebView
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

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

    // Settings value views
    private lateinit var valuePrimaryTz: TextView
    private lateinit var valueSecondaryTz: TextView
    private lateinit var valueTimeFormat: TextView
    private lateinit var valueChime: TextView
    private lateinit var valueWallpaper: TextView
    private lateinit var valueWallpaperInterval: TextView
    private lateinit var valueDrift: TextView
    private lateinit var valueNightDim: TextView
    private lateinit var valueYoutubeUrl: EditText
    private lateinit var valuePlayerSize: TextView
    private lateinit var valueShowPlayer: TextView

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

        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)
        loadResourceArrays()
        bindViews()
        initManagers()
        loadSettingsToUI()
        applySettings()
        startEntranceAnimation()

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

        // Settings value views
        valuePrimaryTz = findViewById(R.id.valuePrimaryTz)
        valueSecondaryTz = findViewById(R.id.valueSecondaryTz)
        valueTimeFormat = findViewById(R.id.valueTimeFormat)
        valueChime = findViewById(R.id.valueChime)
        valueWallpaper = findViewById(R.id.valueWallpaper)
        valueWallpaperInterval = findViewById(R.id.valueWallpaperInterval)
        valueDrift = findViewById(R.id.valueDrift)
        valueNightDim = findViewById(R.id.valueNightDim)
        valueYoutubeUrl = findViewById(R.id.valueYoutubeUrl)
        valuePlayerSize = findViewById(R.id.valuePlayerSize)
        valueShowPlayer = findViewById(R.id.valueShowPlayer)

        // Collect settings items for D-pad navigation
        settingsItems = listOf(
            findViewById(R.id.settingPrimaryTz),
            findViewById(R.id.settingSecondaryTz),
            findViewById(R.id.settingTimeFormat),
            findViewById(R.id.settingChime),
            findViewById(R.id.settingWallpaper),
            findViewById(R.id.settingWallpaperInterval),
            findViewById(R.id.settingDrift),
            findViewById(R.id.settingNightDim),
            findViewById(R.id.settingYoutubeUrl),
            findViewById(R.id.settingPlayerSize),
            findViewById(R.id.settingShowPlayer)
        )
    }

    private fun initManagers() {
        val wallpaperFront = findViewById<android.widget.ImageView>(R.id.wallpaperFront)
        val wallpaperBack = findViewById<android.widget.ImageView>(R.id.wallpaperBack)
        val webView = findViewById<WebView>(R.id.youtubeWebView)

        driftAnimator = DriftAnimator(clockContainer)
        wallpaperMgr = WallpaperManager(wallpaperFront, wallpaperBack, scope)
        chimeMgr = ChimeManager(chimeIndicator, chimeDot)
        youtubeMgr = YouTubePlayerManager(webView, youtubeContainer)
        youtubeMgr.initialize()
    }

    private fun applySettings() {
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

        // YouTube
        val dims = settings.getPlayerDimensions()
        youtubeMgr.updateSize(dims.first, dims.second)

        if (settings.playerVisible && settings.youtubeUrl.isNotBlank()) {
            youtubeContainer.visibility = View.VISIBLE
            youtubeMgr.loadVideo(settings.youtubeUrl)
        } else {
            youtubeContainer.visibility = View.GONE
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

        // Timezone label with abbreviation
        val abbrev = tz.getDisplayName(tz.inDaylightTime(now), TimeZone.SHORT, Locale.US)
        val idx = timezoneIds.indexOf(timezoneId)
        val friendlyName = if (idx >= 0) timezoneLabels[idx] else timezoneId
        labelView.text = "$friendlyName ($abbrev)".uppercase()

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

    // ---- Settings Navigation ----

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (settingsVisible) {
            return handleSettingsKey(keyCode)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                showSettings()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() {
        if (settingsVisible) {
            hideSettings()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun showSettings() {
        settingsVisible = true
        settingsFocusIndex = 0
        settingsOverlay.visibility = View.VISIBLE
        settingsOverlay.alpha = 0f
        settingsOverlay.animate().alpha(1f).setDuration(300).start()
        updateSettingsFocus()
    }

    private fun hideSettings() {
        settingsVisible = false

        // Save YouTube URL from EditText
        val newUrl = valueYoutubeUrl.text.toString().trim()
        if (newUrl != settings.youtubeUrl) {
            settings.youtubeUrl = newUrl
        }

        settingsOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                settingsOverlay.visibility = View.GONE
            }
            .start()

        // Re-apply settings in case anything changed
        applySettings()
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
            0 -> { // Primary timezone
                val idx = (timezoneIds.indexOf(settings.primaryTimezone) + direction)
                    .coerceIn(0, timezoneIds.size - 1)
                settings.primaryTimezone = timezoneIds[idx]
                valuePrimaryTz.text = timezoneLabels[idx]
            }
            1 -> { // Secondary timezone
                val idx = (timezoneIds.indexOf(settings.secondaryTimezone) + direction)
                    .coerceIn(0, timezoneIds.size - 1)
                settings.secondaryTimezone = timezoneIds[idx]
                valueSecondaryTz.text = timezoneLabels[idx]
            }
            2 -> { // Time format
                val fmt = if (direction > 0) SettingsManager.FORMAT_24H else SettingsManager.FORMAT_12H
                settings.timeFormat = fmt
                valueTimeFormat.text = if (fmt == SettingsManager.FORMAT_24H) "24-hour" else "12-hour"
            }
            3 -> { // Chime toggle
                settings.chimeEnabled = !settings.chimeEnabled
                valueChime.text = if (settings.chimeEnabled) "ON" else "OFF"
            }
            4 -> { // Wallpaper toggle
                settings.wallpaperEnabled = !settings.wallpaperEnabled
                valueWallpaper.text = if (settings.wallpaperEnabled) "ON" else "OFF"
            }
            5 -> { // Wallpaper interval
                val currentIdx = wallpaperIntervalValues.indexOf(settings.wallpaperIntervalMinutes)
                val newIdx = (currentIdx + direction).coerceIn(0, wallpaperIntervalValues.size - 1)
                settings.wallpaperIntervalMinutes = wallpaperIntervalValues[newIdx]
                valueWallpaperInterval.text = wallpaperIntervalOptions[newIdx]
                wallpaperMgr.updateInterval(settings.wallpaperIntervalMinutes)
            }
            6 -> { // Drift toggle
                settings.driftEnabled = !settings.driftEnabled
                valueDrift.text = if (settings.driftEnabled) "ON" else "OFF"
            }
            7 -> { // Night dim toggle
                settings.nightDimEnabled = !settings.nightDimEnabled
                valueNightDim.text = if (settings.nightDimEnabled) "ON" else "OFF"
            }
            8 -> { // YouTube URL — focus the EditText for keyboard input
                valueYoutubeUrl.requestFocus()
            }
            9 -> { // Player size
                val newSize = (settings.playerSize + direction).coerceIn(0, 2)
                settings.playerSize = newSize
                valuePlayerSize.text = playerSizeOptions[newSize]
                val dims = settings.getPlayerDimensions()
                youtubeMgr.updateSize(dims.first, dims.second)
            }
            10 -> { // Show player toggle
                settings.playerVisible = !settings.playerVisible
                valueShowPlayer.text = if (settings.playerVisible) "ON" else "OFF"
            }
        }
    }

    private fun toggleOrConfirmSetting() {
        when (settingsFocusIndex) {
            3 -> adjustSettingValue(0) // Chime toggle
            4 -> adjustSettingValue(0) // Wallpaper toggle
            6 -> adjustSettingValue(0) // Drift toggle
            7 -> adjustSettingValue(0) // Night dim toggle
            8 -> valueYoutubeUrl.requestFocus() // Focus URL input
            10 -> adjustSettingValue(0) // Show player toggle
            else -> adjustSettingValue(1) // Cycle forward for dropdowns
        }
    }

    private fun loadSettingsToUI() {
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
        valueYoutubeUrl.setText(settings.youtubeUrl)
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
        handler.removeCallbacksAndMessages(null)
        dimAnimHandler.removeCallbacksAndMessages(null)
        driftAnimator.stop()
        wallpaperMgr.stop()
        chimeMgr.stop()
        youtubeMgr.destroy()
        scope.cancel()
    }
}
