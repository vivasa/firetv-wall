package com.clock.firetv

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class NsdRegistration(
    private val context: Context,
    private val deviceIdentity: DeviceIdentity
) {
    companion object {
        private const val TAG = "NsdRegistration"
        private const val SERVICE_TYPE = "_firetvclock._tcp"
    }

    private var nsdManager: NsdManager? = null
    private var registered = false

    private val registrationListener = object : NsdManager.RegistrationListener {
        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "NSD service registered: ${serviceInfo.serviceName}")
            registered = true
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "NSD registration failed: $errorCode")
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "NSD service unregistered")
            registered = false
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "NSD unregistration failed: $errorCode")
        }
    }

    fun register(port: Int) {
        try {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = deviceIdentity.deviceName
                serviceType = SERVICE_TYPE
                setPort(port)
                setAttribute("deviceId", deviceIdentity.deviceId)
                setAttribute("name", deviceIdentity.deviceName)
                setAttribute("version", "1")
            }

            nsdManager = (context.getSystemService(Context.NSD_SERVICE) as? NsdManager)?.also { mgr ->
                mgr.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register NSD service", e)
        }
    }

    fun unregister() {
        if (registered) {
            try {
                nsdManager?.unregisterService(registrationListener)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister NSD service", e)
            }
        }
    }
}
