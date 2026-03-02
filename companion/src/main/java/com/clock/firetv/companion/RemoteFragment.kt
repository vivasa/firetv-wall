package com.clock.firetv.companion

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.LinearProgressIndicator

class RemoteFragment : Fragment() {

    private lateinit var connectionDot: View
    private lateinit var connectionStatus: TextView
    private lateinit var btnReconnect: MaterialButton
    private lateinit var nowPlayingTitle: TextView
    private lateinit var nowPlayingPlaylist: TextView
    private lateinit var presetChips: ChipGroup
    private lateinit var reconnectingProgress: LinearProgressIndicator
    private lateinit var btnSkipPrev: MaterialButton
    private lateinit var btnRewind: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnSkipNext: MaterialButton

    private val connectionManager get() = CompanionApp.instance.connectionManager
    private val deviceStore get() = CompanionApp.instance.deviceStore

    private val eventListener = object : TvConnectionManager.EventListener {
        override fun onConnectionStateChanged(state: TvConnectionManager.ConnectionState) {
            updateConnectionUI(state)
        }

        override fun onStateReceived(tvState: TvConnectionManager.TvState) {
            updateNowPlaying(tvState.nowPlayingTitle, tvState.nowPlayingPlaylist)
            updatePresets(tvState.presets, tvState.activePreset)
        }

        override fun onTrackChanged(title: String, playlist: String) {
            updateNowPlaying(title, playlist)
        }

        override fun onPlaybackStateChanged(playing: Boolean) {
            if (!playing) {
                updateNowPlaying("", "")
            }
        }

        override fun onSettingChanged(key: String, value: Any) {
            if (key == "activePreset") {
                updatePresets(connectionManager.tvState.presets, (value as Number).toInt())
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_remote, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        connectionDot = view.findViewById(R.id.connectionDot)
        connectionStatus = view.findViewById(R.id.connectionStatus)
        btnReconnect = view.findViewById(R.id.btnReconnect)
        nowPlayingTitle = view.findViewById(R.id.nowPlayingTitle)
        nowPlayingPlaylist = view.findViewById(R.id.nowPlayingPlaylist)
        presetChips = view.findViewById(R.id.presetChips)
        reconnectingProgress = view.findViewById(R.id.reconnectingProgress)
        btnSkipPrev = view.findViewById(R.id.btnSkipPrev)
        btnRewind = view.findViewById(R.id.btnRewind)
        btnStop = view.findViewById(R.id.btnStop)
        btnForward = view.findViewById(R.id.btnForward)
        btnSkipNext = view.findViewById(R.id.btnSkipNext)

        // Make the connection dot circular
        val dotBg = GradientDrawable()
        dotBg.shape = GradientDrawable.OVAL
        dotBg.setColor(com.google.android.material.R.attr.colorOnSurfaceVariant.let {
            val ta = requireContext().obtainStyledAttributes(intArrayOf(com.google.android.material.R.attr.colorOutline))
            val color = ta.getColor(0, 0x9E9E9E.toInt())
            ta.recycle()
            color
        })
        connectionDot.background = dotBg

        // Playback controls
        btnSkipPrev.setOnClickListener { connectionManager.sendSkip(-1) }
        btnRewind.setOnClickListener { connectionManager.sendSeek(-30) }
        btnStop.setOnClickListener { connectionManager.sendStop() }
        btnForward.setOnClickListener { connectionManager.sendSeek(30) }
        btnSkipNext.setOnClickListener { connectionManager.sendSkip(1) }

        // Reconnect button
        btnReconnect.setOnClickListener {
            val lastDevice = deviceStore.getLastConnectedDevice()
            if (lastDevice != null) {
                connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
            }
        }

        // Set initial state
        updateConnectionUI(connectionManager.state)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            val tvState = connectionManager.tvState
            updateNowPlaying(tvState.nowPlayingTitle, tvState.nowPlayingPlaylist)
            updatePresets(tvState.presets, tvState.activePreset)
        }
    }

    override fun onResume() {
        super.onResume()
        connectionManager.addListener(eventListener)
        updateConnectionUI(connectionManager.state)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            val tvState = connectionManager.tvState
            updateNowPlaying(tvState.nowPlayingTitle, tvState.nowPlayingPlaylist)
            updatePresets(tvState.presets, tvState.activePreset)
        }
    }

    override fun onPause() {
        super.onPause()
        connectionManager.removeListener(eventListener)
    }

    private fun updateConnectionUI(state: TvConnectionManager.ConnectionState) {
        val dotBg = connectionDot.background as? GradientDrawable ?: return
        when (state) {
            TvConnectionManager.ConnectionState.CONNECTED -> {
                dotBg.setColor(ContextCompat.getColor(requireContext(), R.color.connected_green))
                connectionStatus.text = connectionManager.tvState.deviceName.ifEmpty { "Connected" }
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.GONE
                setControlsEnabled(true)
            }
            TvConnectionManager.ConnectionState.RECONNECTING -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Reconnecting..."
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.VISIBLE
                setControlsEnabled(false)
            }
            TvConnectionManager.ConnectionState.CONNECTING,
            TvConnectionManager.ConnectionState.AUTHENTICATING -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Connecting..."
                btnReconnect.visibility = View.GONE
                reconnectingProgress.visibility = View.GONE
                setControlsEnabled(false)
            }
            TvConnectionManager.ConnectionState.DISCONNECTED -> {
                dotBg.setColor(resolveThemeColor(com.google.android.material.R.attr.colorOutline))
                connectionStatus.text = "Disconnected"
                btnReconnect.visibility = if (deviceStore.getLastConnectedDevice() != null) View.VISIBLE else View.GONE
                reconnectingProgress.visibility = View.GONE
                setControlsEnabled(false)
                updateNowPlaying("", "")
                presetChips.removeAllViews()
            }
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val ta = requireContext().obtainStyledAttributes(intArrayOf(attr))
        val color = ta.getColor(0, 0x9E9E9E.toInt())
        ta.recycle()
        return color
    }

    private fun setControlsEnabled(enabled: Boolean) {
        btnSkipPrev.isEnabled = enabled
        btnRewind.isEnabled = enabled
        btnStop.isEnabled = enabled
        btnForward.isEnabled = enabled
        btnSkipNext.isEnabled = enabled
    }

    private fun updateNowPlaying(title: String, playlist: String) {
        if (title.isNotEmpty()) {
            nowPlayingTitle.text = title
            if (playlist.isNotEmpty()) {
                nowPlayingPlaylist.text = playlist
                nowPlayingPlaylist.visibility = View.VISIBLE
            } else {
                nowPlayingPlaylist.visibility = View.GONE
            }
        } else {
            nowPlayingTitle.text = "Not playing"
            nowPlayingPlaylist.visibility = View.GONE
        }
    }

    private fun updatePresets(presets: List<TvConnectionManager.Preset>, activePreset: Int) {
        presetChips.removeAllViews()
        presets.forEach { preset ->
            val chip = Chip(requireContext()).apply {
                text = preset.name.ifEmpty { "Preset ${preset.index + 1}" }
                isCheckable = true
                isChecked = preset.index == activePreset
                setOnClickListener {
                    connectionManager.sendPlay(preset.index)
                }
            }
            presetChips.addView(chip)
        }
    }
}
