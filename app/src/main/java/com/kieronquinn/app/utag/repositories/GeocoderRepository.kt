package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.database.GeocodedAddress
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.utils.extensions.geocode
import com.kieronquinn.app.utag.utils.extensions.merge
import com.kieronquinn.app.utag.utils.extensions.roundForGeocoding
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper.RoomEncryptionFailedCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface GeocoderRepository: RoomEncryptionFailedCallback {

    /**
     *  Gets the approximate address for a [LatLng] using the system Geocoder. If the Geocoder is
     *  not available or the location returns no addresses, `null` is returned. [LatLng]s are
     *  rounded to remove an acceptable amount of accuracy before being sent to increase collisions
     *  and reduce network calls, since addresses are encrypted and cached in the database against
     *  a hash of the location.
     */
    suspend fun geocode(location: LatLng, firstLineOnly: Boolean = false): String?

    /**
     *  Clears cached Geocoded addresses. They're kept in the main database to intentionally make it
     *  harder to clear the addresses by mistake, for example if bulk-clearing cache, since they
     *  take longer to reload.
     */
    suspend fun clearCache()

}

class GeocoderRepositoryImpl(
    private val context: Context,
    database: UTagDatabase
): GeocoderRepository {

    private val scope = MainScope()
    private val databaseLock = Mutex()
    private val geocodedAddressTable = database.geocodedAddressTable()

    override suspend fun geocode(location: LatLng, firstLineOnly: Boolean): String? {
        val roundedLocation = location.roundForGeocoding()
        val format: String.() -> String? = {
            if(firstLineOnly) firstLineOrNull() else this
        }
        return (getCachedAddressOrNull(roundedLocation) ?: context.geocode(location)?.merge()?.also {
            cacheAddress(roundedLocation, it)
        })?.format()
    }

    override suspend fun clearCache() {
        withContext(Dispatchers.IO) {
            geocodedAddressTable.clear()
        }
    }

    private suspend fun getCachedAddressOrNull(location: LatLng): String? = databaseLock.withLock {
        withContext(Dispatchers.IO) {
            val hash = location.hashCode()
            try {
                geocodedAddressTable.getAddress(hash)?.address?.bytes?.let {
                    String(it, Charsets.UTF_8)
                }
            }catch (e: IllegalStateException) {
                //Address is somehow null in database, just return null
                null
            }
        }
    }

    private suspend fun cacheAddress(location: LatLng, address: String) = databaseLock.withLock {
        withContext(Dispatchers.IO) {
            val geocodedAddress = GeocodedAddress(
                location.hashCode(),
                EncryptedValue(address.toByteArray(Charsets.UTF_8))
            )
            geocodedAddressTable.insert(geocodedAddress)
        }
    }

    override fun onEncryptionFailed() {
        scope.launch {
            //Nuke the database since the values will no longer decrypt
            geocodedAddressTable.clear()
        }
    }

    private fun String.firstLineOrNull(): String {
        return if(contains(",")) split(",")[0] else this
    }

}