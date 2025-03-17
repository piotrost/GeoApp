package pl.pw.geoapp

import android.content.Context
import android.util.Log
import org.altbeacon.beacon.*
import org.altbeacon.beacon.Beacon
import kotlin.math.pow

class BeaconScanner(private val context: Context, private val callback: (List<BeaconData>) -> Unit) : BeaconConsumer {
    companion object {
        private const val TAG = "pw.BeaconManager"
    }

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private var isScanning = false // Track scanning state

    init {
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_TLM_LAYOUT))
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.EDDYSTONE_URL_LAYOUT))
        beaconManager.foregroundBetweenScanPeriod = 1100L // Scan every ~1 second
        beaconManager.backgroundBetweenScanPeriod = 0L // No delay in background
        beaconManager.updateScanPeriods()
        BeaconManager.setDebug(true) // Enable debug logs for beacon scanning
        beaconManager.isRegionStatePersistenceEnabled = false
    }

    override fun onBeaconServiceConnect() {
        if (isScanning) {
            Log.d(TAG, "✅ Connected to Beacon Service, starting ranging.")
            val region = Region("all-beacons-region", null, null, null)

            Log.d(TAG, "🔍 Adding range notifier...")
            beaconManager.addRangeNotifier {beacons, _ ->
                Log.d(TAG, "num of beacons: ${beacons.count()}")
            }

            Log.d(TAG, "🚀 Starting beacon ranging...")
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
