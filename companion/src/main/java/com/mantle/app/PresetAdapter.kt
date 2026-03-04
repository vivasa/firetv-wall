package com.mantle.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

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

        val card = holder.itemView as MaterialCardView
        val context = holder.itemView.context
        if (isActive) {
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.mantle_surface_elevated))
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.mantle_surface))
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
