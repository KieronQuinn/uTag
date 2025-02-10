package com.kieronquinn.app.utag.utils.extensions

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map

fun ConnectivityManager.currentWiFiInfo(): Flow<WifiInfo?> {
    return capabilities(NetworkCapabilities.TRANSPORT_WIFI).map {
        it?.transportInfo as? WifiInfo
    }.filterNot {
        //Remove any instances where the network is coming back invalid, valid result should follow
        it?.isInvalid() == true
    }.distinctUntilChanged { old, new ->
        old != null && new != null && old.ssid == new.ssid && old.bssid == new.bssid
    }
}

private fun ConnectivityManager.capabilities(capabilities: Int) = callbackFlow {
    val listener = capabilityListener {
        trySend(it)
    }
    val request = NetworkRequest.Builder()
        .addTransportType(capabilities)
        .build()
    registerNetworkCallback(request, listener)
    awaitClose {
        unregisterNetworkCallback(listener)
    }
}

private fun ConnectivityManager.capabilityListener(
    onCapabilitiesChanged: (NetworkCapabilities?) -> Unit
): ConnectivityManager.NetworkCallback {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        object : ConnectivityManager.NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                onCapabilitiesChanged(networkCapabilities)
            }

            override fun onAvailable(network: Network) {
                onCapabilitiesChanged(getNetworkCapabilities(network))
            }

            override fun onLost(network: Network) {
                onCapabilitiesChanged(null)
            }
        }
    } else {
        object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                onCapabilitiesChanged(networkCapabilities)
            }
        }
    }
}