package com.choffa.rtt

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.rtt.RangingRequest
import android.net.wifi.rtt.RangingResult
import android.net.wifi.rtt.RangingResultCallback
import android.net.wifi.rtt.WifiRttManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by Choffa for RTT on 08-Apr-20.
 * On permission can be used outside RTT.
 */

class RttMeasurement : Fragment() {

    private lateinit var button: Button
    private lateinit var counterView: TextView
    private lateinit var descriptionView: EditText
    private lateinit var accessPoints: List<ScanResult>

    private var rttManager: WifiRttManager? = null
    private var scanning = false
    private var count = 0
    private val results = mutableListOf<RangingResult>()
    private val handler = Handler()

    // Permission checked in NetworkList.kt
    private val rangingRunnable by lazy { object : Runnable {

        private val request = RangingRequest.Builder().run {
            accessPoints.forEach { addAccessPoint(it) }
            build()
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            rttManager?.startRanging(
                request,
                AsyncTask.THREAD_POOL_EXECUTOR,
                object : RangingResultCallback() {
                    override fun onRangingResults(res: MutableList<RangingResult>) {
                        results.addAll(res)
                        count++
                        handler.post { counterView.text = count.toString() }
                    }

                    override fun onRangingFailure(p0: Int) {
                        Log.e(TAG, "Ranging failed")
                    }
                }
            )

            handler.postDelayed(this, 1000)
        }
    }}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessPoints = (arguments!!.get("accessPoints") as? List<*>)!!.filterIsInstance<ScanResult>()
        rttManager = context!!.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as WifiRttManager?
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.rtt_view, container, false)

        button = view.findViewById(R.id.rtt_view_button)
        button.setOnClickListener() { onButtonClick() }

        counterView = view.findViewById(R.id.counterView)
        counterView.text = count.toString()

        descriptionView = view.findViewById(R.id.desc)

        return view
    }

    private fun onButtonClick() {
        if (scanning) {
            stopScan()
        } else {
            startScan()
        }

        button.text = if (scanning) "Stop and Save" else "Start"
    }

    private fun startScan() {
        if (isRttAvailable()) {
            descriptionView.isEnabled = false
            handler.post(rangingRunnable)
            scanning = true
        }
    }

    private fun stopScan() {
        handler.removeCallbacks(rangingRunnable)
        scanning = false
        writeFile(context!!.getExternalFilesDir(null)!!, descriptionView.text.toString())
        descriptionView.isEnabled = true
        count = 0
        counterView.text = count.toString()
    }

    private fun isRttAvailable(): Boolean {
        val context = context!!
        return hasFeature(context) && isAvailable(context)
    }

    private fun hasFeature(context: Context): Boolean {
        val hasFeature = context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
        if (!hasFeature) {
            AlertDialog.Builder(context).run {
                setMessage("WiFi RTT is missing on this phone")
                setPositiveButton("OK") { _, _ -> }
                create()
                show()
            }
        }
        return hasFeature
    }

    private fun isAvailable(context: Context): Boolean {
        val isAvailable = rttManager!!.isAvailable
        if (!isAvailable) {
            AlertDialog.Builder(context).run {
                setMessage("WiFi RTT is not available at the moment")
                setPositiveButton("OK") { _, _ -> }
                create()
                show()
            }
        }
        return isAvailable
    }

    private fun writeFile(dir: File, description: String) {
        val now = LocalDateTime.now()
        val filename = now.toEpochSecond(ZoneOffset.UTC).toString() + ".csv"
        val file = File(dir, filename)
        val writer = file.writer()

        writer.write(
            """
            # Publisher chriswg
            # Updated $now
            # Description $description
            mac_address,timestamp,attempted,successful,distance,std_dev,rssi
        
            """.trimIndent()
        )

        results.forEach {

            when (it.status) {
                RangingResult.STATUS_SUCCESS -> writer.write(
                    "%s,%d,%d,%d,%d,%d,%d\n".format(
                        it.macAddress,
                        it.rangingTimestampMillis,
                        it.numAttemptedMeasurements,
                        it.numSuccessfulMeasurements,
                        it.distanceMm,
                        it.distanceStdDevMm,
                        it.rssi
                    )
                )
                else -> writer.write("%s,-,-,-,-,-,-\n".format(it.macAddress))
            }

        }
        writer.close()
    }

    companion object {

        private const val TAG = "RTT_MEASUREMENT"

        fun createBundle(accessPoints: List<ScanResult>): Bundle {
            return bundleOf("accessPoints" to accessPoints)
        }

    }

}
