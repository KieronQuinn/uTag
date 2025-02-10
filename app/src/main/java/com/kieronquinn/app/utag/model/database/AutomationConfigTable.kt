package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationConfigTable {

    @Query("select * from `AutomationConfig` where device_id_hash=:deviceIdHash")
    fun getConfig(deviceIdHash: Int): AutomationConfig?

    @Query("select * from `AutomationConfig`")
    fun getAllConfigs(): List<AutomationConfig>

    @Query("select * from `AutomationConfig` where device_id_hash=:deviceIdHash")
    fun getConfigAsFlow(deviceIdHash: Int): Flow<AutomationConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: AutomationConfig)

    @Query("delete from `AutomationConfig`")
    fun clear()

}