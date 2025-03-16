package pl.pw.geoapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
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

class MainActivity : AppCompatActivity() {

    private lateinit var beaconScanner: BeaconScanner
    private var connectionReceiver: ConnectionChangeStateReceiver? = null  // Ensure single instance

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

        beaconScanner = BeaconScanner(this) { beacons ->
            // The 'beacons' here contains the actual nearby beacons and their distance values
            val beaconPositions = beacons.mapIndexed { index, beaconData ->
                Beacon(x = index.toDouble() * 5.0, y = index.toDouble() * 5.0, distance = beaconData.distance)
            }

            val position = Multilateration.calculate(beaconPositions)
            position?.let {
                Log.d("User Position", "X: ${it.first}, Y: ${it.second}")
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
        checkGpsAndBluetoothState()
    }

    private fun checkGpsAndBluetoothState() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true

        if (isGpsEnabled && isBluetoothEnabled) {
            // Both are enabled, start scanning
            Log.d("MainActivity", "Starting beacon scanning...")
            beaconScanner.startScanning()
        } else {
            // Show a message to the user
            Log.d("MainActivity", "GPS or Bluetooth not enabled. Cannot start scanning.")
            Toast.makeText(this, "Please enable both GPS and Bluetooth.", Toast.LENGTH_SHORT).show()
        }
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
        connectionReceiver?.let { unregisterReceiver(it) }  // Unregister receiver
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

        connectionReceiver = ConnectionChangeStateReceiver(this) { canScan ->
            if (canScan) {
                Log.d("MainActivity", "Starting beacon scanning...")
                beaconScanner.startScanning()
            } else {
                Log.d("MainActivity", "Stopping beacon scanning...")
                beaconScanner.stopScanning()
            }
        }

        val filter = IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        registerReceiver(connectionReceiver, filter)

    }

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
