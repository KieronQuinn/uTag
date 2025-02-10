package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDataTable {

    @Query("select * from TagData")
    fun getTags(): Flow<List<TagData>>

    @Query("select * from TagData where device_id_hash=:deviceIdHash")
    fun getTag(deviceIdHash: Int): TagData?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(tagData: TagData)

    @Query("delete from TagData")
    fun clear()

}