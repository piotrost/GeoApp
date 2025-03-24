package pl.pw.geoapp.data.model

data class ArchiveBeacon (
    val beaconUid: String?,
    val longitude: Double,
    val latitude: Double
)

data class ArchiveBeaconList(
    val items: List<ArchiveBeacon>
)