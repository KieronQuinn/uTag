package com.kieronquinn.app.utag.utils.extensions

import android.content.Context
import android.util.Log
import com.google.gson.reflect.TypeToken
import com.kieronquinn.app.utag.model.database.cache.CacheItem.CacheType
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.repositories.CacheRepository
import com.kieronquinn.app.utag.repositories.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.koin.java.KoinJavaComponent.inject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import java.io.IOException

suspend fun <T> Call<T>.get(throwIO: Boolean = false, name: String): T? {
    return withContext(Dispatchers.IO) {
        executeOrNull(throwIO, name)?.body()
    }
}

/**
 *  Same as [get], but uses cache if there's an IO exception and cache is available. Optional
 *  [convert] block to allow injecting special functionality if the cache object is used. If the
 *  cache is not used, the resulting object is written to the cache for later use.
 */
suspend inline fun <reified T : Any> Call<T>.get(
    type: CacheType,
    subType: String? = null,
    name: String,
    crossinline convert: T.() -> T = { this }
): T? {
    return withContext(Dispatchers.IO) {
        val cache by inject<CacheRepository>(CacheRepository::class.java)
        try {
            executeOrNull(true, name)?.body()?.also {
                //Write to cache for later
                cache.setCache(type, subType, it)
            }
        }catch (e: IOException) {
            //Use cache if available
            val typeToken = object: TypeToken<T>() {}.type
            cache.getCache<T>(type, subType, typeToken)?.convert()
        }
    }
}

suspend fun <T> Call<T>.executeOrNull(throwIO: Boolean = false, name: String): Response<T>? {
    return withContext(Dispatchers.IO) {
        try {
            execute().also {
                if(!it.isSuccessful) {
                    reportRetrofitError(
                        name,
                        error = it.errorBody()?.string(),
                        code = it.code(),
                        url = it.raw().request.url.toString()
                    )
                }
            }
        }catch (e: Exception) {
            if(throwIO && e is IOException) {
                throw e
            }
            //Don't report network issues
            if(e !is IOException) {
                reportRetrofitError(name, e)
            }
            null
        }
    }
}

fun reportRetrofitError(
    name: String,
    exception: Throwable? = null,
    error: String? = null,
    url: String? = null,
    code: Int? = null
) {
    //Ignore auth errors
    if(code in 400..403) return
    //Ignore errors from cancelled coroutines
    if(exception?.javaClass?.simpleName == "ChildCancelledException") return
    val inException = exception ?: Throwable("$url returned $code")
    val userVisibleError = exception?.toString() ?: if(url != null && code != null) {
        "$url returned $code:\n\n$error"
    } else null
    Log.e("Retrofit", "Error", exception ?: inException)
    val retrofitException = RetrofitException(name, exception ?: inException)
    val analytics by inject<AnalyticsRepository>(AnalyticsRepository::class.java)
    //The error body is guaranteed to only be error info so should be safe to send to Crashlytics
    analytics.recordNonFatal(retrofitException, error)
    val notificationRepository by inject<NotificationRepository>(NotificationRepository::class.java)
    notificationRepository.showErrorNotificationIfNeeded(userVisibleError ?: return)
}

private class RetrofitException(name: String, throwable: Throwable):
    RuntimeException("Error loading $name", throwable)

private const val CACHE_SIZE = 10 * 1024 * 1024 // 10 MB

fun Retrofit.Builder.withCache(context: Context): Retrofit.Builder {
    val cache = Cache(context.cacheDir, CACHE_SIZE.toLong())
    return client(OkHttpClient.Builder().cache(cache).build())
}