package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity
data class PassiveModeConfig(
    @PrimaryKey
    @ColumnInfo("device_id_hash")
    val deviceIdHash: Int,
    @ColumnInfo("enabled")
    val enabled: Boolean
) {

    @Parcelize
    data class Backup(
        @SerializedName("device_id_hash")
        val deviceIdHash: Int,
        @SerializedName("enabled")
        val enabled: Boolean
    ): Parcelable {
        fun toConfig() = PassiveModeConfig(
            deviceIdHash = deviceIdHash,
            enabled = enabled
        )
    }

    fun toBackup() = Backup(deviceIdHash, enabled)

}
