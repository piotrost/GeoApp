package pl.pw.geoapp

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.util.Log

class ConnectionChangeStateReceiver : BroadcastReceiver() {
    var isGpsEnabled = false
    var isBluetoothEnabled = false
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action == null) return

        when (intent.action) {
            LocationManager.PROVIDERS_CHANGED_ACTION -> {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                scanBleDevices()
                Log.d("CCSReceiver", "GPS Enabled: $isGpsEnabled")
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
                scanBleDevices()
                Log.d("CCSReceiver", "Bluetooth Enabled: $isBluetoothEnabled")
            }
        }
    }

    private fun scanBleDevices() {
        if (isGpsEnabled && isBluetoothEnabled) {
            Log.d("CCSReceiver", "Scan here!")
            //TODO skanowanie urządzeń
        } else {
            Log.d("CCSReceiver", "Not scanning.")
        }
    }
}
