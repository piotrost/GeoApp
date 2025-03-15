package pl.pw.geoapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer

class MainActivity : AppCompatActivity() {

    private lateinit var beaconScanner: BeaconScanner

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.entries.any { !it.value }) {
                Toast.makeText(
                    this,
                    "Bez przydzielenia niezbędnych uprawnień aplikacja nie będzie działać prawidłowo.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                listenForConnectionChanges()
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setUpUI()
        requestRequiredPermissions()

        observeStatus()
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
        findViewById<Button>(R.id.show_map_button).setOnClickListener{
            Log.d("MainActivity", "CLICKED \"show_map_button\"")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("MainActivity", "onRestart")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy")
        beaconScanner.stopScanning()
    }

    private fun observeStatus() {
        StatusLiveData.isGpsEnabled.observe(this, Observer { gpsEnabled ->
            StatusLiveData.isBluetoothEnabled.observe(this, Observer { bluetoothEnabled ->
                if (gpsEnabled && bluetoothEnabled) {
                    startMultilateration()
                } else {
                    stopMultilateration()
                }
            })
        })
    }

    private fun startMultilateration() {
        beaconScanner = BeaconScanner(this) { beacons ->
            val beaconPositions = listOf(
                Beacon(x = 0.0, y = 0.0, distance = beacons.getOrNull(0)?.distance ?: 0.0),
                Beacon(x = 5.0, y = 0.0, distance = beacons.getOrNull(1)?.distance ?: 0.0),
                Beacon(x = 2.5, y = 4.0, distance = beacons.getOrNull(2)?.distance ?: 0.0)
            )

            val position = Multilateration.calculate(beaconPositions)
            position?.let {
                Log.d("User Position", "X: ${it.first}, Y: ${it.second}")
            }
        }
    }

    private fun stopMultilateration() {
        beaconScanner.stopScanning()
        Log.d("Multilateration", "Stopped due to GPS or Bluetooth being off.")
    }

    private fun requestRequiredPermissions() {
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
        if (allPermissionsGranted(permissions)) {
            listenForConnectionChanges()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private fun allPermissionsGranted(
        permissions: Array<String>,
    ): Boolean {
        permissions.forEach { permissionName ->
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    permissionName
                ) == PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    private fun listenForConnectionChanges() {
        Toast.makeText(
            this,
            "Upewnij się, że masz włączony GPS oraz Bluetooth.",
            Toast.LENGTH_SHORT
        ).show()

        val receiver = ConnectionChangeStateReceiver()
        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)  // For GPS state changes
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)      // For Bluetooth state changes
        }
        registerReceiver(receiver, filter)

    }

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
