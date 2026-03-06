package com.clock.firetv

import android.graphics.Outline
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.firetv.protocol.ProtocolConfig
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
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
    private lateinit var deviceIdentity: DeviceIdentity

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
    private lateinit var chimeIndicator: LinearLayout
    private lateinit var chimeDot: View

    // Managers
    private lateinit var driftAnimator: DriftAnimator
    private lateinit var wallpaperMgr: WallpaperManager
    private lateinit var chimeMgr: ChimeManager
    private lateinit var youtubeMgr: YouTubePlayerManager
    private lateinit var commandHandler: CompanionCommandHandler
    private var companionWs: CompanionWebSocket? = null
    private var blePeripheral: BlePeripheralManager? = null
    private var nsdRegistration: NsdRegistration? = null

    // Now-playing label
    private lateinit var nowPlayingLabel: TextView

    // Transport controls
    private lateinit var transportControls: LinearLayout
    private lateinit var transportButtons: List<ImageButton>
    private var transportControlsVisible = false
    private var transportFocusIndex = 0
    private val transportAutoHideHandler = Handler(Looper.getMainLooper())
    private val transportAutoHideRunnable = Runnable { hideTransportControls() }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())


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
        deviceIdentity = DeviceIdentity(this)

        val layoutRes = when (settings.theme) {
            SettingsManager.THEME_GALLERY -> R.layout.activity_main_gallery
            SettingsManager.THEME_RETRO -> R.layout.activity_main_retro
            else -> R.layout.activity_main
        }
        setContentView(layoutRes)
        bindViews()
        initManagers()
        youtubeMgr.loadPlaybackState(settings.prefs)
        applySettings()
        startEntranceAnimation()
        startCompanionWebSocket()
        startNsdRegistration()
        startBlePeripheral()

        // Start clock updates
        handler.post(clockUpdateRunnable)
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
        chimeIndicator = findViewById(R.id.chimeIndicator)
        chimeDot = findViewById(R.id.chimeDot)

        // Now-playing label
        nowPlayingLabel = findViewById(R.id.nowPlayingLabel)

        // Transport controls
        transportControls = findViewById(R.id.transportControls)
        transportButtons = listOf(
            findViewById(R.id.btnSkipPrevious),
            findViewById(R.id.btnRewind),
            findViewById(R.id.btnFastForward),
            findViewById(R.id.btnSkipNext)
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
        val cornerRadiusDp = when (settings.theme) {
            SettingsManager.THEME_GALLERY -> 4f
            SettingsManager.THEME_RETRO -> 8f
            else -> 12f
        }
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
            // Transport controls mode
            if (transportControlsVisible) {
                return handleTransportKey(event.keyCode)
            }
            // Normal mode
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
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
        if (!transportControlsVisible && event.keyCode != KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (transportControlsVisible) {
            hideTransportControls()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
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
            commandHandler.broadcastEvent(org.json.JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                put(ProtocolKeys.IS_PLAYING, false)
            })
            return
        }
        val text = if (playlistTitle != null) "$videoTitle — $playlistTitle" else videoTitle
        nowPlayingLabel.text = text
        nowPlayingLabel.visibility = if (youtubeContainer.visibility == View.VISIBLE) View.VISIBLE else View.GONE

        // Broadcast to companion
        commandHandler.broadcastEvent(org.json.JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.TRACK_CHANGED)
            put(ProtocolKeys.TITLE, videoTitle)
            put(ProtocolKeys.PLAYLIST, playlistTitle ?: "")
        })
    }

    private fun startCompanionWebSocket() {
        commandHandler = CompanionCommandHandler(settings, deviceIdentity)
        commandHandler.listener = object : CompanionCommandHandler.Listener {
            override fun onCompanionConnected() {
                showLinkIndicator()
                // Send current track info to newly connected companion
                youtubeMgr.currentTrackInfo()?.let { (title, playlist) ->
                    commandHandler.broadcastEvent(org.json.JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.TRACK_CHANGED)
                        put(ProtocolKeys.TITLE, title)
                        put(ProtocolKeys.PLAYLIST, playlist ?: "")
                    })
                    commandHandler.broadcastEvent(org.json.JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                        put(ProtocolKeys.IS_PLAYING, true)
                    })
                }
            }
            override fun onCompanionDisconnected() {
                hideLinkIndicator()
            }
            override fun onShowPin(pin: String) {
                showPinOverlay(pin)
            }
            override fun onDismissPin() {
                dismissPinOverlay()
            }
            override fun onPlayPreset(index: Int) {
                settings.activePreset = index
                applyPlayerSettings()
            }
            override fun onStopPlayback() {
                settings.activePreset = -1
                applyPlayerSettings()
            }
            override fun onPausePlayback() {
                youtubeMgr.pause()
            }
            override fun onResumePlayback() {
                youtubeMgr.resume()
            }
            override fun onSeek(offsetSec: Int) {
                if (offsetSec > 0) youtubeMgr.seekForward() else youtubeMgr.seekBackward()
            }
            override fun onSkip(direction: Int) {
                if (direction > 0) youtubeMgr.playNext() else youtubeMgr.playPrevious()
            }
            override fun onSyncConfig(config: org.json.JSONObject) {
                applySyncConfig(config)
            }
        }

        val ws = CompanionWebSocket(ProtocolConfig.DEFAULT_PORT)
        ws.transportListener = commandHandler
        try {
            ws.startServer()
            Thread.sleep(200) // NanoHTTPD binds on background thread
            if (ws.isAlive) {
                companionWs = ws
                android.util.Log.e("CompanionWS", "WebSocket server started on port ${ws.actualPort}")
            } else {
                ws.stop()
                android.util.Log.e("CompanionWS", "Port ${ProtocolConfig.DEFAULT_PORT} bind failed, trying ${ProtocolConfig.FALLBACK_PORT}")
                val fallback = CompanionWebSocket(ProtocolConfig.FALLBACK_PORT)
                fallback.transportListener = commandHandler
                fallback.startServer()
                Thread.sleep(200)
                if (fallback.isAlive) {
                    companionWs = fallback
                    android.util.Log.e("CompanionWS", "WebSocket server started on fallback port ${fallback.actualPort}")
                } else {
                    fallback.stop()
                    android.util.Log.e("CompanionWS", "WebSocket server failed to start on both ports")
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("CompanionWS", "WebSocket server startup error", e)
        }
    }

    private fun applySyncConfig(config: org.json.JSONObject) {
        val oldTheme = settings.theme

        // Clock settings
        config.optJSONObject("clock")?.let { clock ->
            settings.theme = clock.optInt(ProtocolKeys.THEME, settings.theme)
            settings.primaryTimezone = clock.optString(ProtocolKeys.PRIMARY_TIMEZONE, settings.primaryTimezone)
            settings.secondaryTimezone = clock.optString(ProtocolKeys.SECONDARY_TIMEZONE, settings.secondaryTimezone)
            settings.timeFormat = clock.optInt(ProtocolKeys.TIME_FORMAT, settings.timeFormat)
            settings.nightDimEnabled = clock.optBoolean(ProtocolKeys.NIGHT_DIM_ENABLED, settings.nightDimEnabled)
            settings.driftEnabled = clock.optBoolean(ProtocolKeys.DRIFT_ENABLED, settings.driftEnabled)
        }

        // Wallpaper settings
        config.optJSONObject("wallpaper")?.let { wallpaper ->
            settings.wallpaperEnabled = wallpaper.optBoolean("enabled", settings.wallpaperEnabled)
            settings.wallpaperIntervalMinutes = wallpaper.optInt("intervalMinutes", settings.wallpaperIntervalMinutes)
        }

        // Chime settings
        config.optJSONObject("chime")?.let { chime ->
            settings.chimeEnabled = chime.optBoolean("enabled", settings.chimeEnabled)
        }

        // Player settings
        config.optJSONObject("player")?.let { player ->
            settings.playerSize = player.optInt("size", settings.playerSize)
            settings.playerVisible = player.optBoolean("visible", settings.playerVisible)
            settings.activePreset = player.optInt(ProtocolKeys.ACTIVE_PRESET, settings.activePreset)

            player.optJSONArray(ProtocolKeys.PRESETS)?.let { presets ->
                settings.setPresetsFromConfig(presets)
            }
        }

        // Apply all changes to display
        if (settings.theme != oldTheme) {
            youtubeMgr.savePlaybackState(settings.prefs)
            recreate()
        } else {
            applyNonPlayerSettings()
            applyPlayerSettings()
            wallpaperMgr.updateInterval(settings.wallpaperIntervalMinutes)
        }
    }

    // ---- PIN Overlay ----

    private fun showPinOverlay(pin: String) {
        // Create a simple centered overlay with the PIN
        val rootLayout = findViewById<FrameLayout>(R.id.rootLayout)
        val existing = rootLayout.findViewWithTag<View>("pinOverlay")
        existing?.let { rootLayout.removeView(it) }

        val overlay = FrameLayout(this).apply {
            tag = "pinOverlay"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xCC000000.toInt())
            alpha = 0f
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER
            )
        }

        val pinText = TextView(this).apply {
            text = pin
            textSize = 64f
            setTextColor(0xFFE8A850.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            gravity = android.view.Gravity.CENTER
            letterSpacing = 0.3f
        }

        val instrText = TextView(this).apply {
            text = "Enter this code on your phone"
            textSize = 16f
            setTextColor(0xAAFFFFFF.toInt())
            gravity = android.view.Gravity.CENTER
            setPadding(0, 24, 0, 0)
        }

        container.addView(pinText)
        container.addView(instrText)
        overlay.addView(container)
        rootLayout.addView(overlay)
        overlay.animate().alpha(1f).setDuration(300).start()
    }

    private fun dismissPinOverlay() {
        val rootLayout = findViewById<FrameLayout>(R.id.rootLayout)
        val overlay = rootLayout.findViewWithTag<View>("pinOverlay") ?: return
        overlay.animate().alpha(0f).setDuration(300).withEndAction {
            rootLayout.removeView(overlay)
        }.start()
    }

    // ---- Link Indicator ----

    private fun showLinkIndicator() {
        val indicator = findViewById<LinearLayout>(R.id.linkIndicator) ?: return
        indicator.visibility = View.VISIBLE
        indicator.animate().alpha(1f).setDuration(300).start()
    }

    private fun hideLinkIndicator() {
        val indicator = findViewById<LinearLayout>(R.id.linkIndicator) ?: return
        handler.postDelayed({
            indicator.animate().alpha(0f).setDuration(300).withEndAction {
                indicator.visibility = View.GONE
            }.start()
        }, 3000)
    }

    private fun startNsdRegistration() {
        val ws = companionWs ?: return
        val nsd = NsdRegistration(this, deviceIdentity)
        nsd.register(ws.actualPort)
        nsdRegistration = nsd
    }

    private fun startBlePeripheral() {
        val ble = BlePeripheralManager(this, deviceIdentity)
        ble.transportListener = commandHandler
        if (ble.init()) {
            blePeripheral = ble
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
        blePeripheral?.destroy()
        nsdRegistration?.unregister()
        companionWs?.stop()
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
