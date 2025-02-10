package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import com.kieronquinn.app.utag.utils.extensions.toDouble
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toEnum
import com.kieronquinn.app.utag.utils.extensions.toFloat
import com.kieronquinn.app.utag.utils.extensions.toStringSet
import kotlinx.parcelize.Parcelize

@Entity
data class LocationSafeArea(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("name")
    val name: EncryptedValue,
    @ColumnInfo("latitude")
    val latitude: EncryptedValue,
    @ColumnInfo("longitude")
    val longitude: EncryptedValue,
    @ColumnInfo("radius")
    val radius: EncryptedValue,
    @ColumnInfo("is_active")
    val isActive: EncryptedValue,
    @ColumnInfo("last_exit_timestamp")
    val lastExitTimestamp: EncryptedValue,
    @ColumnInfo("exit_buffer")
    val exitBuffer: EncryptedValue,
    @ColumnInfo("active_device_ids")
    val activeDeviceIds: EncryptedValue
) {
    @Parcelize
    data class Backup(
        @SerializedName("id")
        val id: String,
        @SerializedName("name")
        val name: String,
        @SerializedName("latitude")
        val latitude: Double,
        @SerializedName("longitude")
        val longitude: Double,
        @SerializedName("radius")
        val radius: Float,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("exit_buffer")
        val exitBuffer: ExitBuffer,
        @SerializedName("active_device_ids")
        val activeDeviceIds: Set<String>
    ): Parcelable {
        fun toLocationSafeArea() = LocationSafeArea(
            id = id,
            name = name.toEncryptedValue(),
            latitude = latitude.toEncryptedValue(),
            longitude = longitude.toEncryptedValue(),
            radius = radius.toEncryptedValue(),
            isActive = isActive.toEncryptedValue(),
            exitBuffer = exitBuffer.toEncryptedValue(),
            activeDeviceIds = activeDeviceIds.toEncryptedValue(),
            lastExitTimestamp = 0L.toEncryptedValue()
        )
    }

    fun toBackup() = Backup(
        id = id,
        name = String(name.bytes),
        latitude = latitude.toDouble(),
        longitude = longitude.toDouble(),
        radius = radius.toFloat(),
        isActive = isActive.toBoolean(),
        exitBuffer = exitBuffer.toEnum(),
        activeDeviceIds = activeDeviceIds.toStringSet()
    )
}
