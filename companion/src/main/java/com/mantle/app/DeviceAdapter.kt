package com.mantle.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class DeviceAdapter(
    private val onAction: (DeviceItem) -> Unit,
    private val onLongPress: (DeviceItem) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private val items = mutableListOf<DeviceItem>()
    private var connectedDeviceId: String? = null

    fun setConnectedDeviceId(id: String?) {
        connectedDeviceId = id
        notifyDataSetChanged()
    }

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
        val addressText = if (item.transportType == TransportType.BLE) "BLE" else "${item.host}:${item.port}"
        holder.address.text = addressText
        val context = holder.itemView.context
        if (item.isPaired) {
            val transport = if (item.transportType == TransportType.BLE) "BLE" else "WiFi"
            holder.badge.text = "Paired · $transport"
            holder.badge.setTextColor(ContextCompat.getColor(context, R.color.connected_green))
            if (item.deviceId == connectedDeviceId) {
                holder.action.text = "Connected"
                holder.action.isEnabled = false
                holder.action.alpha = 0.5f
            } else {
                holder.action.text = "Connect"
                holder.action.isEnabled = true
                holder.action.alpha = 1.0f
                holder.action.setOnClickListener { onAction(item) }
            }
        } else {
            val transport = if (item.transportType == TransportType.BLE) "BLE" else "WiFi"
            holder.badge.text = transport
            holder.badge.setTextColor(ContextCompat.getColor(context, R.color.mantle_on_surface_muted))
            holder.action.text = "Pair"
            holder.action.isEnabled = true
            holder.action.alpha = 1.0f
            holder.action.setOnClickListener { onAction(item) }
        }
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
