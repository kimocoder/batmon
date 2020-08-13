package dev.kdrag0n.batterymonitor.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import dev.kdrag0n.batterymonitor.R
import dev.kdrag0n.batterymonitor.data.BatteryUsageFraction
import dev.kdrag0n.batterymonitor.utils.formatDurationNs
import net.grandcentrix.tray.AppPreferences

class HomeFragment : Fragment() {
    private val model: HomeViewModel by viewModels()
    private lateinit var prefs: AppPreferences

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        // TODO: load value asynchronously
        prefs = AppPreferences(context)
        model.activeUsage.postValue(BatteryUsageFraction.loadFromTray(prefs, "active"))
        model.idleUsage.postValue(BatteryUsageFraction.loadFromTray(prefs, "idle"))

        model.activeUsage.observe(viewLifecycleOwner, Observer { frac ->
            val percentText = root.findViewById<TextView>(R.id.text_active_drain_percent)
            percentText.text = getString(R.string.percent_per_hour, frac.perHour())

            val timeText = root.findViewById<TextView>(R.id.text_active_drain_time)
            val durationFormatted = context?.formatDurationNs(frac.timeNs)
            timeText.text = getString(R.string.in_duration, durationFormatted)
        })

        model.idleUsage.observe(viewLifecycleOwner, Observer { frac ->
            val percentText = root.findViewById<TextView>(R.id.text_idle_drain_percent)
            percentText.text = getString(R.string.percent_per_hour, frac.perHour())

            val timeText = root.findViewById<TextView>(R.id.text_idle_drain_time)
            val durationFormatted = context?.formatDurationNs(frac.timeNs)
            timeText.text = getString(R.string.in_duration, durationFormatted)
        })

        return root
    }
}