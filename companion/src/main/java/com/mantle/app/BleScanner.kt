package com.mantle.app

import com.firetv.protocol.BleConstants
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log

class BleScanner {

    companion object {
        private const val TAG = "BleScanner"
    }

    data class BleDevice(
        val device: BluetoothDevice,
        val deviceName: String,
        val deviceId: String
    )

    interface Listener {
        fun onDeviceFound(bleDevice: BleDevice)
    }

    var listener: Listener? = null
    private var scanner: BluetoothLeScanner? = null
    private var scanning = false

    fun startScan(): Boolean {
        if (scanning) return true

        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or not enabled")
            return false
        }

        scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            Log.w(TAG, "BLE scanner not available")
            return false
        }

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner?.startScan(listOf(filter), settings, scanCallback)
            scanning = true
            Log.d(TAG, "BLE scan started")
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE scan permission denied: ${e.message}")
            return false
        }
    }

    fun stopScan() {
        if (!scanning) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Stop scan permission denied: ${e.message}")
        }
        scanning = false
        scanner = null
        Log.d(TAG, "BLE scan stopped")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val record = result.scanRecord ?: return

            var name: String
            try {
                name = device.name ?: record.deviceName ?: "Fire TV"
            } catch (e: SecurityException) {
                name = record.deviceName ?: "Fire TV"
            }

            // Extract deviceId from manufacturer data
            val mfgData = record.getManufacturerSpecificData(BleConstants.MANUFACTURER_ID)
            val deviceId = if (mfgData != null) {
                String(mfgData, Charsets.UTF_8)
            } else {
                device.address
            }

            Log.d(TAG, "Found BLE device: $name ($deviceId)")
            listener?.onDeviceFound(BleDevice(device, name, deviceId))
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed: errorCode=$errorCode")
            scanning = false
        }
    }
}
