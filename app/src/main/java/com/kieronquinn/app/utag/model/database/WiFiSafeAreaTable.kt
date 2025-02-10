package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WiFiSafeAreaTable {

    @Query("select * from `WiFiSafeArea`")
    fun getSafeAreas(): Flow<List<WiFiSafeArea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(safeArea: WiFiSafeArea)

    @Query("delete from `WiFiSafeArea`")
    fun clear()

    @Query("delete from `WiFiSafeArea` where id=:id")
    fun delete(id: String)

}