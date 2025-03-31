package pl.pw.geoapp.data.model

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

data class ArchiveBeacon (
    val beaconUid: String?,
    val longitude: Double,
    val latitude: Double
)

data class ArchiveBeaconList(
    val items: List<ArchiveBeacon>
)

fun loadBeaconsFromAssets(folderName: String, context: Context): Map<String, ArchiveBeacon> {
    val assetManager = context.assets
    val files = assetManager.list(folderName) ?: return emptyMap()

    val gson = Gson()
    val beaconsMap = mutableMapOf<String, ArchiveBeacon>()

    for (fileName in files) {
        val jsonString = InputStreamReader(assetManager.open("$folderName/$fileName")).use { reader ->
            reader.readText()
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