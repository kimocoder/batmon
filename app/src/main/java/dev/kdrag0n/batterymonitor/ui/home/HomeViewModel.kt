package dev.kdrag0n.batterymonitor.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.kdrag0n.batterymonitor.data.BatteryUsageFraction

class HomeViewModel : ViewModel() {
    val activeUsage = MutableLiveData<BatteryUsageFraction>()
    val idleUsage = MutableLiveData<BatteryUsageFraction>()
}