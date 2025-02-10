package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.GeoLocation
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationHistoryResult
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationResult
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import com.kieronquinn.app.utag.xposed.extensions.getLocationAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

interface ContentCreatorRepository {

    /**
     *  Returns whether Content Creator Mode is enabled
     */
    fun isEnabled(): Boolean

    /**
     *  Wrapper for [Context.getLocationAsFlow] which returns a single mock location when content
     *  creator mode is enabled
     */
    fun wrapLocationAsFlow(force: Boolean = false): Flow<Location?>

    /**
     *  Override for location history, loads all content from assets and returns it as
     *  [GetLocationHistoryResult]
     */
    suspend fun getLocationHistory(
        deviceId: String,
        startTime: Long,
        endTime: Long,
        limit: Int = 500,
        onProgressChanged: suspend (Float) -> Unit = {}
    ): GetLocationHistoryResult?

    /**
     *  Override for location getting, loads today's JSON and returns a random location
     */
    suspend fun getLocation(deviceId: String): GetLocationResult?

}

class ContentCreatorRepositoryImpl(
    private val context: Context,
    private val gson: Gson,
    settingsRepository: SettingsRepository
): ContentCreatorRepository {

    companion object {
        private const val PATH_JSON = "content_creator_history/"
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")
        private val GSON_TYPE = object: TypeToken<List<JsonLocation>>(){}.type
        private const val RANDOM_FUZZ = 0.0001
    }

    private val scope = MainScope()
    private val enabled = settingsRepository.contentCreatorModeEnabled.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun isEnabled(): Boolean {
        return runBlocking { enabled.firstNotNull() }
    }

    override fun wrapLocationAsFlow(force: Boolean): Flow<Location?> {
        return enabled.filterNotNull().flatMapLatest {
            if(it) {
                val location = getTimeBasedLocation().let {
                    Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = it.latitude
                        longitude = it.longitude
                    }
                }
                flowOf(location)
            }else{
                context.getLocationAsFlow(force)
            }
        }
    }

    override suspend fun getLocationHistory(
        deviceId: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        onProgressChanged: suspend (Float) -> Unit
    ): GetLocationHistoryResult? {
        if(!isEnabled()) return null
        return withContext(Dispatchers.IO) {
            val now = ZonedDateTime.now()
            val day = Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault())
            val dayOffset = 8 - Duration.between(now, day).toDays().absoluteValue - 1
            val asset = try {
                context.assets.open("${PATH_JSON}${dayOffset}.json")
            }catch (e: IOException) {
                //Day isn't set, return an empty day
                return@withContext GetLocationHistoryResult.Locations(emptyList(), false)
            }.reader()
            val json = gson.fromJson<List<JsonLocation>>(asset, GSON_TYPE)
            val locations = json.map {
                val time = LocalTime.parse(it.time, TIME_FORMAT)
                val timestamp = day.withHour(time.hour).withMinute(time.minute)
                val epochMillis = timestamp.toInstant().toEpochMilli()
                it.toGeoLocation(epochMillis)
            }
            GetLocationHistoryResult.Locations(locations, false)
        }
    }

    override suspend fun getLocation(deviceId: String): GetLocationResult? {
        if(!isEnabled()) return null
        return withContext(Dispatchers.IO) {
            GetLocationResult.Location(getTimeBasedLocation(), isEncrypted = false, cached = false)
        }
    }

    private fun getTimeBasedLocation(): GeoLocation {
        val asset = context.assets.open("${PATH_JSON}live.json").reader()
        val json = gson.fromJson<List<JsonLocation>>(asset, GSON_TYPE)
        val time = ZonedDateTime.now()
        return json[time.minute].randomise().toGeoLocation(time.toInstant().toEpochMilli())
    }

    /**
     *  Applies a small amount of randomisation to a location
     */
    private fun JsonLocation.randomise(): JsonLocation {
        val angle = Math.random() * 2 * Math.PI
        return copy(
            latitude = latitude + (RANDOM_FUZZ * sin(angle)),
            longitude = longitude + (RANDOM_FUZZ * cos(angle)),
        )
    }

    private fun JsonLocation.toGeoLocation(epochMillis: Long): GeoLocation {
        return GeoLocation(
            latitude = latitude,
            longitude = longitude,
            accuracy = 0.0,
            speed = null,
            rssi = null,
            battery = BatteryLevel.FULL,
            time = epochMillis,
            method = "",
            findHost = null,
            nearby = null,
            onDemand = null,
            connectedUserId = null,
            connectedDeviceId = null,
            d2dStatus = null,
            wasEncrypted = false
        )
    }

    private data class JsonLocation(
        @SerializedName("time")
        val time: String?,
        @SerializedName("latitude")
        val latitude: Double,
        @SerializedName("longitude")
        val longitude: Double
    )

}