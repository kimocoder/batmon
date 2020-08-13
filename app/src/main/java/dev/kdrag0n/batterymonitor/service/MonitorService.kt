package dev.kdrag0n.batterymonitor.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dev.kdrag0n.batterymonitor.R
import dev.kdrag0n.batterymonitor.ui.MainActivity
import timber.log.Timber

private const val STATUS_NOTIFICATION_ID = 1
private const val STATUS_CHANNEL_ID = "monitor_service_status_channel"

class MonitorService : Service() {
    private var started = false
    private lateinit var receiver: EventReceiver

    private inner class EventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("Received intent: ${intent?.action}")
        }
    }

    private fun registerEventReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }

        receiver = EventReceiver()
        registerReceiver(receiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.monitor_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(STATUS_CHANNEL_ID, name, importance)
            channel.description = getString(R.string.monitor_notification_channel_description)

            getSystemService<NotificationManager>()?.createNotificationChannel(channel)
        }
    }

    private fun showForegroundNotification() {
        // Launch MainActivity when the notification is tapped
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        val notification = NotificationCompat.Builder(this, STATUS_CHANNEL_ID)
            .setContentTitle(getText(R.string.monitor_notification_title))
            .setSmallIcon(R.drawable.ic_dashboard_black_24dp)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.monitor_notification_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(STATUS_NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Avoid initializing the service multiple times
        if (!started) {
            started = true

            Timber.d("Starting...")
            createNotificationChannel()
            showForegroundNotification()
            registerEventReceiver()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        unregisterReceiver(receiver)
    }

    // Disallow binding since this service should run independently
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun start(context: Context) {
            Intent(context, MonitorService::class.java).also {
                ContextCompat.startForegroundService(context, it)
            }
        }
    }
}