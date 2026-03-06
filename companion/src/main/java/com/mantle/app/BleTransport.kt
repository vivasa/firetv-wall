package com.mantle.app

import com.firetv.protocol.BleConstants
import com.firetv.protocol.BleFragmenter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import org.json.JSONObject

class BleTransport(
    private val context: Context,
    private val device: BluetoothDevice
) : CompanionTransport {

    companion object {
        private const val TAG = "BleTransport"
    }

    private var listener: CompanionTransport.Listener? = null
    private var gatt: BluetoothGatt? = null
    private var commandChar: BluetoothGattCharacteristic? = null
    private var eventChar: BluetoothGattCharacteristic? = null
    private var negotiatedMtu = BleConstants.DEFAULT_MTU

    private val reassembler = BleFragmenter.Reassembler()

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "GATT connected, discovering services")
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    Log.e(TAG, "Discover services permission denied", e)
                    listener?.onError("permission_denied: ${e.message}")
                }
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                Log.d(TAG, "GATT disconnected, status=$status")
                commandChar = null
                eventChar = null
                reassembler.reset()
                this@BleTransport.gatt = null
                listener?.onDisconnected(if (status == BluetoothGatt.GATT_SUCCESS) "disconnected" else "gatt_error_$status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Service discovery failed: $status")
                listener?.onError("service_discovery_failed: $status")
                return
            }

            val service = gatt.getService(BleConstants.SERVICE_UUID)
            if (service == null) {
                Log.e(TAG, "Service 0xFF01 not found")
                listener?.onError("service_not_found")
                return
            }

            commandChar = service.getCharacteristic(BleConstants.COMMAND_CHAR_UUID)
            eventChar = service.getCharacteristic(BleConstants.EVENT_CHAR_UUID)

            if (commandChar == null || eventChar == null) {
                Log.e(TAG, "Required characteristics not found")
                listener?.onError("characteristics_not_found")
                return
            }

            // Request MTU negotiation
            try {
                gatt.requestMtu(BleConstants.TARGET_MTU)
            } catch (e: SecurityException) {
                Log.w(TAG, "MTU request permission denied", e)
                // Continue with default MTU
                subscribeToEvents(gatt)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            negotiatedMtu = if (status == BluetoothGatt.GATT_SUCCESS) mtu else BleConstants.DEFAULT_MTU
            Log.d(TAG, "MTU negotiated: $negotiatedMtu")
            subscribeToEvents(gatt)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleConstants.EVENT_CHAR_UUID) {
                val value = characteristic.value ?: return
                val completeMessage = reassembler.addFragment(value)
                if (completeMessage != null) {
                    listener?.onMessage(completeMessage)
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Characteristic write failed: $status")
            }
            // Process next fragment in queue if any
            synchronized(writeQueue) {
                writeQueue.removeFirstOrNull()
                val next = writeQueue.firstOrNull()
                if (next != null) {
                    writeFragment(gatt, next)
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (descriptor.uuid == BleConstants.CCC_DESCRIPTOR_UUID) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Event notifications subscribed, connection ready")
                    listener?.onConnected()
                } else {
                    Log.e(TAG, "Failed to subscribe to notifications: $status")
                    listener?.onError("notification_subscribe_failed: $status")
                }
            }
        }
    }

    private val writeQueue = ArrayDeque<ByteArray>()

    override fun connect() {
        try {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            Log.e(TAG, "Connect permission denied", e)
            listener?.onError("permission_denied: ${e.message}")
        }
    }

    override fun disconnect() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: SecurityException) {
            Log.w(TAG, "Disconnect permission denied", e)
        }
        gatt = null
        commandChar = null
        eventChar = null
        reassembler.reset()
    }

    override fun send(json: JSONObject) {
        val cmdChar = commandChar
        val g = gatt
        if (cmdChar == null || g == null) {
            Log.w(TAG, "Cannot send, not connected")
            return
        }

        val data = json.toString().toByteArray(Charsets.UTF_8)
        val fragments = BleFragmenter.fragment(data, negotiatedMtu)

        synchronized(writeQueue) {
            val wasEmpty = writeQueue.isEmpty()
            writeQueue.addAll(fragments)
            if (wasEmpty) {
                val first = writeQueue.firstOrNull() ?: return
                writeFragment(g, first)
            }
        }
    }

    override fun setListener(listener: CompanionTransport.Listener) {
        this.listener = listener
    }

    private fun subscribeToEvents(gatt: BluetoothGatt) {
        val evtChar = eventChar ?: return
        try {
            gatt.setCharacteristicNotification(evtChar, true)
            val descriptor = evtChar.getDescriptor(BleConstants.CCC_DESCRIPTOR_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        } catch (e: SecurityException) {
            Log.e(TAG, "Subscribe permission denied", e)
            listener?.onError("permission_denied: ${e.message}")
        }
    }

    private fun writeFragment(gatt: BluetoothGatt, fragment: ByteArray) {
        val cmdChar = commandChar ?: return
        cmdChar.value = fragment
        cmdChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        try {
            gatt.writeCharacteristic(cmdChar)
        } catch (e: SecurityException) {
            Log.w(TAG, "Write permission denied", e)
        }
    }
}
