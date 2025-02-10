package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.smartthings.GetLocationUsersResponse
import com.kieronquinn.app.utag.networking.model.smartthings.GetLocationsResponse
import com.kieronquinn.app.utag.utils.extensions.smartThingsClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path

interface LocationService {

    companion object {
        private const val BASE_URL = "https://client.smartthings.com/"

        fun createService(context: Context, retrofit: Retrofit): LocationService {
            return retrofit.newBuilder()
                .client(smartThingsClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(LocationService::class.java)
        }
    }

    @GET("locations")
    fun getLocations(): Call<GetLocationsResponse>

    @GET("locations/{id}/users")
    @Headers("Accept: application/vnd.smartthings+json;v=6") //Requires this header
    fun getLocationUsers(@Path("id") id: String): Call<GetLocationUsersResponse>

}