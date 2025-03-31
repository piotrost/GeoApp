package pl.pw.geoapp

import android.content.Context
import android.util.Log
import org.altbeacon.beacon.*
import pl.pw.geoapp.data.model.DetectedBeacon

class BeaconScanner(private val context: Context, private val callback: (List<DetectedBeacon>) -> Unit) {
    companion object {
        private const val TAG = "pw.BeaconScanner"
    }

    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private val region = Region("all-beacons-region", null, null, null)

    private var isScanning = false

    init {
        listOf(
            BeaconParser.EDDYSTONE_UID_LAYOUT,
            BeaconParser.EDDYSTONE_TLM_LAYOUT,
            BeaconParser.EDDYSTONE_URL_LAYOUT,
        ).forEach {
            beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(it))
        }
    }

    fun startScanning() {
        if (!isScanning) {
            isScanning = true

            beaconManager.addRangeNotifier { beacons, _ ->
                val beaconList = beacons.map {
                    DetectedBeacon(it.bluetoothAddress.toString(), it.distance)
                }
                Log.d(TAG, "Detected ${beaconList.size} beacons")
                callback(beaconList)
            }
            beaconManager.startRangingBeacons(region)

            Log.d(TAG, "Started beacon scanning")
        }
    }

    fun stopScanning() {
        if (isScanning) {
            isScanning = false

            beaconManager.stopRangingBeacons(region)
            beaconManager.removeAllRangeNotifiers()

            Log.d(TAG, "Stopped beacon scanning")
        }
    }
}
