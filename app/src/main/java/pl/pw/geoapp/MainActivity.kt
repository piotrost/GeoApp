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
import pl.pw.geoapp.data.model.ArchiveBeacon
import pl.pw.geoapp.data.model.FinalBeacon
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import pl.pw.geoapp.data.model.loadBeaconsFromAssets

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "pw.MainActivity"
    }

    private var beaconScanner: BeaconScanner? = null
    private var connectionReceiver: ConnectionChangeStateReceiver? = null
    private var beaconDict: Map<String, ArchiveBeacon>? = null
    private var mapView: MapView? = null
    private var lastKnownPosition: Pair<Double, Double>? = null
    private var currentMarker: Marker? = null  // To store the current marker

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
        val prefs = getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)
        setContentView(R.layout.activity_main)
        setUpUI()
        requestRequiredPermissions()

        setUpPositioning()

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
                Toast.makeText(this, "X=$x\nY=$y", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Position unknown", Toast.LENGTH_SHORT).show()
        }

        val prefs = getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivity", "onPause")

        val prefs = getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        Configuration.getInstance().load(applicationContext, prefs)
        mapView?.onPause()
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

    private fun setUpPositioning() {
        beaconDict = loadBeaconsFromAssets("beacons", this)
        beaconScanner = BeaconScanner(this) { beacons ->
            val beaconPositions = beacons.mapNotNull { detectedBeacon ->
                val archiveBeacon = beaconDict?.get(detectedBeacon.beaconUid)
                archiveBeacon?.let {
                    FinalBeacon(
                        x = it.latitude,
                        y = it.longitude,
                        distance = detectedBeacon.distance
                    )
                }
            }

//            val beaconsss = listOf(
//                FinalBeacon(x=52.22064396008256, y=21.009697033995153, distance=0.4343884542236323),
//                FinalBeacon(x=52.22066679, y=21.00960664, distance=0.15516041187205845),
//                FinalBeacon(x=52.220749757094815, y=21.009567263503705, distance=0.6648326359915008),
//                FinalBeacon(x=52.22075213, y=21.00967231, distance=0.5386151140948994),
//                FinalBeacon(x=52.22064318, y=21.00969221, distance=0.6648326359915008),
//                FinalBeacon(x=52.220750671765316, y=21.00967526042275, distance=0.00984930291881791),
//                FinalBeacon(x=52.220759284911374, y=21.0098905077384, distance=0.27850097600940216),
//                FinalBeacon(x=52.22071499960274, y=21.009802293665103, distance=0.17490122876598085),
//                FinalBeacon(x=52.22069838307279, y=21.009744562593166, distance=0.4839823071792934)
//            )

            val position = PositioningAlgorithm.multilateration(beaconPositions)
            if (position != null){
                lastKnownPosition = position
            }
            position?.let {
                Log.d(TAG, "USER POSITION -> X: ${it.first}, Y: ${it.second}")
                currentMarker = updateMapWithPosition(it.first, it.second, mapView, currentMarker)
            }
        }
        mapView = findViewById(R.id.mapView)
        mapView?.setMultiTouchControls(true) // Enable pinch zoom

        val mapController = mapView?.controller
        mapController?.setZoom(19.0) // Zoom level (higher = closer)
        mapController?.setCenter(GeoPoint(52.2207, 21.0096))
    }

    private fun setUpUI() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
