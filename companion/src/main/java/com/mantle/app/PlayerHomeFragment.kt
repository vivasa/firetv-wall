package com.mantle.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class PlayerHomeFragment : Fragment() {

    private val viewModel: PlayerViewModel by activityViewModels()
    private val artworkService = ArtworkService()

    private val blePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            viewModel.startDiscovery()
        }
    }

    private lateinit var connectionDot: View
    private lateinit var deviceNameChip: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var connectionBanner: TextView
    private lateinit var playlistRecycler: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var fabAdd: FloatingActionButton

    private val recentAdapter = RecentPlayedAdapter()
    private val allPlaylistsAdapter = AllPlaylistsAdapter()
    private var currentState: PlayerUiState? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_player_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupRecyclerView()
        setupClickListeners()
        setupSwipeAndDrag()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state -> render(state) }
        }
    }

    override fun onResume() {
        super.onResume()
        requestBlePermissionsAndStartDiscovery()
    }

    private fun requestBlePermissionsAndStartDiscovery() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.startDiscovery()
        } else {
            blePermissionLauncher.launch(permissions)
        }
    }

    private fun bindViews(view: View) {
        connectionDot = view.findViewById(R.id.connectionDot)
        deviceNameChip = view.findViewById(R.id.deviceNameChip)
        btnSettings = view.findViewById(R.id.btnSettings)
        connectionBanner = view.findViewById(R.id.connectionBanner)
        playlistRecycler = view.findViewById(R.id.playlistRecycler)
        emptyState = view.findViewById(R.id.emptyState)
        fabAdd = view.findViewById(R.id.fabAdd)
    }

    private fun setupRecyclerView() {
        playlistRecycler.layoutManager = LinearLayoutManager(requireContext())
        playlistRecycler.adapter = allPlaylistsAdapter
    }

    private fun setupClickListeners() {
        btnSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        deviceNameChip.setOnClickListener {
            DeviceSheetFragment().show(parentFragmentManager, "device_sheet")
        }

        connectionBanner.setOnClickListener {
            viewModel.reconnect()
        }

        fabAdd.setOnClickListener {
            showAddPresetDialog()
        }
    }

    private fun setupSwipeAndDrag() {
        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT
        ) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                val from = vh.adapterPosition
                val to = target.adapterPosition
                viewModel.reorderPreset(from, to)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.adapterPosition
                val playlist = currentState?.allPlaylists?.getOrNull(position) ?: return
                AlertDialog.Builder(requireContext())
                    .setTitle("Delete ${playlist.name}?")
                    .setPositiveButton("Delete") { _, _ -> viewModel.removePreset(position) }
                    .setNegativeButton("Cancel") { _, _ -> allPlaylistsAdapter.notifyItemChanged(position) }
                    .setOnCancelListener { allPlaylistsAdapter.notifyItemChanged(position) }
                    .show()
            }
        })
        touchHelper.attachToRecyclerView(playlistRecycler)
    }

    private fun render(state: PlayerUiState) {
        currentState = state
        renderTopBar(state)
        renderConnectionBanner(state)
        renderPlaylists(state)
    }

    private fun renderTopBar(state: PlayerUiState) {
        val connected = state.connectionState == TvConnectionManager.ConnectionState.CONNECTED
        connectionDot.setBackgroundResource(
            if (connected) R.drawable.circle_indicator else R.drawable.circle_indicator
        )
        connectionDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (connected) resources.getColor(R.color.connected_green, null)
            else resources.getColor(R.color.mantle_on_surface_muted, null)
        )
        deviceNameChip.text = if (connected && state.deviceName != null) state.deviceName else "Not connected"
    }

    private fun renderConnectionBanner(state: PlayerUiState) {
        val wasConnected = state.devices.any { it.isPaired }
        val isDisconnected = state.connectionState != TvConnectionManager.ConnectionState.CONNECTED
        connectionBanner.visibility = if (wasConnected && isDisconnected) View.VISIBLE else View.GONE
    }

    private fun renderPlaylists(state: PlayerUiState) {
        if (state.allPlaylists.isEmpty()) {
            playlistRecycler.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            playlistRecycler.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            allPlaylistsAdapter.submitList(state.allPlaylists, state.switchingPresetIndex)
        }
    }

    private fun showAddPresetDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_preset, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.presetNameInput)
        val urlInput = dialogView.findViewById<EditText>(R.id.presetUrlInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Playlist")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString()
                val url = urlInput.text.toString()
                if (url.isNotBlank()) {
                    viewModel.addPreset(
                        name = name.ifEmpty { "Preset ${(currentState?.allPlaylists?.size ?: 0) + 1}" },
                        url = url
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditPresetDialog(playlist: PlaylistItem) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_preset, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.presetNameInput)
        val urlInput = dialogView.findViewById<EditText>(R.id.presetUrlInput)
        nameInput.setText(playlist.name)
        urlInput.setText(playlist.url)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Playlist")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text.toString()
                val url = urlInput.text.toString()
                if (url.isNotBlank()) {
                    viewModel.updatePreset(playlist.index, name, url)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- All Playlists adapter ---

    inner class AllPlaylistsAdapter : RecyclerView.Adapter<AllPlaylistsAdapter.ViewHolder>() {

        private var items: List<PlaylistItem> = emptyList()
        private var switchingIndex: Int? = null

        fun submitList(newItems: List<PlaylistItem>, newSwitchingIndex: Int?) {
            val oldItems = items
            val oldSwitchingIndex = switchingIndex
            items = newItems
            switchingIndex = newSwitchingIndex

            if (oldItems.size != newItems.size) {
                notifyDataSetChanged()
                return
            }

            for (i in newItems.indices) {
                val old = oldItems[i]
                val new = newItems[i]
                val wasSwitching = i == oldSwitchingIndex
                val isSwitching = i == newSwitchingIndex
                if (old.isActive != new.isActive || old.isPlaying != new.isPlaying ||
                    old.name != new.name || old.artworkUrl != new.artworkUrl ||
                    wasSwitching != isSwitching) {
                    notifyItemChanged(i)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val artwork: ImageView = view.findViewById(R.id.rowArtwork)
            private val name: TextView = view.findViewById(R.id.rowName)
            private val subtitle: TextView = view.findViewById(R.id.rowSubtitle)
            private val activeBar: View = view.findViewById(R.id.rowActiveBar)
            private val overflow: ImageButton = view.findViewById(R.id.rowOverflow)

            fun bind(item: PlaylistItem) {
                name.text = item.name

                if (item.artworkUrl != null) {
                    artwork.load(item.artworkUrl) { crossfade(true) }
                } else {
                    val (c1, c2) = artworkService.fallbackGradientColors(item.name)
                    artwork.setImageDrawable(
                        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
                    )
                }

                val isSwitching = item.index == switchingIndex
                if (isSwitching) {
                    subtitle.text = "Loading..."
                    subtitle.visibility = View.VISIBLE
                    activeBar.visibility = View.VISIBLE
                    name.setTextColor(resources.getColor(R.color.mantle_accent, null))
                } else if (item.isActive) {
                    subtitle.text = "Now Playing"
                    subtitle.visibility = View.VISIBLE
                    activeBar.visibility = View.VISIBLE
                    name.setTextColor(resources.getColor(R.color.mantle_accent, null))
                } else {
                    subtitle.visibility = View.GONE
                    activeBar.visibility = View.GONE
                    name.setTextColor(resources.getColor(R.color.mantle_on_surface, null))
                }

                itemView.setOnClickListener {
                    viewModel.selectPreset(item.index)
                }

                itemView.setOnLongClickListener {
                    showEditPresetDialog(item)
                    true
                }

                overflow.setOnClickListener {
                    val popup = android.widget.PopupMenu(requireContext(), it)
                    popup.menu.add("Edit")
                    popup.menu.add("Delete")
                    popup.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.title) {
                            "Edit" -> showEditPresetDialog(item)
                            "Delete" -> viewModel.removePreset(item.index)
                        }
                        true
                    }
                    popup.show()
                }
            }
        }
    }

    // --- Recently Played adapter (used for the grid cards) ---

    inner class RecentPlayedAdapter : RecyclerView.Adapter<RecentPlayedAdapter.ViewHolder>() {

        private var items: List<PlaylistItem> = emptyList()

        fun submitList(newItems: List<PlaylistItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_playlist_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val artwork: ImageView = view.findViewById(R.id.cardArtwork)
            private val name: TextView = view.findViewById(R.id.cardName)
            private val playingIndicator: ImageView = view.findViewById(R.id.cardPlayingIndicator)

            fun bind(item: PlaylistItem) {
                name.text = item.name
                playingIndicator.visibility = if (item.isPlaying) View.VISIBLE else View.GONE

                if (item.artworkUrl != null) {
                    artwork.load(item.artworkUrl) { crossfade(true) }
                } else {
                    val (c1, c2) = artworkService.fallbackGradientColors(item.name)
                    artwork.setImageDrawable(
                        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c1, c2))
                    )
                }

                itemView.setOnClickListener {
                    viewModel.selectPreset(item.index)
                }
            }
        }
    }
}
