package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kieronquinn.app.utag.model.EncryptedValue
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetConfigTable {

    @Query("select app_widget_id from `WidgetConfig`")
    fun getAppWidgetIds(): Flow<List<Int>>

    @Query("select device_ids from `WidgetConfig`")
    fun getDeviceIds(): Flow<List<EncryptedValue>>

    @Query("select * from `WidgetConfig` where app_widget_id=:appWidgetId")
    fun getConfig(appWidgetId: Int): Flow<WidgetConfig?>

    @Query("select * from `WidgetConfig`")
    fun getAllConfigs(): Flow<List<WidgetConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: WidgetConfig)

    @Query("delete from `WidgetConfig`")
    fun clear()

    @Query("delete from `WidgetConfig` where app_widget_id=:appWidgetId")
    fun delete(appWidgetId: Int)

}