package com.kieronquinn.app.utag.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue

@Entity
data class UnknownTag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Long = 0,
    @ColumnInfo("timestamp")
    val timestamp: Long,
    @ColumnInfo("mac")
    val mac: EncryptedValue?,
    @ColumnInfo("rssi")
    val rssi: EncryptedValue,
    @ColumnInfo("latitude")
    val latitude: EncryptedValue,
    @ColumnInfo("longitude")
    val longitude: EncryptedValue,
    @ColumnInfo("service_data")
    val serviceData: EncryptedValue
)
