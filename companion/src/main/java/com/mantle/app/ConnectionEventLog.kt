package com.mantle.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ConnectionEventLog {

    enum class EventType {
        CONNECTING, AUTH_OK, AUTH_FAILED, CONNECTED, DISCONNECTED, TIMEOUT, RECONNECTING, ERROR
    }

    data class ConnectionEvent(
        val timestamp: Long,
        val type: EventType,
        val detail: String
    )

    private const val MAX_EVENTS = 100
    private const val PERSIST_COUNT = 50
    private const val PREFS_KEY = "connection_event_log"
    private const val PREFS_NAME = "connection_diagnostics"

    private val events = ArrayDeque<ConnectionEvent>(MAX_EVENTS)

    fun log(type: EventType, detail: String = "") {
        synchronized(events) {
            if (events.size >= MAX_EVENTS) {
                events.removeFirst()
            }
            events.addLast(ConnectionEvent(System.currentTimeMillis(), type, detail))
        }
    }

    fun getEvents(): List<ConnectionEvent> {
        synchronized(events) {
            return events.toList().reversed()
        }
    }

    fun persistToPrefs(context: Context) {
        val arr = JSONArray()
        synchronized(events) {
            val toPersist = events.toList().takeLast(PERSIST_COUNT)
            for (event in toPersist) {
                arr.put(JSONObject().apply {
                    put("t", event.timestamp)
                    put("type", event.type.name)
                    put("d", event.detail)
                })
            }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, arr.toString())
            .apply()
    }

    fun restoreFromPrefs(context: Context) {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_KEY, null) ?: return
        try {
            val arr = JSONArray(json)
            synchronized(events) {
                events.clear()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    events.addLast(
                        ConnectionEvent(
                            timestamp = obj.getLong("t"),
                            type = EventType.valueOf(obj.getString("type")),
                            detail = obj.optString("d", "")
                        )
                    )
                }
            }
        } catch (_: Exception) { /* corrupt data, ignore */ }
    }
}
