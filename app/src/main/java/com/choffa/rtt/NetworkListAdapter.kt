package com.choffa.rtt

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

/**
 * Created by Choffa for RTT on 06-Apr-20.
 * On permission can be used outside RTT.
 */
class NetworkListAdapter(
    private val networks: List<NetworkList.Network>
) : RecyclerView.Adapter<NetworkListAdapter.ViewHolder>() {

    class ViewHolder(val row: ConstraintLayout) : RecyclerView.ViewHolder(row)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val row = LayoutInflater.from(parent.context)
            .inflate(R.layout.network_row, parent, false) as ConstraintLayout

        return ViewHolder(row)
    }

    override fun getItemCount(): Int {
        return networks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val network = networks[position]
        val checkbox = holder.row.findViewById<CheckBox>(R.id.checkBox)
        checkbox.isChecked = network.added

        checkbox.setOnCheckedChangeListener { _, isChecked -> network.added = isChecked }

        holder.row.setOnClickListener {
            checkbox.toggle()
        }

        holder.row.findViewById<TextView>(R.id.networkName).text = network.scanResult.SSID
        holder.row.findViewById<TextView>(R.id.networkId).text = network.scanResult.BSSID
    }

}