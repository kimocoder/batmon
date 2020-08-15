package dev.kdrag0n.batterymonitor.utils

import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.io.SuFile
import java.io.IOException

fun SuFile.readText(): String {
    val result = Shell.su("cat '$absolutePath'").exec()

    if (!result.isSuccess) {
        throw IOException(result.err.joinToString(""))
    }

    return result.out.joinToString("")
}