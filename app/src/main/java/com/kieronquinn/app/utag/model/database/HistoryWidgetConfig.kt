package com.kieronquinn.app.utag.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme

@Entity
data class HistoryWidgetConfig(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo("package_name")
    val packageName: String,
    @ColumnInfo("device_id")
    val deviceId: EncryptedValue?,
    @ColumnInfo("map_style")
    val mapStyle: MapStyle,
    @ColumnInfo("map_theme")
    val mapTheme: MapTheme
)