package dev.kdrag0n.batterymonitor.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat

class MonitorService : Service() {
    private var started = false

    // Disallow binding since this service should run independently
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Avoid initializing the service multiple times
        if (!started) {
            started = true

            // TODO: implement service
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val serviceIntent = Intent(context, MonitorService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}