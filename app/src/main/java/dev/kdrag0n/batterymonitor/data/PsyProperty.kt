package dev.kdrag0n.batterymonitor.data

import com.topjohnwu.superuser.io.SuFile
import dev.kdrag0n.batterymonitor.utils.readText

data class PsyProperty(val supply: String, val property: String) {
    val path = "/sys/class/power_supply/$supply/$property"
    private val file: SuFile by lazy {
        SuFile(path)
    }

    fun exists() = file.exists()
    fun read() = file.readText().toLong()
}