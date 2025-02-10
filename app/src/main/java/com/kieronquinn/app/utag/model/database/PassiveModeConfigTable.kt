package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PassiveModeConfigTable {

    @Query("select * from `PassiveModeConfig`")
    fun getConfigs(): Flow<List<PassiveModeConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: PassiveModeConfig)

    @Query("delete from `PassiveModeConfig`")
    fun clear()

}