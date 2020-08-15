package dev.kdrag0n.batterymonitor.utils

import android.content.Context
import android.os.BatteryManager
import androidx.core.content.getSystemService
import com.topjohnwu.superuser.Shell
import dev.kdrag0n.batterymonitor.data.PsyProperty
import timber.log.Timber
import kotlin.math.roundToInt

private val rawCapacityProps = arrayOf(
    // Maxim MAX1720X companion PMIC on Pixel 3 (requires kernel patch)
    PsyProperty("maxfg", "capacity_raw"),
    // Battery Monitor System (fuel gauge) on Qualcomm Snapdragon PMICs
    PsyProperty("bms", "capacity_raw"),
    // Charging battery node (SMB) on Qualcomm Snapdragon PMICs
    PsyProperty("battery", "capacity_raw"),
    // Maxim PMIC on Samsung Exynos devices
    PsyProperty("battery", "batt_read_raw_soc")
)

private val rawCapacityLimits = arrayOf(
    100,
    255,
    10000,
    25600,
    65535
)

// 5%
private val rawCapacityMargin = 0.02

class BatteryReader(val context: Context) {
    // These are expensive to populate and should never change for the lifetime of this object
    private val rawCapacityProp: PsyProperty? by lazy {
        findRawCapacityProp()
    }

    private val maxRawCapacity: Int by lazy {
        findMaxRawCapacity()
    }

    private fun findRawCapacityProp(): PsyProperty? {
        for (prop in rawCapacityProps) {
            Timber.v("Checking for ${prop.path}...")
            if (prop.exists()) {
                Timber.v("Attempting to read ${prop.path}...")
                val value = prop.read()

                if (value > 0) {
                    Timber.v("Value of ${prop.path} appears to be sane ($value), using property")
                    return prop
                } else {
                    Timber.v("Value of ${prop.path} appears to be invalid (zero or negative: $value), skipping")
                }
            }
        }

        return null
    }

    private fun findMaxRawCapacity(): Int {
        val rawCapacity = rawCapacityProp?.read() ?: return 0
        val capacity = getLevelAndroid()

        val newMax = (rawCapacity / (capacity / 100)).roundToInt()
        Timber.d("Calculated max raw capacity: $newMax")
        return newMax
    }

    private fun getLevelAndroid(): Double {
        val bm = context.getSystemService<BatteryManager>()
        return bm!!.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()
    }

    private fun getLevelRaw(): Double? {
        val rawCapacity = rawCapacityProp?.read() ?: return null
        return rawCapacity.toDouble() / maxRawCapacity * 100
    }

    fun getLevel(): Double {
        // First, attempt to read the raw level if root is available
        // Fall back to requesting capacity from Android if no root or raw fails
        return try {
            if (Shell.rootAccess()) {
                getLevelRaw()
            } else {
                null
            } ?: getLevelAndroid()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get raw battery level")
            getLevelAndroid()
        }
    }
}