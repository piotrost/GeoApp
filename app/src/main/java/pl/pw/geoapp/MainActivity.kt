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
import pl.pw.geoapp.data.model.FinalBeacon
import com.google.gson.Gson
import pl.pw.geoapp.data.model.ArchiveBeacon
import pl.pw.geoapp.data.model.ArchiveBeaconList
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "pw.MainActivity"
    }

    private var beaconScanner: BeaconScanner? = null
    private var connectionReceiver: ConnectionChangeStateReceiver? = null
    private var lastKnownPosition: Pair<Double, Double>? = null
    private var beaconDict: Map<String, ArchiveBeacon>? = null

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
        Log.d(TAG, "onCreate")
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setUpUI()
        requestRequiredPermissions()

        beaconDict = loadBeaconsFromAssets(this, "beacons")
        beaconScanner = BeaconScanner(this) { beacons ->
            val beaconPositions = beacons.mapNotNull { detectedBeacon ->
                val archiveBeacon = beaconDict?.get(detectedBeacon.beaconUid)
                archiveBeacon?.let {
                    FinalBeacon(
                        x = it.longitude,
                        y = it.latitude,
                        distance = detectedBeacon.distance
                    )
                }
            }

            val position = Multilateration.calculate(beaconPositions)
            lastKnownPosition = position
            position?.let {
                Log.d(TAG, "USER POSITION -> X: ${it.first}, Y: ${it.second}")
            }
        }

    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume")
        findViewById<Button>(R.id.show_map_button).setOnClickListener {
            lastKnownPosition?.let { (x, y) ->
                Toast.makeText(this, "Last known position: X=$x, Y=$y", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Position unknown", Toast.LENGTH_SHORT).show()
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
        beaconScanner?.stopScanning()
        connectionReceiver?.let { unregisterReceiver(it) }
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
        connectionReceiver = ConnectionChangeStateReceiver(this) { canScan ->
            if (canScan) {
                Log.d("MainActivity", "Starting beacon scanning...")
                beaconScanner?.startScanning()
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

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadBeaconsFromAssets(context: Context, folderName: String): Map<String, ArchiveBeacon> {
        val assetManager = context.assets
        val files = assetManager.list(folderName) ?: return emptyMap()

        val gson = Gson()
        val beaconsMap = mutableMapOf<String, ArchiveBeacon>()

        for (fileName in files) {
            val jsonString = assetManager.open("$folderName/$fileName").use { inputStream ->
                InputStreamReader(inputStream).use { it.readText() }
            }

            val beaconList: ArchiveBeaconList = gson.fromJson(jsonString, ArchiveBeaconList::class.java)

            for (beacon in beaconList.items) {
                if (beacon.beaconUid != null) {
                    beaconsMap[beacon.beaconUid] = beacon
                }
            }
        }

        return beaconsMap
    }
}
