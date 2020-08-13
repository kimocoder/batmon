package dev.kdrag0n.batterymonitor.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dev.kdrag0n.batterymonitor.R
import dev.kdrag0n.batterymonitor.data.BatteryUsageFraction
import dev.kdrag0n.batterymonitor.ui.MainActivity
import dev.kdrag0n.batterymonitor.utils.getBatteryLevel
import timber.log.Timber
import java.util.*

private const val STATUS_NOTIFICATION_ID = 1
private const val STATUS_CHANNEL_ID = "monitor_service_status_channel"

class MonitorService : Service() {
    // Base service
    private var started = false
    private lateinit var receiver: EventReceiver

    // Monitoring state
    private var lastScreenState = true
    private var lastBatteryLevel = -1.0
    private var lastStateTime = -1L
    private val activeUsage = BatteryUsageFraction()
    private val idleUsage = BatteryUsageFraction()

    private inner class EventReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("Received intent: ${intent?.action}")

            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> setState(true)
                Intent.ACTION_SCREEN_OFF -> setState(false)
            }

            Timber.i("Usage stats: ${activeUsage.perHour()}%/h active, ${idleUsage.perHour()}%/h idle")
        }
    }

    private fun updateLastState(newScreenState: Boolean,
                                newBatteryLevel: Double = getBatteryLevel(),
                                newTime: Long = SystemClock.elapsedRealtimeNanos()) {
        lastScreenState = newScreenState
        lastBatteryLevel = newBatteryLevel
        lastStateTime = newTime

        Timber.v("New state: screenOn=$newScreenState battery=$lastBatteryLevel time=$lastStateTime")
    }

    @SuppressLint("TimberArgCount")
    private fun setState(newScreenState: Boolean) {
        // Retrieve values first in case our accounting happens to drain 1% of battery
        val newBatteryLevel = getBatteryLevel()
        val newTime = SystemClock.elapsedRealtimeNanos()

        // This calculation is reversed to account for drain being negative
        val drainedPct = lastBatteryLevel - newBatteryLevel
        val elapsedNs = newTime - lastStateTime
        Timber.v("Blaming ${if (lastScreenState) "active" else "idle"} state for $drainedPct% usage in $elapsedNs ns")

        val blamedUsage = if (lastScreenState) activeUsage else idleUsage
        blamedUsage.apply {
            usage += drainedPct
            timeNs += elapsedNs
        }

        updateLastState(newScreenState, newBatteryLevel, newTime)
    }

    private fun setupEventReceiver() {
        // Create receiver and initialize state
        receiver = EventReceiver()
        updateLastState(getSystemService<PowerManager>()!!.isInteractive)

        // Register the receiver now that initialization is finished
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
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
            setupEventReceiver()
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