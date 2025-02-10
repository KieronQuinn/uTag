package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity
data class AutomationConfig(
    @PrimaryKey
    @ColumnInfo("device_id_hash")
    val deviceIdHash: Int,
    @ColumnInfo("press_enabled")
    val pressEnabled: Boolean = false,
    @ColumnInfo("press_intent")
    val pressIntent: String? = null,
    @ColumnInfo("press_remote_enabled")
    val pressRemoteEnabled: Boolean = false,
    @ColumnInfo("hold_enabled")
    val holdEnabled: Boolean = false,
    @ColumnInfo("hold_intent")
    val holdIntent: String? = null,
    @ColumnInfo("hold_remote_enabled")
    val holdRemoteEnabled: Boolean = false,
) {
    @Parcelize
    data class Backup(
        @SerializedName("device_id_hash")
        val deviceIdHash: Int,
        @SerializedName("press_intent")
        val pressIntent: String?,
        @SerializedName("hold_intent")
        val holdIntent: String?
    ): Parcelable {
        fun toConfig() = AutomationConfig(
            deviceIdHash = deviceIdHash,
            pressIntent = pressIntent,
            holdIntent = holdIntent
        )
    }

    fun toBackup() = Backup(deviceIdHash, pressIntent, holdIntent)
}
