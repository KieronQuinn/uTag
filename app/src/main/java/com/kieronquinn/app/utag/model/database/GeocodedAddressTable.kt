package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface GeocodedAddressTable {

    @Query("select * from `GeocodedAddress` where lat_lng_hash=:latLngHash")
    fun getAddress(latLngHash: Int): GeocodedAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(address: GeocodedAddress)

    @Query("delete from `GeocodedAddress`")
    fun clear()

}
