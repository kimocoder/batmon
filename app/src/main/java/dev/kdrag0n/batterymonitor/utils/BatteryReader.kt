package dev.kdrag0n.batterymonitor.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.getSystemService
import com.topjohnwu.superuser.Shell
import dev.kdrag0n.batterymonitor.data.PsyProperty
import timber.log.Timber
import kotlin.math.roundToInt

private val rawCapacityProps = arrayOf(
    /* OEM nodes */
    // Maxim companion PMIC on Pixel 3/4 (requires kernel patch)
    PsyProperty("maxfg", "capacity_raw"),

    /* SoC nodes */
    // Battery Monitor System (fuel gauge) on Qualcomm PMICs
    PsyProperty("bms", "capacity_raw"),
    // Battery node (SMB charger) on Qualcomm PMICs
    PsyProperty("battery", "capacity_raw"),
    // Maxim PMIC on Samsung Exynos devices
    PsyProperty("battery", "batt_read_raw_soc")
)

// List of known max raw capacity values for rounding
private val rawCapacityLimits = arrayOf(
    // If some device happens to expose raw capacity as percentage
    100,
    // Older Qualcomm PMICs (qpnp-fg-gen3 and older)
    255,
    // Newer Qualcomm PMICs (qpnp-fg-gen4 and newer, qpnp-qg) with qcom,soc-hi-res DT property
    10000,
    // Maxim companion PMIC on Pixel 3/4
    25600,
    // Qualcomm PMICs (qpnp-fg-gen3 and newer) with a kernel patch to expose hardware MSOC value
    65535
)

// +-3% error margin for rounding to known limits
private const val rawCapacityLimitMargin = 0.03

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

        val calcMax = (rawCapacity / (capacity / 100)).roundToInt()
        Timber.d("Calculated max raw capacity: $calcMax")

        for (exactMax in rawCapacityLimits) {
            val compareMin = (exactMax * (1 - rawCapacityLimitMargin)).roundToInt()
            val compareMax = (exactMax * (1 + rawCapacityLimitMargin)).roundToInt()

            Timber.v("Comparing $calcMax with known exact value $exactMax: margin ${rawCapacityLimitMargin * 100}% -> min $compareMin, max $compareMax")
            if (calcMax in compareMin..compareMax) {
                Timber.d("Rounding to known exact value: $exactMax")
                return exactMax
            }
        }

        Timber.w("Unable to find close exact value, falling back to calculated max raw capacity")
        return calcMax
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

    fun isPowerConnected(): Boolean {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)

        return when (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1) {
            BatteryManager.BATTERY_PLUGGED_AC -> true
            BatteryManager.BATTERY_PLUGGED_USB -> true
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> true
            else -> false
        }
    }
}