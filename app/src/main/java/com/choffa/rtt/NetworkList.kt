package com.choffa.rtt

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView

class NetworkList : Fragment() {

    private val wifiManager by lazy { activity!!.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private lateinit var networkListView: RecyclerView
    private var networkList: List<Network> = listOf()

    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermissions()
        } else {
            doScan()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.network_list_view, container, false)
        networkListView = view.findViewById(R.id.networkList)
        view.findViewById<Button>(R.id.nextButton)
            .setOnClickListener {
                navigate(
                    networkList
                        .filter { it.added }
                        .map { it.scanResult }
                )
            }

        view.findViewById<Button>(R.id.refreshButton)
            .setOnClickListener {
                if (!checkPermissions()) {
                    requestPermissions()
                } else {
                    doScan()
                }
            }

        return view
    }

    private fun doScan() {
        val context = context!!

        val wifiScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                context.unregisterReceiver(this)
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    Log.i(TAG, "Scan was successful")
                    showAPs()
                } else {
                    Log.i(TAG, "Scan was unsuccessful, using old results")
                    showAPs()
                }
            }
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        wifiManager.startScan()
    }

    private fun showAPs() {
        networkList = wifiManager.scanResults
            .filter { it.is80211mcResponder }
            .map { Network(it) }
        networkListView.adapter = NetworkListAdapter(networkList)
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            activity as Activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            1
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // Since we only have one dangerous permission we don't need to check
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            // Permission granted
            doScan()
        }
    }

    private fun navigate(items: List<ScanResult>) {
        val args = RttMeasurement.createBundle(items)
        findNavController().navigate(R.id.action_testFragment_to_rttMeasurement, args)
    }

    companion object {
        private const val TAG: String = "NETWORKLIST"
    }

    class Network(val scanResult: ScanResult, var added: Boolean = false)

}
