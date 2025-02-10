package com.kieronquinn.app.utag.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue

/**
 *  Stores a hashed form of the LatLng (which has previously been rounded to increase collisions)
 *  against the address retrieved from the Geocoder, to reduce network calls. The address encrypted
 *  here so we're not storing an unencrypted list of the user's location history on device, and the
 *  LatLng is hashed since we can query by it, but we don't want to store it either.
 */
@Entity
data class GeocodedAddress(
    @PrimaryKey
    @ColumnInfo("lat_lng_hash")
    val latLngHash: Int,
    @ColumnInfo("address")
    val address: EncryptedValue
)
