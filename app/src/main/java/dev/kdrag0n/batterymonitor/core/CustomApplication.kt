package dev.kdrag0n.batterymonitor.core

import android.app.Application
import dev.kdrag0n.batterymonitor.BuildConfig
import timber.log.Timber

class CustomApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}