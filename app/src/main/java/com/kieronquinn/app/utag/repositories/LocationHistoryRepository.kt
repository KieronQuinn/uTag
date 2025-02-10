package com.kieronquinn.app.utag.repositories

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.D2DStatus
import com.kieronquinn.app.utag.model.DeviceType
import com.kieronquinn.app.utag.model.GeoLocation
import com.kieronquinn.app.utag.repositories.ApiRepository.GetLocationHistoryResult
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.ExportLocation
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.HistoryState
import com.kieronquinn.app.utag.repositories.LocationHistoryRepository.LocationHistoryPoint
import com.kieronquinn.app.utag.utils.extensions.atEndOfDay
import com.kieronquinn.app.utag.utils.extensions.groupConsecutiveBy
import com.kieronquinn.app.utag.utils.extensions.toEpochMilli
import kotlinx.parcelize.Parcelize
import me.moallemi.tools.daterange.localdate.rangeTo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.roundToInt

interface LocationHistoryRepository {

    /**
     *  Gets location history from server for the past n [days] (as separate requests due to the
     *  limit being 500 locations per response), gets each address by geocoding, groups them
     *  consecutively to simplify the UI, with each location getting either a start/end time or
     *  a singular time of visiting.
     *
     *  Optionally provide [onProgressChanged] to receive updates with progress from 0-100%
     */
    suspend fun getLocationHistory(
        deviceId: String,
        days: Int = 8,
        limit: Int = 500,
        onProgressChanged: suspend (Int?) -> Unit = {}
    ): HistoryState

