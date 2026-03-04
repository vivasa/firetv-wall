package com.mantle.app

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MantleActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MantleActivity"
    }

    private val homeFragment = HomeFragment()
    private val musicFragment = MusicFragment()
    private val tvFragment = TvFragment()
    private var activeFragment: Fragment = homeFragment

    private val connectionManager get() = MantleApp.instance.connectionManager
    private val deviceStore get() = MantleApp.instance.deviceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragmentContainer, tvFragment, "tv").hide(tvFragment)
                .add(R.id.fragmentContainer, musicFragment, "music").hide(musicFragment)
                .add(R.id.fragmentContainer, homeFragment, "home")
                .commit()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            val target = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_music -> musicFragment
                R.id.nav_tv -> tvFragment
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(target)
                .commit()
            activeFragment = target
            true
        }

        // Home tab is default (already selected in layout)
    }

    override fun onResume() {
        super.onResume()
        // Auto-connect to last device on resume
        if (connectionManager.state == TvConnectionManager.ConnectionState.DISCONNECTED) {
            tryAutoConnect()
        }
    }

    private fun tryAutoConnect() {
        if (connectionManager.state != TvConnectionManager.ConnectionState.DISCONNECTED) return
        val lastDevice = deviceStore.getLastConnectedDevice() ?: return
        Log.d(TAG, "Auto-connecting to ${lastDevice.deviceName} (${lastDevice.host}:${lastDevice.port})")
        connectionManager.connect(lastDevice.host, lastDevice.port, lastDevice.token)
    }

    fun switchToTv() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_tv
    }
}
