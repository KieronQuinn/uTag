package com.kieronquinn.app.utag.model.database.cache

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import java.util.UUID

/**
 *  Stores data in the cache database. Each item should have a type (one of [CacheType]), and can
 *  also have a subtype, for example a device ID. The data should be JSON.
 *
 *  All values other than the ID are encrypted, when updating the cache conflicting items are
 *  checked for and removed.
 */
@Entity
data class CacheItem(
    @PrimaryKey
    @ColumnInfo("id")
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo("type")
    val type: EncryptedValue,
    @ColumnInfo("sub_type")
    val subType: EncryptedValue?,
    @ColumnInfo("data")
    val data: EncryptedValue
) {

    enum class CacheType {
        INSTALLED_APP_ID,
        USER_OPTIONS,
        USER_INFO,
        CONSENT_DETAILS,
        DEVICES,
        FMM_DEVICES,
        DEVICE_INFO,
        TAG_LOCATION,
        RULES,
        LOST_MODE,
        IMAGE_REDIRECT,
        ENCRYPTION_KEY
    }

}
