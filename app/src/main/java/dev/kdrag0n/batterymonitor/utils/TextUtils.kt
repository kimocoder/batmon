package dev.kdrag0n.batterymonitor.utils

import android.content.Context
import dev.kdrag0n.batterymonitor.R

const val NS_PER_SEC = 1e9

fun Context.formatDurationNs(ns: Long): String {
    val us = ns / 1000
    val ms = us / 1000
    val s = ms / 1000
    val m = s / 60
    val h = m / 60
    val d = h / 24

    if (d >= 1) {
        val remH = h % 24
        return getString(R.string.duration_format_days_hours, d, remH)
    }

    if (h >= 1) {
        val remM = m % 60
        return getString(R.string.duration_format_hours_minutes, h, remM)
    }

    if (m >= 1) {
        val remS = s % 60
        return getString(R.string.duration_format_minutes_seconds, m, remS)
    }

    if (s >= 1) {
        return getString(R.string.duration_format_seconds, s)
    }

    if (ms >= 1) {
        return getString(R.string.duration_format_milliseconds, ms)
    }

    if (us >= 1) {
        return getString(R.string.duration_format_microseconds, us)
    }

    return getString(R.string.duration_format_nanoseconds, ns)
}