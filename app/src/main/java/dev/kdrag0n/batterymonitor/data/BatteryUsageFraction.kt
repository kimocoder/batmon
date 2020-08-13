package dev.kdrag0n.batterymonitor.data

private const val NS_PER_SEC = 1e9

data class BatteryUsageFraction(var usage: Double = 0.0, var timeNs: Long = 0) {
    fun perHour(): Double {
        return usage / (timeNs / NS_PER_SEC / 60 / 60)
    }
}