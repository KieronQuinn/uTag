package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UnknownTagTable {

    @Query("select * from UnknownTag")
    fun getTags(): Flow<List<UnknownTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(unknownTag: UnknownTag)

    @Query("delete from UnknownTag where timestamp < :timestamp")
    fun trim(timestamp: Long)

    @Query("delete from UnknownTag")
    fun clear()

}