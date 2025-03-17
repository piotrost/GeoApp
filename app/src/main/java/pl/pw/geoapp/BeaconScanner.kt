package pl.pw.geoapp

import android.content.Context
import android.util.Log
import org.altbeacon.beacon.*
import kotlin.math.pow

class BeaconScanner(private val context: Context, private val callback: (List<BeaconData>) -> Unit) : BeaconConsumer {
    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private var isScanning = false // Track scanning state

    init {
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT))
        beaconManager.foregroundBetweenScanPeriod = 1100L // Scan every ~1 second
        beaconManager.backgroundBetweenScanPeriod = 0L // No delay in background
        beaconManager.updateScanPeriods()
        BeaconManager.setDebug(true) // Enable debug logs for beacon scanning
        beaconManager.isRegionStatePersistenceEnabled = false
    }

    override fun onBeaconServiceConnect() {
        if (isScanning) {
            Log.d("BeaconScanner", "✅ Connected to Beacon Service, starting ranging.")
            val region = Region("all-beacons-region", null, null, null)

            Log.d("BeaconScanner", "🔍 Adding range notifier...")
            beaconManager.addRangeNotifier { beacons, _ ->
                Log.d("BeaconScanner", "🔔 RangeNotifier triggered!")  // <-- Add this!

                if (beacons.isEmpty()) {
                    Log.d("BeaconScanner", "⚠ No beacons detected!")
                } else {
                    Log.d("BeaconScanner", "📡 Detected ${beacons.size} beacon(s)!")
                }

                val beaconDataList = beacons.map { beacon ->
                    val rssi = beacon.rssi
                    val txPower = beacon.txPower
                    val distance = estimateDistance(txPower, rssi)

                    Log.d("BeaconScanner", "UUID: ${beacon.id1}, RSSI: $rssi, TX: $txPower, Distance: $distance")

                    BeaconData(
                        uuid = beacon.id1.toString(),
                        distance = distance
                    )
                }
                callback(beaconDataList)
            }

            Log.d("BeaconScanner", "🚀 Starting beacon ranging...")
            beaconManager.startRangingBeacons(region)
        }
    }

    fun startScanning() {
        if (!isScanning) {
            isScanning = true
            beaconManager.bind(this) // Bind to start scanning
        }
    }

    fun stopScanning() {
        if (isScanning) {
            isScanning = false
            beaconManager.unbind(this) // Unbind to stop scanning
        }
    }

    override fun getApplicationContext(): Context = context
    override fun unbindService(serviceConnection: android.content.ServiceConnection) {
        context.unbindService(serviceConnection)
    }
    override fun bindService(intent: android.content.Intent, serviceConnection: android.content.ServiceConnection, flags: Int): Boolean {
        return context.bindService(intent, serviceConnection, flags)
    }

    private fun estimateDistance(txPower: Int, rssi: Int): Double {
        val n = 2.0 // Path-loss exponent for indoor environments
        return 10.0.pow((txPower - rssi) / (10 * n))
    }
}

data class BeaconData(val uuid: String, val distance: Double)
