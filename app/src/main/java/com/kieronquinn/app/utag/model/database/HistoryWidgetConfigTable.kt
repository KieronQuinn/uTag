package com.kieronquinn.app.utag.model.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryWidgetConfigTable {

    @Query("select app_widget_id from HistoryWidgetConfig")
    fun getAppWidgetIds(): Flow<List<Int>>

    @Query("select * from HistoryWidgetConfig where app_widget_id=:appWidgetId")
    fun getConfig(appWidgetId: Int): Flow<HistoryWidgetConfig?>

    @Query("select * from `HistoryWidgetConfig`")
    fun getAllConfigs(): Flow<List<HistoryWidgetConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(config: HistoryWidgetConfig)

    @Query("delete from HistoryWidgetConfig")
    fun clear()

    @Query("delete from HistoryWidgetConfig where app_widget_id=:appWidgetId")
    fun delete(appWidgetId: Int)

}