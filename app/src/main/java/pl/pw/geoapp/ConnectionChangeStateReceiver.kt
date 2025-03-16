package pl.pw.geoapp

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log

class ConnectionChangeStateReceiver(private val context: Context, private val callback: (Boolean) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action == null) return

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true

        val canScan = isGpsEnabled && isBluetoothEnabled
        Log.d("CCSReceiver", "GPS: $isGpsEnabled, Bluetooth: $isBluetoothEnabled, Can Scan: $canScan")

        callback(canScan)
    }
}
