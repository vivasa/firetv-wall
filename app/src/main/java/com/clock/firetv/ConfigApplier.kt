package com.clock.firetv

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.firetv.protocol.ProtocolKeys
import org.json.JSONObject

class ConfigApplier {

    fun apply(
        config: JSONObject,
        settings: SettingsManager,
        wallpaperMgr: WallpaperManager,
        chimeMgr: ChimeManager,
        driftAnimator: DriftAnimator,
        youtubeMgr: YouTubePlayerManager,
        youtubeContainer: FrameLayout,
        nowPlayingLabel: TextView,
        onThemeChanged: () -> Unit
    ) {
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

        // Apply changes
        if (settings.theme != oldTheme) {
            youtubeMgr.savePlaybackState(settings.prefs)
            onThemeChanged()
        } else {
            applyNonPlayerSettings(settings, wallpaperMgr, chimeMgr, driftAnimator)
            applyPlayerSettings(settings, youtubeMgr, youtubeContainer, nowPlayingLabel)
            wallpaperMgr.updateInterval(settings.wallpaperIntervalMinutes)
        }
    }

    fun applyNonPlayerSettings(
        settings: SettingsManager,
        wallpaperMgr: WallpaperManager,
        chimeMgr: ChimeManager,
        driftAnimator: DriftAnimator
    ) {
        if (settings.driftEnabled) driftAnimator.start() else driftAnimator.stop()

        if (settings.wallpaperEnabled) {
            wallpaperMgr.start(settings.wallpaperIntervalMinutes)
        } else {
            wallpaperMgr.stop()
        }

        if (settings.chimeEnabled) chimeMgr.scheduleNextChime() else chimeMgr.stop()
    }

    fun applyPlayerSettings(
        settings: SettingsManager,
        youtubeMgr: YouTubePlayerManager,
        youtubeContainer: FrameLayout,
        nowPlayingLabel: TextView
    ) {
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
}
