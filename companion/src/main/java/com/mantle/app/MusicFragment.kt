package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MusicFragment : Fragment() {

    private lateinit var presetList: RecyclerView
    private lateinit var emptyState: View
    private lateinit var fabAddPreset: FloatingActionButton
    private lateinit var adapter: PresetAdapter

    private val configStore get() = MantleApp.instance.configStore
    private val connectionManager get() = MantleApp.instance.connectionManager

    private val configListener = object : MantleConfigStore.OnConfigChangedListener {
        override fun onConfigChanged(config: MantleConfig) {
            refreshList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_music, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        presetList = view.findViewById(R.id.presetList)
        emptyState = view.findViewById(R.id.emptyState)
        fabAddPreset = view.findViewById(R.id.fabAddPreset)

        adapter = PresetAdapter(
            onPlay = { index -> onPlayPreset(index) },
            onClick = { index -> showEditDialog(index) },
            onLongClick = { index -> showDeleteDialog(index) }
        )
        presetList.layoutManager = LinearLayoutManager(requireContext())
        presetList.adapter = adapter

        setupDragReorder()

        fabAddPreset.setOnClickListener { showAddDialog() }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        configStore.addListener(configListener)
        refreshList()
    }

    override fun onPause() {
        super.onPause()
        configStore.removeListener(configListener)
    }

    private fun refreshList() {
        val cfg = configStore.config
        adapter.setData(cfg.player.presets, cfg.player.activePreset)
        emptyState.visibility = if (cfg.player.presets.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun onPlayPreset(index: Int) {
        configStore.setActivePreset(index)
        if (connectionManager.state == TvConnectionManager.ConnectionState.CONNECTED) {
            connectionManager.sendPlay(index)
        }
    }

    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_preset, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.presetNameInput)
        val urlInput = dialogView.findViewById<TextInputEditText>(R.id.presetUrlInput)
        val urlLayout = dialogView.findViewById<TextInputLayout>(R.id.presetUrlLayout)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Playlist")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val url = urlInput.text?.toString()?.trim() ?: ""
                if (url.isEmpty()) {
                    urlLayout.error = "URL is required"
                    return@setPositiveButton
                }
                configStore.addPreset(Preset(name = name.ifEmpty { "Preset ${configStore.config.player.presets.size + 1}" }, url = url))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(index: Int) {
        val preset = configStore.config.player.presets.getOrNull(index) ?: return
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_preset, null)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.presetNameInput)
        val urlInput = dialogView.findViewById<TextInputEditText>(R.id.presetUrlInput)

        nameInput.setText(preset.name)
        urlInput.setText(preset.url)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Playlist")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = nameInput.text?.toString()?.trim() ?: ""
                val url = urlInput.text?.toString()?.trim() ?: ""
                if (url.isNotEmpty()) {
                    configStore.updatePreset(index, Preset(name = name.ifEmpty { "Preset ${index + 1}" }, url = url))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteDialog(index: Int) {
        val preset = configStore.config.player.presets.getOrNull(index) ?: return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Preset")
            .setMessage("Delete \"${preset.name.ifEmpty { "Preset ${index + 1}" }}\"?")
            .setPositiveButton("Delete") { _, _ ->
                configStore.removePreset(index)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupDragReorder() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.adapterPosition
                val to = target.adapterPosition
                configStore.reorderPreset(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        }
        ItemTouchHelper(callback).attachToRecyclerView(presetList)
    }
}
