package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotifyDisconnectConfigTable {

    @Query("select * from `NotifyDisconnectConfig`")
    fun getConfigs(): Flow<List<NotifyDisconnectConfig>>

    @Query("select * from `NotifyDisconnectConfig` where device_id_hash=:deviceIdHash")
    fun getConfig(deviceIdHash: Int): NotifyDisconnectConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: NotifyDisconnectConfig)

    @Query("delete from `NotifyDisconnectConfig`")
    fun clear()

}