package com.kieronquinn.app.utag.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kieronquinn.app.utag.model.EncryptedValue
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapStyle
import com.kieronquinn.app.utag.repositories.SettingsRepository.MapTheme

@Entity
data class WidgetConfig(
    @PrimaryKey
    @ColumnInfo(name = "app_widget_id")
    val appWidgetId: Int,
    @ColumnInfo("package_name")
    val packageName: String,
    @ColumnInfo("device_ids")
    val deviceIds: EncryptedValue,
    @ColumnInfo("open_device_id")
    val openDeviceId: EncryptedValue?,
    @ColumnInfo("status_device_id")
    val statusDeviceId: EncryptedValue?,
    @ColumnInfo("map_style")
    val mapStyle: MapStyle,
    @ColumnInfo("map_theme")
    val mapTheme: MapTheme
)