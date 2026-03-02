package com.clock.firetv.companion

import android.app.Application

class CompanionApp : Application() {

    lateinit var connectionManager: TvConnectionManager
        private set

    lateinit var deviceStore: DeviceStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        deviceStore = DeviceStore(this)
        connectionManager = TvConnectionManager()
    }

    companion object {
        lateinit var instance: CompanionApp
            private set
    }
}
