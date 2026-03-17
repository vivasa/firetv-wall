package com.mantle.app

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConfigSyncManager(
    private val configStore: MantleConfigStore,
    private val scope: CoroutineScope,
    private val sendSync: (JSONObject) -> Unit
) : MantleConfigStore.OnConfigChangedListener {

    companion object {
        private const val TAG = "ConfigSyncMgr"
        private const val SYNC_DEBOUNCE_MS = 500L
    }

    private var connected = false
    private var syncJob: Job? = null
    var suppressNextSync = false

    fun setConnected(connected: Boolean) {
        this.connected = connected
        if (connected) {
            syncPlaylistsOnly()
        } else {
            syncJob?.cancel()
        }
    }

    override fun onConfigChanged(config: MantleConfig) {
        if (suppressNextSync) {
            suppressNextSync = false
            return
        }
        if (connected) {
            scheduleSyncConfig()
        }
    }

    fun syncPlaylistsOnly() {
        val configJson = configStore.toJson()
        configJson.optJSONObject("player")?.put("activePreset", -1)
        Log.d(TAG, "Sending sync_config (playlists only) v${configJson.optInt("version")}")
        sendSync(TvProtocolHandler.buildSyncConfig(configJson))
    }

    fun flushSync() {
        if (connected) {
            syncJob?.cancel()
            doSync()
        }
    }

    private fun scheduleSyncConfig() {
        syncJob?.cancel()
        syncJob = scope.launch {
            delay(SYNC_DEBOUNCE_MS)
            doSync()
        }
    }

    private fun doSync() {
        val configJson = configStore.toJson()
        Log.d(TAG, "Sending sync_config v${configJson.optInt("version")}")
        sendSync(TvProtocolHandler.buildSyncConfig(configJson))
    }
}
