package com.mantle.app

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MantleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MantleActivity"
    }

    private val viewModel: PlayerViewModel by viewModels()
    private val connectionManager get() = MantleApp.instance.connectionManager
    private val deviceStore get() = MantleApp.instance.deviceStore

    private lateinit var miniPlayer: MiniPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        miniPlayer = findViewById(R.id.miniPlayer)

        if (savedInstanceState == null) {
            val needsOnboarding = deviceStore.getPairedDevices().isEmpty()
            if (needsOnboarding) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, OnboardingFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, PlayerHomeFragment())
                    .commit()
            }
        }

        // Wire mini player
        miniPlayer.onPlayPauseClick = { viewModel.togglePlayPause() }
        miniPlayer.onBarClick = {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NowPlayingFragment())
                .addToBackStack(null)
                .commit()
        }

        // Observe state for mini player
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                miniPlayer.bind(state)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (connectionManager.state == TvConnectionManager.ConnectionState.DISCONNECTED) {
            tryAutoConnect()
        }
    }

    private fun tryAutoConnect() {
        if (connectionManager.state != TvConnectionManager.ConnectionState.DISCONNECTED) return
        val lastDevice = deviceStore.getLastConnectedDevice() ?: return
        Log.d(TAG, "Auto-connecting to ${lastDevice.deviceName} (${lastDevice.host}:${lastDevice.port})")
        connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
    }
}
