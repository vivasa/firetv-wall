package com.mantle.app

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class PresetAdapter(
    private val onPlay: (Int) -> Unit,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<PresetAdapter.ViewHolder>() {

    private var presets: List<Preset> = emptyList()
    private var activePreset: Int = -1

    fun setData(presets: List<Preset>, activePreset: Int) {
        this.presets = presets
        this.activePreset = activePreset
        notifyDataSetChanged()
    }

    override fun getItemCount() = presets.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_preset, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val preset = presets[position]
        val isActive = position == activePreset

        holder.presetIndex.text = "${position + 1}"
        holder.presetName.text = preset.name.ifEmpty { "Preset ${position + 1}" }
        holder.presetUrl.text = preset.url
        holder.btnPlay.text = if (isActive) "■" else "▶"

        if (isActive) {
            holder.itemView.setBackgroundColor(0x0D1A73E8.toInt())
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.btnPlay.setOnClickListener { onPlay(position) }
        holder.itemView.setOnClickListener { onClick(position) }
        holder.itemView.setOnLongClickListener {
            onLongClick(position)
            true
        }
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val presetIndex: TextView = view.findViewById(R.id.presetIndex)
        val presetName: TextView = view.findViewById(R.id.presetName)
        val presetUrl: TextView = view.findViewById(R.id.presetUrl)
        val btnPlay: MaterialButton = view.findViewById(R.id.btnPlay)
    }
}
