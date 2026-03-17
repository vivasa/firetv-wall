package com.mantle.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConnectionDiagnosticsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private val adapter = EventAdapter()

    private val connectionManager get() = MantleApp.instance.connectionManager

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_diagnostics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerEvents)
        emptyText = view.findViewById(R.id.emptyText)
        val btnBack = view.findViewById<ImageButton>(R.id.btnBack)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        refreshEvents()

        viewLifecycleOwner.lifecycleScope.launch {
            connectionManager.connectionState.collect {
                refreshEvents()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ConnectionEventLog.restoreFromPrefs(requireContext())
        refreshEvents()
    }

    override fun onPause() {
        super.onPause()
        ConnectionEventLog.persistToPrefs(requireContext())
    }

    private fun refreshEvents() {
        val events = ConnectionEventLog.getEvents()
        adapter.submitList(events)
        emptyText.visibility = if (events.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (events.isEmpty()) View.GONE else View.VISIBLE
    }

    // --- Adapter ---

    private inner class EventAdapter : RecyclerView.Adapter<EventAdapter.ViewHolder>() {

        private var items: List<ConnectionEventLog.ConnectionEvent> = emptyList()

        fun submitList(newItems: List<ConnectionEventLog.ConnectionEvent>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_connection_event, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val indicator: View = view.findViewById(R.id.indicator)
            private val eventType: TextView = view.findViewById(R.id.eventType)
            private val eventDetail: TextView = view.findViewById(R.id.eventDetail)
            private val eventTime: TextView = view.findViewById(R.id.eventTime)

            private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            private val dateFormat = SimpleDateFormat("MMM d, HH:mm:ss", Locale.getDefault())

            fun bind(event: ConnectionEventLog.ConnectionEvent) {
                eventType.text = event.type.name.replace('_', ' ')

                if (event.detail.isNotEmpty()) {
                    eventDetail.text = event.detail
                    eventDetail.visibility = View.VISIBLE
                } else {
                    eventDetail.visibility = View.GONE
                }

                // Show date if not today, otherwise just time
                val now = System.currentTimeMillis()
                val isToday = (now - event.timestamp) < 86_400_000L
                eventTime.text = if (isToday) timeFormat.format(Date(event.timestamp))
                    else dateFormat.format(Date(event.timestamp))

                val colorRes = when (event.type) {
                    ConnectionEventLog.EventType.CONNECTED,
                    ConnectionEventLog.EventType.AUTH_OK -> R.color.connected_green

                    ConnectionEventLog.EventType.ERROR,
                    ConnectionEventLog.EventType.TIMEOUT,
                    ConnectionEventLog.EventType.AUTH_FAILED,
                    ConnectionEventLog.EventType.DISCONNECTED -> R.color.error_red

                    ConnectionEventLog.EventType.CONNECTING,
                    ConnectionEventLog.EventType.RECONNECTING -> R.color.warning_yellow
                }

                indicator.background.setTint(ContextCompat.getColor(indicator.context, colorRes))
            }
        }
    }
}
