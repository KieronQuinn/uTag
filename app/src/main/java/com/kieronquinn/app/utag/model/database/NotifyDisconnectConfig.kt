package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import kotlinx.parcelize.Parcelize

@Entity
data class NotifyDisconnectConfig(
    @PrimaryKey
    @ColumnInfo("device_id_hash")
    val deviceIdHash: Int,
    @ColumnInfo("notify_disconnect")
    val notifyDisconnect: EncryptedValue,
    @ColumnInfo("show_image")
    val showImage: EncryptedValue
) {
    @Parcelize
    data class Backup(
        @SerializedName("device_id_hash")
        val deviceIdHash: Int,
        @SerializedName("notify_disconnect")
        val notifyDisconnect: Boolean,
        @SerializedName("show_image")
        val showImage: Boolean
    ): Parcelable {
        fun toConfig() = NotifyDisconnectConfig(
            deviceIdHash,
            notifyDisconnect.toEncryptedValue(),
            showImage.toEncryptedValue()
        )
    }

    fun toBackup() = Backup(
        deviceIdHash,
        notifyDisconnect.toBoolean(),
        showImage.toBoolean(),
    )
}
