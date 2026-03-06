package com.mantle.app

import android.content.Context
import com.firetv.protocol.ProtocolConfig
import org.json.JSONArray
import org.json.JSONObject

class DeviceStore(context: Context) {

    private val prefs = context.getSharedPreferences("companion_devices", Context.MODE_PRIVATE)

    data class PairedDevice(
        val deviceId: String,
        val deviceName: String,
        val token: String,
        val host: String,
        val port: Int,
        val lastConnected: Long = 0
    )

    fun getPairedDevices(): List<PairedDevice> {
        val json = prefs.getString("paired_devices", "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PairedDevice(
                    deviceId = obj.getString("deviceId"),
                    deviceName = obj.getString("deviceName"),
                    token = obj.getString("token"),
                    host = obj.getString("host"),
                    port = obj.optInt("port", ProtocolConfig.DEFAULT_PORT),
                    lastConnected = obj.optLong("lastConnected", 0)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addDevice(device: PairedDevice) {
        val devices = getPairedDevices().toMutableList()
        devices.removeAll { it.deviceId == device.deviceId }
        devices.add(device)
        save(devices)
    }

    fun removeDevice(deviceId: String) {
        val devices = getPairedDevices().toMutableList()
        devices.removeAll { it.deviceId == deviceId }
        save(devices)
    }

    fun updateLastConnected(deviceId: String) {
        val devices = getPairedDevices().toMutableList()
        val idx = devices.indexOfFirst { it.deviceId == deviceId }
        if (idx >= 0) {
            devices[idx] = devices[idx].copy(lastConnected = System.currentTimeMillis())
            save(devices)
        }
    }

    fun getLastConnectedDevice(): PairedDevice? {
        return getPairedDevices().maxByOrNull { it.lastConnected }
    }

    private fun save(devices: List<PairedDevice>) {
        val arr = JSONArray()
        devices.forEach { d ->
            arr.put(JSONObject().apply {
                put("deviceId", d.deviceId)
                put("deviceName", d.deviceName)
                put("token", d.token)
                put("host", d.host)
                put("port", d.port)
                put("lastConnected", d.lastConnected)
            })
        }
        prefs.edit().putString("paired_devices", arr.toString()).apply()
    }
}
