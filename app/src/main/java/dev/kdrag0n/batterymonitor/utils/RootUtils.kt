package dev.kdrag0n.batterymonitor.utils

import com.topjohnwu.superuser.Shell

fun checkRootAccess(): Boolean {
    val result = Shell.su("whoami").exec()
    return result.isSuccess && result.out.any { "root" in it }
}