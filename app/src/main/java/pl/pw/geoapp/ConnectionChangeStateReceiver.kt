package pl.pw.geoapp

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log

class ConnectionChangeStateReceiver(private val context: Context, private val callback: (Boolean) -> Unit) : BroadcastReceiver() {
    companion object {
        private const val TAG = "pw.ConnectionChangeStateReceiver"
    }

    private var isGpsEnabled  = checkGpsEnabled()
    private var isBluetoothEnabled = checkBluetoothEnabled()

    init {
        Handler(Looper.getMainLooper()).post {
            performCallback()
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action == null) return

        when (intent.action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                isGpsEnabled  = checkGpsEnabled()
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                isBluetoothEnabled = checkBluetoothEnabled()
            }

        }

        performCallback()
    }

    private fun checkGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun checkBluetoothEnabled(): Boolean {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        return bluetoothAdapter?.isEnabled == true
    }
    private fun performCallback() {
        val canScan = isGpsEnabled && isBluetoothEnabled
        Log.d(TAG, "GPS: $isGpsEnabled, Bluetooth: $isBluetoothEnabled, Can Scan: $canScan")
        callback(canScan)
    }

}
