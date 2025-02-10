package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.networking.services.LocationService
import com.kieronquinn.app.utag.utils.extensions.get
import retrofit2.Retrofit

interface LocationRepository {

    /**
     *  Gets all users across all known locations, as a map of ID -> Name. Both the Samsung Account
     *  IDs and UUIDs are included in this list, because Samsung can't seem to decide which to use
     *  for different APIs.
     */
    suspend fun getAllUsers(): Map<String, String>?

}

class LocationRepositoryImpl(context: Context, retrofit: Retrofit): LocationRepository {

    private val service = LocationService.createService(context, retrofit)

    override suspend fun getAllUsers(): Map<String, String>? {
        val locations = service.getLocations().get(name = "locations")?.items?.map {
            it.locationId
        } ?: return null
        return locations.flatMap {
            service.getLocationUsers(it).get(name = "locationUsers")?.let { response ->
                val members = response.members?.toTypedArray() ?: emptyArray()
                listOfNotNull(response.owner, *members)
            }?.map { member ->
                listOf(
                    Pair(member.samsungAccountId, member.name),
                    Pair(member.uuid, member.name)
                )
            }?.flatten() ?: emptyList()
        }.toMap().ifEmpty { null }
    }

}