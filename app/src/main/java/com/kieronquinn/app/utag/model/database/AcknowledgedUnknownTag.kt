package com.kieronquinn.app.utag.model.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toBoolean
import kotlinx.parcelize.Parcelize

@Entity
data class AcknowledgedUnknownTag(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Long = 0,
    @ColumnInfo("privacy_id")
    val privacyId: EncryptedValue,
    @ColumnInfo("has_acknowledged")
    val hasAcknowledged: EncryptedValue? = null,
    @ColumnInfo("is_safe")
    val isSafe: EncryptedValue? = null
) {
    @Parcelize
    data class Backup(
        @SerializedName("privacy_id")
        val privacyId: String,
        @SerializedName("has_acknowledged")
        val hasAcknowledged: Boolean?,
        @SerializedName("is_safe")
        val isSafe: Boolean?
    ): Parcelable

    fun toBackup() = Backup(
        String(privacyId.bytes),
        hasAcknowledged?.toBoolean(),
        isSafe?.toBoolean()
    )
}
