package pl.pw.geoapp

import android.content.Context
import org.altbeacon.beacon.*
import kotlin.math.pow

class BeaconScanner(private val context: Context, private val callback: (List<BeaconData>) -> Unit) : BeaconConsumer {
    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)
    private var isScanning = false // Track scanning state

    init {
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT))
    }

    override fun onBeaconServiceConnect() {
        if (isScanning) {
            val region = Region("all-beacons-region", null, null, null)
            beaconManager.addRangeNotifier { beacons, _ ->
                val beaconDataList = beacons.map { beacon ->
                    BeaconData(
                        uuid = beacon.id1.toString(),
                        distance = estimateDistance(beacon.txPower, beacon.rssi)
                    )
                }
                callback(beaconDataList)
            }
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
