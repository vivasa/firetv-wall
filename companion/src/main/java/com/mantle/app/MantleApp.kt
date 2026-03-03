package com.mantle.app

import android.app.Application

class MantleApp : Application() {

    lateinit var configStore: MantleConfigStore
        private set

    lateinit var connectionManager: TvConnectionManager
        private set

    lateinit var deviceStore: DeviceStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        configStore = MantleConfigStore(this)
        deviceStore = DeviceStore(this)
        connectionManager = TvConnectionManager()
        connectionManager.registerConfigListener()
    }

    companion object {
        lateinit var instance: MantleApp
            private set
    }
}
