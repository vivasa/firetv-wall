package com.mantle.app

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import coil.load

class MiniPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val artwork: ImageView
    private val title: TextView
    private val playPauseButton: ImageButton

    var onPlayPauseClick: (() -> Unit)? = null
    var onBarClick: (() -> Unit)? = null

    private val artworkService = ArtworkService()

    init {
        LayoutInflater.from(context).inflate(R.layout.view_mini_player, this, true)
        artwork = findViewById(R.id.miniArtwork)
        title = findViewById(R.id.miniTitle)
        playPauseButton = findViewById(R.id.miniPlayPause)

        playPauseButton.setOnClickListener {
            performHaptic(it)
            onPlayPauseClick?.invoke()
        }

        setOnClickListener {
            onBarClick?.invoke()
        }
    }

    private var lastDisplayedTitle: String? = null

    fun bind(state: PlayerUiState) {
        val nowPlaying = state.nowPlaying
        val switchingIdx = state.switchingPresetIndex
        val isSwitching = switchingIdx != null

        // During a switch, show the new playlist name; otherwise show the track title
        val switchingPlaylist = if (isSwitching) {
            state.allPlaylists.getOrNull(switchingIdx!!)
        } else null

        val displayTitle = if (isSwitching && switchingPlaylist != null) {
            switchingPlaylist.name
        } else {
            nowPlaying.title
        }

        val displayArtworkUrl = if (isSwitching && switchingPlaylist != null) {
            switchingPlaylist.artworkUrl
        } else {
            nowPlaying.artworkUrl
        }

        val artworkName = if (isSwitching && switchingPlaylist != null) {
            switchingPlaylist.name
        } else {
            nowPlaying.playlist ?: "Unknown"
        }

        if (displayTitle != null || isSwitching) {
            visibility = View.VISIBLE

            // Crossfade title when it changes
            if (displayTitle != lastDisplayedTitle && lastDisplayedTitle != null) {
                title.animate().alpha(0f).setDuration(150).withEndAction {
                    title.text = displayTitle ?: ""
                    title.animate().alpha(1f).setDuration(150).start()
                }.start()
            } else {
                title.text = displayTitle ?: ""
            }
            lastDisplayedTitle = displayTitle

            if (displayArtworkUrl != null) {
                artwork.load(displayArtworkUrl) {
                    crossfade(true)
                }
            } else {
                val (color1, color2) = artworkService.fallbackGradientColors(artworkName)
                artwork.setImageDrawable(
                    GradientDrawable(
                        GradientDrawable.Orientation.TL_BR,
                        intArrayOf(color1, color2)
                    ).apply {
                        cornerRadius = 8f * resources.displayMetrics.density
                    }
                )
            }

            playPauseButton.setImageResource(
                if (nowPlaying.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        } else if (nowPlaying.title == null && !isSwitching) {
            visibility = View.GONE
            lastDisplayedTitle = null
        }
    }

    private fun performHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}
