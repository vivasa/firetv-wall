package com.clock.firetv

import com.firetv.protocol.ProtocolEvents
import com.firetv.protocol.ProtocolKeys
import org.json.JSONArray
import org.json.JSONObject

class CompanionBridge(
    private val settings: SettingsManager,
    private val youtubeMgr: YouTubePlayerManager,
    private val commandHandler: CompanionCommandHandler
) : CompanionCommandHandler.Listener {

    interface Callback {
        fun onShowPin(pin: String)
        fun onDismissPin()
        fun onCompanionConnected()
        fun onCompanionDisconnected()
        fun onSyncConfig(config: JSONObject)
        fun onPlayerSettingsChanged()
    }

    var callback: Callback? = null

    fun attach() {
        commandHandler.listener = this
    }

    override fun onCompanionConnected() {
        callback?.onCompanionConnected()
    }

    override fun onCompanionDisconnected() {
        callback?.onCompanionDisconnected()
    }

    override fun onShowPin(pin: String) {
        callback?.onShowPin(pin)
    }

    override fun onDismissPin() {
        callback?.onDismissPin()
    }

    override fun onPlayPreset(index: Int) {
        settings.activePreset = index
        callback?.onPlayerSettingsChanged()
    }

    override fun onStopPlayback() {
        settings.activePreset = -1
        callback?.onPlayerSettingsChanged()
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

    override fun onSyncConfig(config: JSONObject) {
        callback?.onSyncConfig(config)
    }

    override fun onPlayTrack(trackIndex: Int) {
        youtubeMgr.playTrackAtIndex(trackIndex)
    }

    override fun onGetPlaylistTracks() {
        commandHandler.broadcastEvent(buildPlaylistTracksEvent())
    }

    fun buildPlaylistTracksEvent(): JSONObject {
        val tracks = JSONArray()
        youtubeMgr.getTrackList().forEach { (index, title) ->
            tracks.put(JSONObject().apply {
                put(ProtocolKeys.INDEX, index)
                put(ProtocolKeys.TITLE, title ?: "")
            })
        }
        return JSONObject().apply {
            put(ProtocolKeys.EVT, ProtocolEvents.PLAYLIST_TRACKS)
            put(ProtocolKeys.PLAYLIST, youtubeMgr.getPlaylistTitle() ?: "")
            put(ProtocolKeys.CURRENT_INDEX, youtubeMgr.getCurrentIndex())
            put(ProtocolKeys.TRACKS, tracks)
        }
    }
}
