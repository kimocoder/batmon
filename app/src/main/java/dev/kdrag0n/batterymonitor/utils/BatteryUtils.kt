package dev.kdrag0n.batterymonitor.utils

import android.content.Context
import android.os.BatteryManager
import androidx.core.content.getSystemService

fun Context.getBatteryLevel(): Double {
    val bm = getSystemService<BatteryManager>()
    return bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()
}