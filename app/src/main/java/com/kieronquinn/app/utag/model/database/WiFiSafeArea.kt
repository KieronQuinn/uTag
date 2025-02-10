package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.ExitBuffer
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toEnum
import com.kieronquinn.app.utag.utils.extensions.toStringSet
import kotlinx.parcelize.Parcelize

@Entity
data class WiFiSafeArea(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String,
    @ColumnInfo("name")
    val name: EncryptedValue,
    @ColumnInfo("ssid")
    val ssid: EncryptedValue,
    @ColumnInfo("mac")
    val mac: EncryptedValue?,
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
        val ssid: String,
        @SerializedName("longitude")
        val mac: String?,
        @SerializedName("is_active")
        val isActive: Boolean,
        @SerializedName("exit_buffer")
        val exitBuffer: ExitBuffer,
        @SerializedName("active_device_ids")
        val activeDeviceIds: Set<String>
    ): Parcelable {
        fun toWiFiSafeArea() = WiFiSafeArea(
            id = id,
            name = name.toEncryptedValue(),
            ssid = ssid.toEncryptedValue(),
            mac = mac?.toEncryptedValue(),
            isActive = isActive.toEncryptedValue(),
            exitBuffer = exitBuffer.toEncryptedValue(),
            activeDeviceIds = activeDeviceIds.toEncryptedValue(),
            lastExitTimestamp = 0L.toEncryptedValue()
        )
    }

    fun toBackup() = Backup(
        id = id,
        name = String(name.bytes),
        ssid = String(ssid.bytes),
        mac = mac?.let { String(it.bytes) },
        isActive = isActive.toBoolean(),
        exitBuffer = exitBuffer.toEnum(),
        activeDeviceIds = activeDeviceIds.toStringSet()
    )
}