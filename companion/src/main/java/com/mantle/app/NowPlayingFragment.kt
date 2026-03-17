package com.mantle.app

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class NowPlayingFragment : Fragment() {

    private val viewModel: PlayerViewModel by activityViewModels()
    private val artworkService = ArtworkService()

    private lateinit var btnBack: ImageButton
    private lateinit var deviceChip: Chip
    private lateinit var largeArtwork: ImageView
    private lateinit var trackTitle: TextView
    private lateinit var playlistName: TextView
    private lateinit var btnSkipPrev: ImageButton
    private lateinit var btnRewind: ImageButton
    private lateinit var btnPlayPause: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnSkipNext: ImageButton
    private lateinit var trackListHeader: TextView
    private lateinit var trackListRecycler: RecyclerView
    private val trackListAdapter = TrackListAdapter()
    private lateinit var btnSleepTimer: ImageButton
    private lateinit var sleepTimerLabel: TextView

    companion object {
        private val SLEEP_TIMER_OPTIONS = listOf(15, 30, 45, 60, 120, 0) // 0 = Off
    }

    private var currentSleepTimerIndex = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_now_playing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupClickListeners()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state -> render(state) }
        }
    }

    private fun bindViews(view: View) {
        btnBack = view.findViewById(R.id.btnBack)
        deviceChip = view.findViewById(R.id.deviceChip)
        largeArtwork = view.findViewById(R.id.largeArtwork)
        trackTitle = view.findViewById(R.id.trackTitle)
        playlistName = view.findViewById(R.id.playlistName)
        btnSkipPrev = view.findViewById(R.id.btnSkipPrev)
        btnRewind = view.findViewById(R.id.btnRewind)
        btnPlayPause = view.findViewById(R.id.btnPlayPause)
        btnForward = view.findViewById(R.id.btnForward)
        btnSkipNext = view.findViewById(R.id.btnSkipNext)
        trackListHeader = view.findViewById(R.id.trackListHeader)
        trackListRecycler = view.findViewById(R.id.trackListRecycler)
        trackListRecycler.layoutManager = LinearLayoutManager(requireContext())
        trackListRecycler.adapter = trackListAdapter
        btnSleepTimer = view.findViewById(R.id.btnSleepTimer)
        sleepTimerLabel = view.findViewById(R.id.sleepTimerLabel)

        // Make artwork square
        largeArtwork.post {
            val params = largeArtwork.layoutParams
            params.height = largeArtwork.width
            largeArtwork.layoutParams = params
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPlayPause.setOnClickListener {
            performHaptic(it)
            viewModel.togglePlayPause()
        }

        btnSkipPrev.setOnClickListener {
            performHapticLight(it)
            viewModel.skipPrevious()
        }

        btnSkipNext.setOnClickListener {
            performHapticLight(it)
            viewModel.skipNext()
        }

        btnRewind.setOnClickListener {
            performHapticLight(it)
            viewModel.seekBackward()
        }

        btnForward.setOnClickListener {
            performHapticLight(it)
            viewModel.seekForward()
        }

        btnSleepTimer.setOnClickListener {
            performHapticLight(it)
            cycleSleepTimer()
        }

        deviceChip.setOnClickListener {
            DeviceSheetFragment().show(parentFragmentManager, "device_sheet")
        }
    }

    private fun render(state: PlayerUiState) {
        renderNowPlaying(state)
        renderControls(state)
        renderTrackList(state)
        renderSleepTimer(state)
        renderDeviceChip(state)
    }

    private fun renderNowPlaying(state: PlayerUiState) {
        val nowPlaying = state.nowPlaying
        if (nowPlaying.title != null) {
            trackTitle.text = nowPlaying.title
            playlistName.text = nowPlaying.playlist ?: ""
            playlistName.visibility = if (nowPlaying.playlist != null) View.VISIBLE else View.GONE

            if (nowPlaying.artworkUrl != null) {
                largeArtwork.load(nowPlaying.artworkUrl) { crossfade(true) }
            } else {
                val name = nowPlaying.playlist ?: "Unknown"
                val (c1, c2) = artworkService.fallbackGradientColors(name)
                largeArtwork.setImageDrawable(
                    GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
                )
            }
        } else {
            trackTitle.text = "Not playing"
            trackTitle.setTextColor(resources.getColor(R.color.mantle_on_surface_muted, null))
            playlistName.visibility = View.GONE
            largeArtwork.setImageDrawable(
                GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    intArrayOf(resources.getColor(R.color.mantle_surface, null),
                        resources.getColor(R.color.mantle_surface_elevated, null)))
            )
        }
    }

    private fun renderControls(state: PlayerUiState) {
        val connected = state.connectionState == TvConnectionManager.ConnectionState.CONNECTED
        val alpha = if (connected) 1f else 0.38f

        btnPlayPause.apply {
            isEnabled = connected
            this.alpha = alpha
            setImageResource(if (state.nowPlaying.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }

        listOf(btnSkipPrev, btnSkipNext, btnRewind, btnForward).forEach {
            it.isEnabled = connected
            it.alpha = alpha
        }
    }

    private fun renderSleepTimer(state: PlayerUiState) {
        val minutes = state.sleepTimerMinutes
        if (minutes != null && minutes > 0) {
            sleepTimerLabel.text = "${minutes} min"
            sleepTimerLabel.visibility = View.VISIBLE
            btnSleepTimer.setColorFilter(resources.getColor(R.color.mantle_accent, null))
        } else {
            sleepTimerLabel.visibility = View.GONE
            btnSleepTimer.setColorFilter(resources.getColor(R.color.mantle_on_surface_muted, null))
        }
    }

    private fun renderDeviceChip(state: PlayerUiState) {
        if (state.connectionState == TvConnectionManager.ConnectionState.CONNECTED && state.deviceName != null) {
            deviceChip.text = state.deviceName
        } else {
            deviceChip.text = "Not connected"
        }
    }

    private fun renderTrackList(state: PlayerUiState) {
        if (state.trackList.isEmpty()) {
            trackListHeader.visibility = View.GONE
            trackListRecycler.visibility = View.GONE
            return
        }
        trackListHeader.visibility = View.VISIBLE
        trackListRecycler.visibility = View.VISIBLE
        val previousIndex = trackListAdapter.currentTrackIndex
        trackListAdapter.submitList(state.trackList, state.currentTrackIndex)
        if (state.currentTrackIndex != previousIndex && state.currentTrackIndex >= 0) {
            trackListRecycler.scrollToPosition(state.currentTrackIndex)
        }
    }

    // --- Track List Adapter ---

    inner class TrackListAdapter : RecyclerView.Adapter<TrackListAdapter.ViewHolder>() {

        private var items: List<TrackItem> = emptyList()
        var currentTrackIndex: Int = -1
            private set

        fun submitList(newItems: List<TrackItem>, newCurrentIndex: Int) {
            val oldIndex = currentTrackIndex
            items = newItems
            currentTrackIndex = newCurrentIndex
            if (items.size != itemCount) {
                notifyDataSetChanged()
            } else {
                if (oldIndex in items.indices) notifyItemChanged(oldIndex)
                if (newCurrentIndex in items.indices && newCurrentIndex != oldIndex) notifyItemChanged(newCurrentIndex)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_track_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val indexText: TextView = view.findViewById(R.id.trackIndex)
            private val titleText: TextView = view.findViewById(R.id.trackTitle)

            fun bind(item: TrackItem) {
                indexText.text = (item.index + 1).toString()
                titleText.text = item.title.ifEmpty { "Track ${item.index + 1}" }

                val isCurrent = item.index == currentTrackIndex
                titleText.setTextColor(resources.getColor(
                    if (isCurrent) R.color.mantle_accent else R.color.mantle_on_surface, null
                ))
                indexText.setTextColor(resources.getColor(
                    if (isCurrent) R.color.mantle_accent else R.color.mantle_on_surface_muted, null
                ))

                itemView.setOnClickListener {
                    viewModel.playTrack(item.index)
                }
            }
        }
    }

    private fun cycleSleepTimer() {
        currentSleepTimerIndex = (currentSleepTimerIndex + 1) % SLEEP_TIMER_OPTIONS.size
        val minutes = SLEEP_TIMER_OPTIONS[currentSleepTimerIndex]
        viewModel.setSleepTimer(if (minutes == 0) null else minutes)
    }

    private fun performHaptic(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    private fun performHapticLight(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
