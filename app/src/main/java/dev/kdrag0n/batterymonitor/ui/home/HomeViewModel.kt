package dev.kdrag0n.batterymonitor.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {
    val text = MutableLiveData<String>().apply {
        value = "This is home Fragment"
    }
}