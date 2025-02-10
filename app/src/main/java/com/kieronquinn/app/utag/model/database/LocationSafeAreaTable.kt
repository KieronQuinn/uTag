package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationSafeAreaTable {

    @Query("select * from `LocationSafeArea`")
    fun getSafeAreas(): Flow<List<LocationSafeArea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(safeArea: LocationSafeArea)

    @Query("delete from `LocationSafeArea`")
    fun clear()

    @Query("delete from `LocationSafeArea` where id=:id")
    fun delete(id: String)

}