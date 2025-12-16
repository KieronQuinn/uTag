package com.kieronquinn.app.utag.networking.services

import android.content.Context
import com.kieronquinn.app.utag.networking.model.smartthings.GetDeviceResponse
import com.kieronquinn.app.utag.networking.model.smartthings.GetDevicesResponse
import com.kieronquinn.app.utag.utils.extensions.smartThingsClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface DeviceService {

    companion object {
        private const val BASE_URL = "https://client.smartthings.com/"
        const val GET_DEVICES_URL = "${BASE_URL}devices?includeAllowedActions=true&includeMfuLocations=true&includeUserDevices=false&excludeLocationDevices=false&includeGroups=true&includeHidden=true&exclusiveToHidden=false"

        fun createService(context: Context, retrofit: Retrofit): DeviceService {
            return retrofit.newBuilder()
                .client(smartThingsClient(context))
                .baseUrl(BASE_URL)
                .build()
                .create(DeviceService::class.java)
        }
    }

    @GET
    fun getDevices(@Url url: String): Call<GetDevicesResponse>

    @GET("devices/{deviceId}")
    fun getDevice(@Path("deviceId") deviceId: String): Call<GetDeviceResponse>

}