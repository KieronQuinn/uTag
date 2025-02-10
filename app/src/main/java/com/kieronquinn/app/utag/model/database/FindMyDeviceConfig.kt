package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class FindMyDeviceConfig(
    @PrimaryKey
    @ColumnInfo("device_id_hash")
    val deviceIdHash: Int,
    @ColumnInfo("enabled")
    val enabled: Boolean = false,
    @ColumnInfo("vibrate")
    val vibrate: Boolean = true,
    @ColumnInfo("delay")
    val delay: Boolean = false,
    @ColumnInfo("volume")
    val volume: Float = 100f
) : Parcelable {
    @Parcelize
    data class Backup(
        @SerializedName("device_id_hash")
        val deviceIdHash: Int,
        @SerializedName("vibrate")
        val vibrate: Boolean,
        @SerializedName("delay")
        val delay: Boolean,
        @SerializedName("volume")
        val volume: Float
    ): Parcelable {
        fun toConfig() = FindMyDeviceConfig(
            deviceIdHash = deviceIdHash,
            vibrate = vibrate,
            delay = delay,
            volume = volume
        )
    }

    fun toBackup() = Backup(deviceIdHash, vibrate, delay, volume)
}
