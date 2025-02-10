package com.kieronquinn.app.utag.model.database.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kieronquinn.app.utag.model.EncryptedValueConverter
import com.kieronquinn.app.utag.utils.extensions.wrapAsApplicationContext
import java.io.File

@Database(entities = [
    CacheItem::class
], version = 1, exportSchema = false)
@TypeConverters(EncryptedValueConverter::class)
abstract class CacheDatabase: RoomDatabase() {

    companion object {
        fun getDatabase(context: Context): CacheDatabase {
            val cacheFolder = context.cacheDir.apply {
                mkdirs()
            }
            val cacheDbFile = File(cacheFolder, "cache.db")
            return Room.databaseBuilder(
                context.wrapAsApplicationContext(),
                CacheDatabase::class.java,
                cacheDbFile.absolutePath
            ).enableMultiInstanceInvalidation().build()
        }
    }

    abstract fun cacheItemTable(): CacheItemTable

}