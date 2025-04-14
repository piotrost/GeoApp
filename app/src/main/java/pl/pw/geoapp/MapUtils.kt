package pl.pw.geoapp

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

fun updateMapWithPosition(
    latitude: Double, longitude: Double,
    mapView: MapView?, currentMarker: Marker?
): Marker {
    currentMarker?.let {
        mapView?.overlays?.remove(it)
    }

    val mapController = mapView?.controller
    mapController?.setCenter(GeoPoint(latitude, longitude))

    val newMarker = Marker(mapView)
    newMarker.position = GeoPoint(latitude, longitude)
    newMarker.title = "Your Position"

    mapView?.overlays?.add(newMarker)

    return newMarker
}