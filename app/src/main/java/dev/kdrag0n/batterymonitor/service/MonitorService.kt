package dev.kdrag0n.batterymonitor.service

import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dev.kdrag0n.batterymonitor.R
import dev.kdrag0n.batterymonitor.data.BatteryUsageFraction
import dev.kdrag0n.batterymonitor.ui.MainActivity
import dev.kdrag0n.batterymonitor.utils.BatteryReader
import net.grandcentrix.tray.AppPreferences
import timber.log.Timber

private const val STATUS_NOTIFICATION_ID = 1
private const val STATUS_CHANNEL_ID = "monitor_service_status_channel"

class MonitorService : Service() {
    // Base service
    private var started = false
    private lateinit var receiver: EventReceiver
    private lateinit var prefs: AppPreferences
    private val batteryReader = BatteryReader(this)

    // Monitoring state
    private var lastScreenState = true
    private var lastBatteryLevel = -1.0
    private var lastStateTime = -1L
    private var isPowered = false
    private lateinit var activeUsage: BatteryUsageFraction
    private lateinit var idleUsage: BatteryUsageFraction

    private inner class EventReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Timber.d("Received intent: ${intent?.action}")

            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (!isPowered) {
                        setState(true)
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (!isPowered) {
                        setState(false)
                    }
                }
                Intent.ACTION_POWER_CONNECTED -> {
                    isPowered = true
                    refreshState()
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    isPowered = false
                    refreshState()
                    // Start tracking again with fresh values
                    // We can't use the old ones or usage will become negative
                    initState()
                }
                Intent.ACTION_SHUTDOWN -> {
                    if (!isPowered) {
                        // Save what we have as there's no guarantee that nothing else has happened
                        // since the last time we were up
                        refreshState()
                    }
                }
            }

            Timber.i("Usage stats: ${activeUsage.perHour()}%/h active, ${idleUsage.perHour()}%/h idle")
        }
    }

    private fun updateLastState(newScreenState: Boolean,
                                newBatteryLevel: Double = batteryReader.getLevel(),
                                newTime: Long = SystemClock.elapsedRealtimeNanos()) {
        lastScreenState = newScreenState
        lastBatteryLevel = newBatteryLevel
        lastStateTime = newTime

        Timber.v("New state: screenOn=$newScreenState battery=$lastBatteryLevel time=$lastStateTime")
    }

    @SuppressLint("TimberArgCount")
    private fun setState(newScreenState: Boolean) {
        // Retrieve values first in case our accounting happens to drain 1% of battery
        val newBatteryLevel = batteryReader.getLevel()
        val newTime = SystemClock.elapsedRealtimeNanos()

        // This calculation is reversed to account for drain being negative
        val drainedPct = lastBatteryLevel - newBatteryLevel
        val elapsedNs = newTime - lastStateTime

        val blamedUsage = if (lastScreenState) {
            val newUsage = activeUsage.add(drainedPct, elapsedNs)
            activeUsage = newUsage
            newUsage
        } else {
            val newUsage = idleUsage.add(drainedPct, elapsedNs)
            idleUsage = newUsage
            newUsage
        }

        Timber.v("Blamed ${if (lastScreenState) "active" else "idle"} state for using $drainedPct% of battery in $elapsedNs ns")
        Timber.v("Blamed usage fraction: $blamedUsage")

        updateLastState(newScreenState, newBatteryLevel, newTime)
        activeUsage.saveToTray(prefs, "active")
        idleUsage.saveToTray(prefs, "idle")
    }

    private fun refreshState() {
        // Setting the same state causes usage to be updated without causing a state change
        setState(lastScreenState)
    }

    private fun initState() {
        // Restart "last state" tracking after charging or service restart
        updateLastState(getSystemService<PowerManager>()!!.isInteractive)
    }

    private fun setupEventReceiver() {
        // Create receiver and initialize state
        receiver = EventReceiver()
        // Initialize last state tracking with fresh values
        // We can't save and restore these values because we can't find out what happened during
        // the downtime -- battery died? device crashed? battery was drained by another OS?
        initState()
        isPowered = batteryReader.isPowerConnected()

        // Register the receiver now that initialization is finished
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SHUTDOWN)
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

    override fun onCreate() {
        prefs = AppPreferences(this)
        activeUsage = BatteryUsageFraction.loadFromTray(prefs, "active")
        idleUsage = BatteryUsageFraction.loadFromTray(prefs, "idle")
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