package dev.kdrag0n.batterymonitor.utils

import net.grandcentrix.tray.AppPreferences
import net.grandcentrix.tray.core.ItemNotFoundException

fun AppPreferences.put(key: String, value: Double): Boolean {
    return put(key, java.lang.Double.doubleToRawLongBits(value))
}

fun AppPreferences.getDouble(key: String): Double {
    return java.lang.Double.longBitsToDouble(getLong(key))
}

fun AppPreferences.getDouble(key: String, defaultValue: Double): Double {
    return try {
        java.lang.Double.longBitsToDouble(getLong(key))
    } catch (e: ItemNotFoundException) {
        defaultValue
    }
}