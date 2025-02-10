package com.kieronquinn.app.utag.utils.extensions

import android.location.Location
import android.location.LocationManager
import com.google.android.gms.maps.model.LatLng

private const val GEOCODING_ROUND_TO = 4

/**
 *  When Geocoding, we commit the resulting address to an encrypted database to save on network
 *  calls, so we also need to reduce the accuracy of the location slightly to increase the number
 *  of collisions to further save on network calls. This rounds the LatLng to 4 decimal places,
 *  which is sufficient for looking up addresses, but the original LatLng will always be shown on
 *  the map to the user.
 */
fun LatLng.roundForGeocoding(): LatLng {
    return LatLng(
        latitude.round(GEOCODING_ROUND_TO),
        longitude.round(GEOCODING_ROUND_TO)
    )
}

fun LatLng.toLocation(): Location {
    return Location(LocationManager.GPS_PROVIDER).apply {
        latitude = this@toLocation.latitude
        longitude = this@toLocation.longitude
    }
}