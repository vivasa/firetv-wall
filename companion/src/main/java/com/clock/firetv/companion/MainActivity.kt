package com.clock.firetv.companion

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val devicesFragment = DevicesFragment()
    private val remoteFragment = RemoteFragment()
    private val settingsFragment = SettingsFragment()
    private var activeFragment: Fragment = devicesFragment

    private val connectionManager get() = CompanionApp.instance.connectionManager
    private val deviceStore get() = CompanionApp.instance.deviceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, settingsFragment, "settings").hide(settingsFragment)
                .add(R.id.fragmentContainer, remoteFragment, "remote").hide(remoteFragment)
                .add(R.id.fragmentContainer, devicesFragment, "devices")
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_devices -> devicesFragment
                R.id.nav_remote -> remoteFragment
                R.id.nav_settings -> settingsFragment
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
            true
        }

        // Auto-connect to last device on launch
        tryAutoConnect()
    }

    override fun onResume() {
        super.onResume()
        // Reconnect if we were previously connected but lost connection
        if (connectionManager.state == TvConnectionManager.ConnectionState.DISCONNECTED) {
            tryAutoConnect()
        }
    }

    private fun tryAutoConnect() {
        if (connectionManager.state != TvConnectionManager.ConnectionState.DISCONNECTED) return
        val lastDevice = deviceStore.getLastConnectedDevice() ?: return
        Log.d(TAG, "Auto-connecting to ${lastDevice.deviceName} (${lastDevice.host}:${lastDevice.port})")
        connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
        // Switch to remote tab to show "Connecting..." state
        switchToRemote()
    }

    fun switchToRemote() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_remote
    }
}
