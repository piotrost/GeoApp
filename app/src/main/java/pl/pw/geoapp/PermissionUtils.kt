package pl.pw.geoapp.utils

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import pl.pw.geoapp.ConnectionChangeStateReceiver
import pl.pw.geoapp.MainActivity

fun requestRequiredPermissions(
    activity: MainActivity,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>
) {
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
    if (allPermissionsGranted(activity, permissions)) {
        activity.listenForConnectionChanges()
    } else {
        requestPermissionLauncher.launch(permissions)
    }
}

fun allPermissionsGranted(
    context: Context,
    permissions: Array<String>
): Boolean {
    permissions.forEach { permissionName ->
        if (ContextCompat.checkSelfPermission(context, permissionName) == PackageManager.PERMISSION_DENIED) {
            return false
        }
    }
    return true
}

fun MainActivity.listenForConnectionChanges() {
    connectionReceiver = ConnectionChangeStateReceiver(this) { canScan ->
        if (canScan) {
            Log.d("MainActivity", "Starting beacon scanning...")
            beaconScanner?.startScanning()

            Toast.makeText(
                this,
                "Skanowanie w toku...",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.d("MainActivity", "Stopping beacon scanning...")
            beaconScanner?.stopScanning()

            Toast.makeText(
                this,
                "Upewnij się, że masz włączony GPS oraz Bluetooth.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val filter = IntentFilter().apply {
        addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
    }
    registerReceiver(connectionReceiver, filter)
}
