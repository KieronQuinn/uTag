package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FindMyDeviceConfigTable {

    @Query("select * from `FindMyDeviceConfig` where device_id_hash=:deviceIdHash")
    fun getConfig(deviceIdHash: Int): FindMyDeviceConfig?

    @Query("select * from `FindMyDeviceConfig`")
    fun getConfigs(): List<FindMyDeviceConfig>

    @Query("select * from `FindMyDeviceConfig` where device_id_hash=:deviceIdHash")
    fun getConfigAsFlow(deviceIdHash: Int): Flow<FindMyDeviceConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: FindMyDeviceConfig)

    @Query("delete from `FindMyDeviceConfig`")
    fun clear()

}