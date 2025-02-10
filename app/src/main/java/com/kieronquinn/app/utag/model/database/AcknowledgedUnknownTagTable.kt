package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface AcknowledgedUnknownTagTable {

    companion object {
        suspend fun AcknowledgedUnknownTagTable.getTag(privacyId: String): AcknowledgedUnknownTag {
            return getTags().first().firstOrNull { String(it.privacyId.bytes) == privacyId }
                ?: AcknowledgedUnknownTag(privacyId = privacyId.toEncryptedValue())
        }
    }

    @Query("select * from AcknowledgedUnknownTag")
    fun getTags(): Flow<List<AcknowledgedUnknownTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(unknownTag: AcknowledgedUnknownTag)

    @Query("delete from AcknowledgedUnknownTag where id=:id")
    fun delete(id: Long)

    @Query("delete from AcknowledgedUnknownTag")
    fun clear()

}