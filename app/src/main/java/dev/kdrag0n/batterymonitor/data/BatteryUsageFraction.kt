package dev.kdrag0n.batterymonitor.data

import dev.kdrag0n.batterymonitor.utils.NS_PER_SEC
import dev.kdrag0n.batterymonitor.utils.getDouble
import dev.kdrag0n.batterymonitor.utils.put
import net.grandcentrix.tray.AppPreferences

data class BatteryUsageFraction(var usage: Double = 0.0, var timeNs: Long = 0) {
    fun perHour(): Double {
        if (timeNs == 0L) {
            return 0.0
        }

        return usage / (timeNs / NS_PER_SEC / 60 / 60)
    }

    fun saveToTray(prefs: AppPreferences, name: String) {
        prefs.put("battery_fraction_${name}_usage", usage)
        prefs.put("battery_fraction_${name}_time_ns", timeNs)
    }

    companion object {
        fun loadFromTray(prefs: AppPreferences, name: String): BatteryUsageFraction {
            val usage = prefs.getDouble("battery_fraction_${name}_usage", 0.0)
            val timeNs = prefs.getLong("battery_fraction_${name}_time_ns", 0)

            return BatteryUsageFraction(usage = usage, timeNs = timeNs)
        }
    }
}