package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale

suspend fun Context.geocode(location: LatLng) = withContext(Dispatchers.IO) {
    if(!Geocoder.isPresent()) {
        return@withContext null
    }
    val geocoder = Geocoder(this@geocode, Locale.getDefault())
    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
        geocoder.geocode(location).first()
    }else{
        geocoder.geocodeLegacy(location)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun Geocoder.geocode(location: LatLng) = callbackFlow {
    val callback = object: GeocodeListener {
        override fun onError(errorMessage: String?) {
            trySend(null)
        }

        override fun onGeocode(addresses: MutableList<Address>) {
            trySend(addresses.firstOrNull())
        }
    }
    getFromLocation(location.latitude, location.longitude, 1, callback)
    awaitClose {
        //Call cannot be cancelled
    }
}

@Suppress("DEPRECATION")
private fun Geocoder.geocodeLegacy(location: LatLng): Address? {
    return try {
        getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
    }catch (e: Exception) {
        null
    }
}

fun Address.merge(location: LatLng, separator: String = ", "): String {
    val lines = ArrayList<String>()
    for(i in 0 .. maxAddressLineIndex) {
        lines.add(getAddressLine(i))
    }
    return lines.joinToString(separator).ifBlank {
        mergeFallback()
    }.ifBlank {
        "${location.latitude}, ${location.longitude}"
    }
}

/**
 *  getAddressLine is not guaranteed to be filled, this has caused issues in the past with custom
 *  geocoders (eg. GrapheneOS), so a fallback is used to provide a "best guess" address based on
 *  available data.
 */
fun Address.mergeFallback(separator: String = ", "): String {
    val components = mutableListOf<String>()

    if (!thoroughfare.isNullOrBlank()) {
        if (!subThoroughfare.isNullOrBlank()) {
            components.add("$subThoroughfare $thoroughfare")
        } else {
            components.add(thoroughfare)
        }
    } else if (!subThoroughfare.isNullOrBlank()) {
        components.add(subThoroughfare)
    }

    if (!locality.isNullOrBlank()) {
        components.add(locality)
    } else if (!subLocality.isNullOrBlank()) {
        components.add(subLocality)
    }

    if (!adminArea.isNullOrBlank()) {
        components.add(adminArea)
    } else if (!subAdminArea.isNullOrBlank()) {
        components.add(subAdminArea)
    }

    if (!postalCode.isNullOrBlank()) {
        components.add(postalCode)
    }

    if (!countryName.isNullOrBlank()) {
        components.add(countryName)
    }

    return components.joinToString(separator)
}