    data class LocationHistoryPoint(
        val location: LatLng,
        val address: String?,
        val startTime: LocalDateTime? = null,
        val time: LocalDateTime? = null,
        val endTime: LocalDateTime? = null,
        val locations: List<GeoLocation>
    ) {
        fun isOnDay(day: LocalDate): Boolean {
            val availableDays = ArrayList<LocalDate>()
            if (startTime != null && endTime != null) {
                for (i in startTime.toLocalDate()..endTime.toLocalDate()) {
                    availableDays.add(i)
                }
            }
            if (time != null) {
                availableDays.add(time.toLocalDate())
            }
            return availableDays.any { it == day }
        }

        fun timestamp(): Long? {
            return (endTime ?: time ?: startTime)?.let {
                ZonedDateTime.of(it, ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }
    }

    sealed class HistoryState(
        open val deviceId: String,
        open val timestamp: Long
    ) {
        data class Loading(
            override val deviceId: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val progress: Int?
        ) : HistoryState(deviceId, timestamp)

        data class Loaded(
            override val deviceId: String,
            override val timestamp: Long = System.currentTimeMillis(),
            val items: List<LocationHistoryPoint>,
            val exportLocations: List<ExportLocation>,
            val decryptFailed: Boolean
        ) : HistoryState(deviceId, timestamp)

        data class Error(
            override val deviceId: String,
            override val timestamp: Long = System.currentTimeMillis()
        ) : HistoryState(deviceId, timestamp)
    }

    @Parcelize
    data class ExportLocation(
        val location: LatLng,
        val address: String?,
        val time: Instant,
        val method: String,
        val accuracy: Double,
        val speed: Double?,
        val rssi: Int?,
        val battery: BatteryLevel?,
        val findHost: DeviceType?,
        val nearby: Boolean?,
        val onDemand: Boolean?,
        val connectedUserId: String?,
        val connectedDeviceId: String?,
        val d2dStatus: D2DStatus?,
        val wasEncrypted: Boolean
    ): Parcelable

}

class LocationHistoryRepositoryImpl(
    private val apiRepository: ApiRepository,
    private val geocoderRepository: GeocoderRepository,
    private val contentCreatorRepository: ContentCreatorRepository
) : LocationHistoryRepository {

    override suspend fun getLocationHistory(
        deviceId: String,
        days: Int,
        limit: Int,
        onProgressChanged: suspend (Int?) -> Unit
    ): HistoryState {
        val now = ZonedDateTime.now()
        val zone = now.zone
        val offset = now.offset
        val endDay = now.toLocalDate()
        val startDay = endDay.minusDays(days.toLong())
        val locations = ArrayList<GeoLocation>()
        var isPinRequired = false
        val range = startDay..endDay
        val totalDays = range.count()
        val chunk = 100 / totalDays
        var currentDay = 0
        val onProgressChangedWithDays: suspend (Float) -> Unit = {
            val baseProgress = chunk * currentDay
            val subProgress = chunk * it
            onProgressChanged((baseProgress + subProgress).roundToInt())
        }
        for (day in range) {
            val start = day.atStartOfDay().toEpochMilli(offset)
            val end = day.atEndOfDay().toEpochMilli(offset)
            val dayLocations = getFullLocationHistory(
                deviceId, start, end, limit, onProgressChangedWithDays
            )
            dayLocations.forEach {
                when(it) {
                    is GetLocationHistoryResult.Locations -> {
                        locations.addAll(it.locations)
                        if(it.pinRequired) {
                            isPinRequired = true
                        }
                    }
                    is GetLocationHistoryResult.Error -> {
                        return HistoryState.Error(deviceId)
                    }
                }
            }
            currentDay++
        }
        //Everything after this is indeterminate
        onProgressChanged(null)
        val exportLocations = ArrayList<ExportLocation>()
        return HistoryState.Loaded(deviceId, items = locations.map {
            val latLng = LatLng(it.latitude, it.longitude)
            val address = geocoderRepository.geocode(latLng)
            val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(it.time), zone)
            val exportLocation = ExportLocation(
                latLng,
                address,
                Instant.ofEpochMilli(it.time),
                it.method,
                it.accuracy,
                it.speed,
                it.rssi,
                it.battery,
                it.findHost,
                it.nearby,
                it.onDemand,
                it.connectedUserId,
                it.connectedDeviceId,
                it.d2dStatus,
                it.wasEncrypted
            )
            exportLocations.add(exportLocation)
            LocationHistoryItem(latLng, address, time, it)
        }.groupConsecutiveBy { a, b ->
            //Group by either the address being the same if not null, or the LatLngs matching
            (a.address != null && b.address != null && a.address == b.address) || a.latLng == b.latLng
        }.map {
            val primary = it.first()
            //If there's multiple points for this location, the start time is the first
            val startTime = if (it.size > 1) {
                primary.time
            } else null
            //If there's multiple points for this location, the end time is the last
            val endTime = if (it.size > 1) {
                it.last().time
            } else null
            //If there's only a single point for this location, the sole time is the first
            val time = if (it.size == 1) {
                primary.time
            } else null
            LocationHistoryPoint(
                location = primary.latLng,
                address = primary.address,
                startTime = startTime,
                time = time,
                endTime = endTime,
                locations = it.map { item -> item.source }
            )
        }, exportLocations = exportLocations, decryptFailed = isPinRequired)
    }

    private data class LocationHistoryItem(
        val latLng: LatLng,
        val address: String?,
        val time: LocalDateTime,
        val source: GeoLocation
    )

    private suspend fun getFullLocationHistory(
        deviceId: String,
        startTime: Long,
        endTime: Long,
        limit: Int,
        onProgressChanged: suspend (Float) -> Unit = {}
    ): List<GetLocationHistoryResult> {
        //If content creator mode is enabled, only use that
        contentCreatorRepository.getLocationHistory(deviceId, startTime, endTime)?.let {
            return listOf(it)
        }
        val results = ArrayList<GetLocationHistoryResult>()
        var currentStartTime = startTime
        while(true) {
            val result = apiRepository.getLocationHistory(
                deviceId,
                currentStartTime,
                endTime,
                limit,
                onProgressChanged = onProgressChanged
            )
            results.add(result)
            when {
                result is GetLocationHistoryResult.Locations && result.locations.size >= limit -> {
                    //More locations for this day, take the last time, add 1ms and call again
                    val lastTime = result.locations.lastOrNull()?.time ?: break
                    currentStartTime = lastTime + 1
                }
                //If the result is an error or there's less than the limit, we're done
                else -> break
            }
        }
        return results
    }

}