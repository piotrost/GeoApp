package pl.pw.geoapp

import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

fun updateMapWithPosition(
    latitude: Double, longitude: Double,
    mapView: MapView?, currentMarker: Marker?
): Marker {
    // Remove the previous marker if it exists
    currentMarker?.let {
        mapView?.overlays?.remove(it)
    }

    // Update map center to the new position
    val mapController = mapView?.controller
    mapController?.setCenter(GeoPoint(latitude, longitude))

    // Place a new marker at the new position
    val newMarker = Marker(mapView)
    newMarker.position = GeoPoint(latitude, longitude)
    newMarker.title = "Your Position"

    // Add the new marker to the map
    mapView?.overlays?.add(newMarker)

    return newMarker
}