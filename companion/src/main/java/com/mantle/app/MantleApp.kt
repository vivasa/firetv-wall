package com.mantle.app

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MantleApp : Application() {

    lateinit var configStore: MantleConfigStore
        private set

    lateinit var connectionManager: TvConnectionManager
        private set

    lateinit var deviceStore: DeviceStore
        private set

    lateinit var configSyncManager: ConfigSyncManager
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this
        configStore = MantleConfigStore(this, appScope)
        deviceStore = DeviceStore(this)
        connectionManager = TvConnectionManager(appScope)
        connectionManager.appContext = this
        configSyncManager = ConfigSyncManager(configStore, appScope) { json ->
            connectionManager.send(json)
        }
        configStore.addListener(configSyncManager)
        appScope.launch {
            connectionManager.connectionState.collect { state ->
                configSyncManager.setConnected(state == TvConnectionManager.ConnectionState.CONNECTED)
            }
        }
    }

    companion object {
        lateinit var instance: MantleApp
            private set
    }
}
