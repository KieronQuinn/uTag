package com.kieronquinn.app.utag.model.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kieronquinn.app.utag.model.EncryptedValueConverter
import com.kieronquinn.app.utag.utils.extensions.wrapAsApplicationContext

@Database(entities = [
    AcknowledgedUnknownTag::class,
    AutomationConfig::class,
    FindMyDeviceConfig::class,
    GeocodedAddress::class,
    TagData::class,
    LocationSafeArea::class,
    HistoryWidgetConfig::class,
    NotifyDisconnectConfig::class,
    PassiveModeConfig::class,
    UnknownTag::class,
    WiFiSafeArea::class,
    WidgetConfig::class
], version = 1, exportSchema = false)
@TypeConverters(EncryptedValueConverter::class)
abstract class UTagDatabase: RoomDatabase() {

    companion object {
        fun getDatabase(context: Context): UTagDatabase {
            return Room.databaseBuilder(
                context.wrapAsApplicationContext(),
                UTagDatabase::class.java,
                "utag.db"
            ).enableMultiInstanceInvalidation().build()
        }
    }

    abstract fun geocodedAddressTable(): GeocodedAddressTable
    abstract fun tagDataTable(): TagDataTable
    abstract fun findMyDeviceConfigTable(): FindMyDeviceConfigTable
    abstract fun automationConfigTable(): AutomationConfigTable
    abstract fun locationSafeAreaTable(): LocationSafeAreaTable
    abstract fun wifiSafeAreaTable(): WiFiSafeAreaTable
    abstract fun notifyDisconnectTable(): NotifyDisconnectConfigTable
    abstract fun widgetConfigTable(): WidgetConfigTable
    abstract fun locationWidgetConfigTable(): HistoryWidgetConfigTable
    abstract fun passiveModeConfigTable(): PassiveModeConfigTable
    abstract fun unknownTagTable(): UnknownTagTable
    abstract fun acknowledgedUnknownTagTable(): AcknowledgedUnknownTagTable

}