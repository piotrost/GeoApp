package pl.pw.geoapp

import android.content.Context
import org.altbeacon.beacon.*
import kotlin.math.pow

class BeaconScanner(private val context: Context, private val callback: (List<BeaconData>) -> Unit) : BeaconConsumer {
    private val beaconManager: BeaconManager = BeaconManager.getInstanceForApplication(context)

    init {
        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT))
        beaconManager.bind(this)
    }

    override fun onBeaconServiceConnect() {
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.addRangeNotifier { beacons, _ ->
            val beaconDataList = beacons.map { beacon ->
                BeaconData(
                    uuid = beacon.id1.toString(),
                    distance = estimateDistance(beacon.txPower, beacon.rssi)
                )
            }
            callback(beaconDataList) // Send data to the listener
        }

        beaconManager.startRangingBeacons(region)
    }

    fun stopScanning() {
        beaconManager.unbind(this)
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
