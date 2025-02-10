package com.kieronquinn.app.utag.model.database.cache

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheItemTable {

    @Query("select * from CacheItem")
    fun get(): List<CacheItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: CacheItem)

    @Delete
    fun delete(item: CacheItem)

    @Query("delete from CacheItem")
    fun clear()

}