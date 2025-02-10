package com.kieronquinn.app.utag.xposed.extensions

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.kieronquinn.app.utag.model.LocationStaleness
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take

@Suppress("DEPRECATION")
private fun Location.isMockCompat(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        isFromMockProvider
    }
}

@SuppressLint("MissingPermission")
fun Context.getLocation(staleness: LocationStaleness): Flow<Location?> {
    val now = System.currentTimeMillis()
    return getLocationAsFlow().filter {
        it != null && (now - it.time <= staleness.millis)
    }.take(1)
}

@SuppressLint("MissingPermission")
private fun FusedLocationProviderClient.getLastLocationAsFlow() = callbackFlow {
    try {
        lastLocation.addOnSuccessListener {
            trySend(it)
        }.addOnFailureListener {
            trySend(null)
        }
    }catch (e: SecurityException){
        trySend(null)
    }
    awaitClose {
        //Task cannot be cancelled
    }
}

@SuppressLint("MissingPermission")
fun Context.getLocationAsFlow(force: Boolean = false): Flow<Location?> {
    val provider = LocationServices.getFusedLocationProviderClient(this)
    return callbackFlow {
        val callback = object: LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                trySend(result.lastLocation?.takeUnless { it.isMockCompat() })
            }
        }
        if(!force) {
            trySend(provider.getLastLocationAsFlow().first())
        }
        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            1000L
        ).build()
        try {
            provider.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }catch (e: SecurityException){
            trySend(null)
        }
        awaitClose {
            try {
                provider.removeLocationUpdates(callback)
            }catch (e: SecurityException) {
                //Ignore
            }
        }
    }
}