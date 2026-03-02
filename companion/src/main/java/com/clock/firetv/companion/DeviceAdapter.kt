package com.clock.firetv.companion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DeviceAdapter(
    private val onAction: (DeviceItem) -> Unit,
    private val onLongPress: (DeviceItem) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    data class DeviceItem(
        val deviceId: String,
        val deviceName: String,
        val host: String,
        val port: Int,
        val isPaired: Boolean,
        val storedToken: String? = null
    )

    private val items = mutableListOf<DeviceItem>()

    fun setItems(newItems: List<DeviceItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.deviceName
        holder.address.text = "${item.host}:${item.port}"
        if (item.isPaired) {
            holder.badge.text = "Paired"
            holder.badge.setTextColor(0xFF4CAF50.toInt())
            holder.action.text = "Connect"
        } else {
            holder.badge.text = "New"
            holder.badge.setTextColor(0xFF888888.toInt())
            holder.action.text = "Pair"
        }
        holder.action.setOnClickListener { onAction(item) }
        holder.itemView.setOnLongClickListener {
            onLongPress(item)
            true
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.deviceName)
        val address: TextView = view.findViewById(R.id.deviceAddress)
        val badge: TextView = view.findViewById(R.id.deviceBadge)
        val action: MaterialButton = view.findViewById(R.id.btnAction)
    }
}
