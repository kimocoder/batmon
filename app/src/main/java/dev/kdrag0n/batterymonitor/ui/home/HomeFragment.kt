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

class HomeFragment : Fragment() {
    private val model: HomeViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        model.text.observe(viewLifecycleOwner, Observer {
            root.findViewById<TextView>(R.id.text_active_drain_label).text = it
        })
        
        return root
    }
}