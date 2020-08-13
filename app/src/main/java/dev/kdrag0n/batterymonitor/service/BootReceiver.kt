package dev.kdrag0n.batterymonitor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Make sure other apps can't spoof this intent
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        MonitorService.start(context ?: return)
    }
}