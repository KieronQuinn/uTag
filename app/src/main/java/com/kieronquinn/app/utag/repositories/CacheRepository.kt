package com.kieronquinn.app.utag.repositories

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.model.database.cache.CacheDatabase
import com.kieronquinn.app.utag.model.database.cache.CacheItem
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.model.database.cache.CacheItemTable
import com.kieronquinn.app.utag.utils.extensions.toEncryptedValue
import com.kieronquinn.app.utag.utils.extensions.toEnumOrNull
import com.kieronquinn.app.utag.utils.room.RoomEncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 *  Handles cache storage of encrypted Tag data. Database is stored in cache folder in internal
 *  storage so it can be cleared correctly.
 *
 *  Image cache is stored unencrypted, so should only be used for non-sensitive images.
 */
interface CacheRepository: RoomEncryptionHelper.RoomEncryptionFailedCallback {

    companion object {
        suspend inline fun <reified T : Any> CacheRepository.getCache(
            type: CacheType,
            subType: String? = null
        ): T? {
            val typeToken = object: TypeToken<T>() {}.type
            return getCache(type, subType, typeToken)
        }
    }

    suspend fun <T> getCache(type: CacheType, subType: String? = null, typeToken: Type): T?
    suspend fun <T> setCache(type: CacheType, subType: String? = null, data: T)
    suspend fun clearCache()

}

class CacheRepositoryImpl(
    private val gson: Gson,
    cacheDatabase: CacheDatabase
): CacheRepository {

    private val table = cacheDatabase.cacheItemTable()
    private val tableLock = Mutex()
    private val scope = MainScope()

    override suspend fun <T> getCache(type: CacheType, subType: String?, typeToken: Type): T? {
        return tableLock.withLock {
            val item = table.getItem(type, subType)?.data?.let {
                String(it.bytes)
            }
            if(item == null) return null
            try {
                gson.fromJson<T>(item, typeToken)?.takeIf { it.isValid() }
            } catch (e: Exception) {
                //If the cache is invalid (for example if the model has changed),
                null
            }
        }
    }


    override suspend fun <T> setCache(type: CacheType, subType: String?, data: T) {
        tableLock.withLock {
            val currentItem = table.getItem(type, subType)
            if(currentItem != null) {
                withContext(Dispatchers.IO) {
                    table.delete(currentItem)
                }
            }
            val newItem = CacheItem(
                type = type.toEncryptedValue(),
                subType = subType?.toEncryptedValue(),
                data = gson.toJson(data).toEncryptedValue()
            )
            withContext(Dispatchers.IO) {
                table.insert(newItem)
            }
        }
    }

    override suspend fun clearCache() {
        tableLock.withLock {
            withContext(Dispatchers.IO) {
                table.clear()
            }
        }
    }

    override fun onEncryptionFailed() {
        scope.launch {
            clearCache()
        }
    }

    private suspend fun CacheItemTable.getItem(type: CacheType, subType: String?): CacheItem? {
        return try {
            withContext(Dispatchers.IO) {
                get().firstOrNull {
                    it.type.toEnumOrNull<CacheType>() == type &&
                            (subType == null || it.subType?.let { type -> String(type.bytes) } == subType)
                }
            }
        }catch (e: IllegalStateException) {
            //Cache has become corrupt, clear and start again
            withContext(Dispatchers.IO) {
                table.clear()
            }
            null
        }
    }

    /**
     *  Recursively checks data class fields for this object, making sure any fields that are marked
     *  as not-null are not null. This ensures that the deserialised JSON object is not stale, and
     *  won't cause null pointer issues elsewhere in the app.
     */
    private fun Any.isValid(): Boolean {
        this::class.memberProperties.filter { it.visibility == KVisibility.PUBLIC }.forEach {
            if(!it.returnType.isMarkedNullable && it.getter.call(this) == null) {
                if(BuildConfig.DEBUG) {
                    Log.e("uCache", "Field ${it.name} is not marked nullable but is null!")
                }
                return false
            }
            if((it.returnType.classifier as? KClass<*>)?.isData == true &&
                it.getter.call(this)?.isValid() == false) {
                return false
            }
        }
        return true
    }

}