package com.clock.firetv

import android.graphics.Outline
import android.os.Bundle
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
import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class MainActivity : AppCompatActivity(), YouTubePlayerManager.OnTrackChangeListener {

    private lateinit var settings: SettingsManager
    private lateinit var deviceIdentity: DeviceIdentity

    // Views retained for callbacks
    private lateinit var youtubeContainer: FrameLayout
    private lateinit var nowPlayingLabel: TextView

    // Managers
    private lateinit var driftAnimator: DriftAnimator
    private lateinit var wallpaperMgr: WallpaperManager
    private lateinit var chimeMgr: ChimeManager
    private lateinit var youtubeMgr: YouTubePlayerManager
    private lateinit var commandHandler: CompanionCommandHandler

    // Extracted controllers
    private lateinit var clockPresenter: ClockPresenter
    private lateinit var nightDimController: NightDimController
    private lateinit var pinOverlayManager: PinOverlayManager
    private lateinit var transportController: TransportControlsController
    private lateinit var configApplier: ConfigApplier
    private lateinit var companionBridge: CompanionBridge
    private lateinit var serviceCoordinator: ServiceCoordinator

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var clockUpdateJob: Job? = null

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

        bindViewsAndControllers()
        initManagers()
        youtubeMgr.loadPlaybackState(settings.prefs)
        applySettings()
        startEntranceAnimation()
        startServices()

        startClockUpdates()
    }

    private fun bindViewsAndControllers() {
        youtubeContainer = findViewById(R.id.youtubeContainer)
        nowPlayingLabel = findViewById(R.id.nowPlayingLabel)

        clockPresenter = ClockPresenter(
            primaryTime = findViewById(R.id.primaryTime),
            primarySeconds = findViewById(R.id.primarySeconds),
            primaryAmPm = findViewById(R.id.primaryAmPm),
            primaryLabel = findViewById(R.id.primaryLabel),
            primaryDate = findViewById(R.id.primaryDate),
            secondaryTime = findViewById(R.id.secondaryTime),
            secondarySeconds = findViewById(R.id.secondarySeconds),
            secondaryAmPm = findViewById(R.id.secondaryAmPm),
            secondaryLabel = findViewById(R.id.secondaryLabel),
            secondaryDate = findViewById(R.id.secondaryDate)
        )

        nightDimController = NightDimController(findViewById(R.id.nightDimOverlay), scope)
        pinOverlayManager = PinOverlayManager(this, findViewById(R.id.rootLayout))

        transportController = TransportControlsController(
            transportControls = findViewById(R.id.transportControls),
            buttons = listOf(
                findViewById(R.id.btnSkipPrevious),
                findViewById(R.id.btnRewind),
                findViewById(R.id.btnFastForward),
                findViewById(R.id.btnSkipNext)
            ),
            callback = object : TransportControlsController.Callback {
                override fun onSkipBack() = youtubeMgr.playPrevious()
                override fun onRewind() = youtubeMgr.seekBackward()
                override fun onFastForward() = youtubeMgr.seekForward()
                override fun onSkipForward() = youtubeMgr.playNext()
            },
            scope = scope
        )

        configApplier = ConfigApplier()
    }

    private fun initManagers() {
        NewPipe.init(OkHttpDownloader(), Localization("en", "US"), ContentCountry("US"))

        val wallpaperFront = findViewById<android.widget.ImageView>(R.id.wallpaperFront)
        val wallpaperBack = findViewById<android.widget.ImageView>(R.id.wallpaperBack)
        val playerView = findViewById<PlayerView>(R.id.youtubePlayerView)
        val clockContainer = findViewById<LinearLayout>(R.id.clockContainer)
        val chimeIndicator = findViewById<LinearLayout>(R.id.chimeIndicator)
        val chimeDot = findViewById<View>(R.id.chimeDot)

        driftAnimator = DriftAnimator(clockContainer, scope)
        wallpaperMgr = WallpaperManager(wallpaperFront, wallpaperBack, scope)
        chimeMgr = ChimeManager(chimeIndicator, chimeDot, scope)
        youtubeMgr = YouTubePlayerManager(this, playerView, youtubeContainer, scope)
        youtubeMgr.trackChangeListener = this
        youtubeMgr.playlistLoadedListener = object : YouTubePlayerManager.OnPlaylistLoadedListener {
            override fun onPlaylistLoaded() {
                if (::companionBridge.isInitialized && ::commandHandler.isInitialized) {
                    commandHandler.broadcastEvent(companionBridge.buildPlaylistTracksEvent())
                }
            }
        }
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
        configApplier.applyNonPlayerSettings(settings, wallpaperMgr, chimeMgr, driftAnimator)
        configApplier.applyPlayerSettings(settings, youtubeMgr, youtubeContainer, nowPlayingLabel)
    }

    private fun startServices() {
        commandHandler = CompanionCommandHandler(settings, deviceIdentity, scope)

        companionBridge = CompanionBridge(settings, youtubeMgr, commandHandler)
        companionBridge.callback = object : CompanionBridge.Callback {
            override fun onShowPin(pin: String) = pinOverlayManager.showPin(pin)
            override fun onDismissPin() = pinOverlayManager.dismissPin()
            override fun onCompanionConnected() {
                showLinkIndicator()
                youtubeMgr.currentTrackInfo()?.let { (title, playlist) ->
                    commandHandler.broadcastEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.TRACK_CHANGED)
                        put(ProtocolKeys.TITLE, title)
                        put(ProtocolKeys.PLAYLIST, playlist ?: "")
                    })
                    commandHandler.broadcastEvent(JSONObject().apply {
                        put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                        put(ProtocolKeys.IS_PLAYING, true)
                    })
                }
                if (youtubeMgr.hasPlaylist()) {
                    commandHandler.broadcastEvent(companionBridge.buildPlaylistTracksEvent())
                }
            }
            override fun onCompanionDisconnected() = hideLinkIndicator()
            override fun onSyncConfig(config: JSONObject) {
                configApplier.apply(
                    config, settings, wallpaperMgr, chimeMgr, driftAnimator,
                    youtubeMgr, youtubeContainer, nowPlayingLabel
                ) { recreate() }
            }
            override fun onPlayerSettingsChanged() {
                configApplier.applyPlayerSettings(settings, youtubeMgr, youtubeContainer, nowPlayingLabel)
            }
        }
        companionBridge.attach()

        serviceCoordinator = ServiceCoordinator(this, deviceIdentity, commandHandler, scope)
        serviceCoordinator.startAll()
    }

    private fun startClockUpdates() {
        clockUpdateJob?.cancel()
        clockUpdateJob = scope.launch {
            while (isActive) {
                clockPresenter.update(settings.primaryTimezone, settings.secondaryTimezone, settings.timeFormat)
                nightDimController.check(settings.nightDimEnabled, settings.primaryTimezone)
                delay(1000)
            }
        }
    }

    // ---- Entrance Animation ----

    private fun startEntranceAnimation() {
        val primaryLayout = findViewById<View>(R.id.primaryClockLayout)
        val secondaryCard = findViewById<View>(R.id.secondaryClockCard)
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
            if (::commandHandler.isInitialized) {
                commandHandler.broadcastEvent(JSONObject().apply {
                    put(ProtocolKeys.EVT, ProtocolEvents.PLAYBACK_STATE)
                    put(ProtocolKeys.IS_PLAYING, false)
                })
            }
            return
        }
        val text = if (playlistTitle != null) "$videoTitle — $playlistTitle" else videoTitle
        nowPlayingLabel.text = text
        nowPlayingLabel.visibility = if (youtubeContainer.visibility == View.VISIBLE) View.VISIBLE else View.GONE

        if (::commandHandler.isInitialized) {
            commandHandler.broadcastEvent(JSONObject().apply {
                put(ProtocolKeys.EVT, ProtocolEvents.TRACK_CHANGED)
                put(ProtocolKeys.TITLE, videoTitle)
                put(ProtocolKeys.PLAYLIST, playlistTitle ?: "")
            })
        }
    }

    // ---- Input Navigation ----

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (transportController.isVisible) {
                return transportController.handleKeyEvent(event.keyCode)
            }
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (youtubeContainer.visibility == View.VISIBLE) {
                        transportController.show()
                        return true
                    }
                }
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    youtubeMgr.togglePlayPause()
                    return true
                }
                KeyEvent.KEYCODE_BACK -> { }
            }
        }
        if (!transportController.isVisible && event.keyCode != KeyEvent.KEYCODE_BACK) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (transportController.isVisible) {
            transportController.hide()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    // ---- Link Indicator ----

    private fun showLinkIndicator() {
        val indicator = findViewById<LinearLayout>(R.id.linkIndicator) ?: return
        indicator.visibility = View.VISIBLE
        indicator.animate().alpha(1f).setDuration(300).start()
    }

    private fun hideLinkIndicator() {
        val indicator = findViewById<LinearLayout>(R.id.linkIndicator) ?: return
        scope.launch {
            delay(3000)
            indicator.animate().alpha(0f).setDuration(300).withEndAction {
                indicator.visibility = View.GONE
            }.start()
        }
    }

    // ---- Lifecycle ----

    override fun onResume() {
        super.onResume()
        startClockUpdates()
    }

    override fun onPause() {
        super.onPause()
        clockUpdateJob?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceCoordinator.stopAll()
        nightDimController.cleanup()
        transportController.cleanup()
        driftAnimator.stop()
        wallpaperMgr.stop()
        chimeMgr.stop()
        youtubeMgr.destroy()
        scope.cancel()
    }
}
