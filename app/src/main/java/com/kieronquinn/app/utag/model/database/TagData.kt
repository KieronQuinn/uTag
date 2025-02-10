package com.kieronquinn.app.utag.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue

/**
 *  Stores a hashed form of the device ID against an encrypted version of its name and last received
 *  service data, for quick lookup when the device list may not have loaded yet.
 */
@Entity
data class TagData(
    @PrimaryKey
    @ColumnInfo("device_id_hash")
    val deviceIdHash: Int,
    @ColumnInfo("name")
    val name: EncryptedValue? = null,
    @ColumnInfo("ble_mac")
    val bleMac: EncryptedValue? = null,
    @ColumnInfo("last_service_data")
    val lastServiceData: EncryptedValue? = null,
    @ColumnInfo("service_data_timestamp")
    val serviceDataTimestamp: EncryptedValue? = null
)
