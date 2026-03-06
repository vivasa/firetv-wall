package com.clock.firetv

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import org.json.JSONObject

class BlePeripheralManager(
    private val context: Context,
    private val commandHandler: CompanionCommandHandler,
    private val deviceIdentity: DeviceIdentity
) {
    companion object {
        private const val TAG = "BlePeripheral"
    }

    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var eventChar: BluetoothGattCharacteristic? = null

    // Connected client state
    private var connectedDevice: BluetoothDevice? = null
    private var authenticated = false
    private var negotiatedMtu = BleConstants.DEFAULT_MTU
    private var subscribedToEvents = false

    // Fragmentation
    private val reassembler = BleFragmenter.Reassembler()

    private val transportSink = object : CompanionCommandHandler.TransportSink {
        override fun sendEvent(json: JSONObject) {
            sendNotification(json)
        }

        override fun closeConnection(reason: String) {
            connectedDevice?.let { device ->
                gattServer?.cancelConnection(device)
            }
        }
    }

    fun init(): Boolean {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            Log.w(TAG, "BluetoothAdapter not available")
            return false
        }

        val bleAdvertiser = btAdapter.bluetoothLeAdvertiser
        if (bleAdvertiser == null) {
            Log.w(TAG, "BLE advertising not supported on this device")
            return false
        }

        advertiser = bleAdvertiser

        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (btManager == null) {
            Log.w(TAG, "BluetoothManager not available")
            return false
        }

        try {
            gattServer = btManager.openGattServer(context, gattCallback)
            setupGattService()
            startAdvertising()
            Log.i(TAG, "BLE peripheral initialized successfully")
            return true
        } catch (e: SecurityException) {
            Log.w(TAG, "BLE permission denied: ${e.message}")
            return false
        }
    }

    fun destroy() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Stop advertising permission error: ${e.message}")
        }
        try {
            gattServer?.close()
        } catch (e: Exception) {
            Log.w(TAG, "GATT server close error: ${e.message}")
        }
        gattServer = null
        advertiser = null
        connectedDevice = null
        authenticated = false
    }

    private fun setupGattService() {
        val service = BluetoothGattService(
            BleConstants.SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Command characteristic: Write With Response
        val cmdChar = BluetoothGattCharacteristic(
            BleConstants.COMMAND_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // Event characteristic: Notify
        val evtChar = BluetoothGattCharacteristic(
            BleConstants.EVENT_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        // CCC descriptor for notification subscription
        val cccDescriptor = BluetoothGattDescriptor(
            BleConstants.CCC_DESCRIPTOR_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        evtChar.addDescriptor(cccDescriptor)

        service.addCharacteristic(cmdChar)
        service.addCharacteristic(evtChar)

        commandChar = cmdChar
        eventChar = evtChar

        try {
            gattServer?.addService(service)
        } catch (e: SecurityException) {
            Log.w(TAG, "Add service permission error: ${e.message}")
        }
    }

    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        // Advertisement data: service UUID only (fits in 31 bytes)
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()

        // Scan response: manufacturer data with short deviceId hash
        // Use first 8 chars of deviceId to keep within 31-byte limit
        val shortId = deviceIdentity.deviceId.take(8).toByteArray(Charsets.UTF_8)

        val scanResponse = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addManufacturerData(BleConstants.MANUFACTURER_ID, shortId)
            .build()

        try {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            // Set BT adapter name (truncated to fit advertisement)
            val shortName = deviceIdentity.deviceName.take(8)
            try {
                btAdapter?.name = shortName
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot set BT name: ${e.message}")
            }
            advertiser?.startAdvertising(settings, advertiseData, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            Log.w(TAG, "Start advertising permission error: ${e.message}")
        }
    }

    private fun sendNotification(json: JSONObject) {
        val device = connectedDevice ?: return
        val evtChar = eventChar ?: return
        val server = gattServer ?: return

        if (!subscribedToEvents) return

        val data = json.toString().toByteArray(Charsets.UTF_8)
        val fragments = BleFragmenter.fragment(data, negotiatedMtu)

        for (fragment in fragments) {
            evtChar.value = fragment
            try {
                server.notifyCharacteristicChanged(device, evtChar, false)
            } catch (e: SecurityException) {
                Log.w(TAG, "Notify permission error: ${e.message}")
                return
            }
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "BLE advertising started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: errorCode=$errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            try {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    Log.i(TAG, "BLE client connected: ${device.address}")
                    // Disconnect previous client if any
                    connectedDevice?.let { prev ->
                        if (prev.address != device.address) {
                            Log.i(TAG, "Replacing previous BLE client: ${prev.address}")
                            try {
                                gattServer?.cancelConnection(prev)
                            } catch (e: SecurityException) { /* ignore */ }
                        }
                    }
                    connectedDevice = device
                    authenticated = false
                    subscribedToEvents = false
                    negotiatedMtu = BleConstants.DEFAULT_MTU
                    reassembler.reset()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    Log.i(TAG, "BLE client disconnected: ${device.address}")
                    if (connectedDevice?.address == device.address) {
                        if (authenticated) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                commandHandler.listener?.onCompanionDisconnected()
                            }
                        }
                        connectedDevice = null
                        authenticated = false
                        subscribedToEvents = false
                        reassembler.reset()
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Connection state change permission error: ${e.message}")
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            Log.d(TAG, "MTU changed to $mtu for ${device.address}")
            negotiatedMtu = mtu
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (characteristic.uuid == BleConstants.COMMAND_CHAR_UUID && value != null) {
                // Send GATT success response
                if (responseNeeded) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Send response permission error: ${e.message}")
                    }
                }

                // Reassemble fragments
                val completeMessage = reassembler.addFragment(value)
                if (completeMessage != null) {
                    handleCompleteMessage(completeMessage)
                }
            } else {
                if (responseNeeded) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    } catch (e: SecurityException) { /* ignore */ }
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            if (descriptor.uuid == BleConstants.CCC_DESCRIPTOR_UUID) {
                subscribedToEvents = value?.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == true
                Log.d(TAG, "Event notifications ${if (subscribedToEvents) "enabled" else "disabled"}")
                if (responseNeeded) {
                    try {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    } catch (e: SecurityException) { /* ignore */ }
                }
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            if (descriptor.uuid == BleConstants.CCC_DESCRIPTOR_UUID) {
                val value = if (subscribedToEvents)
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (e: SecurityException) { /* ignore */ }
            }
        }
    }

    private fun handleCompleteMessage(text: String) {
        try {
            val json = JSONObject(text)
            val cmd = json.optString("cmd", "")
            val becameAuthenticated = commandHandler.handleCommand(cmd, json, transportSink, authenticated)
            if (becameAuthenticated) {
                authenticated = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Invalid BLE message: $text", e)
            transportSink.sendEvent(JSONObject().apply {
                put("evt", "error")
                put("message", "invalid message format")
            })
        }
    }
}
