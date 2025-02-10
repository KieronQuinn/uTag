package com.kieronquinn.app.utag.networking.services

import android.os.Build
import com.kieronquinn.app.utag.networking.model.smartthings.ConsentDetails
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

interface SamsungConsentService {

    companion object {
        private const val BASE_URL = "https://api.samsungconsent.com"

        fun createService(retrofit: Retrofit): SamsungConsentService {
            return retrofit.newBuilder()
                .baseUrl(BASE_URL)
                .build()
                .create(SamsungConsentService::class.java)
        }
    }

    @Headers(
        "X-Package-Name: com.samsung.android.plugin.fme",
        "X-Package-Version: PluginFME 3",
        "X-App-Id: 6ke9i504fe"
    )
    @GET("/v1/consent?type=PN&appKey=6ke9i504fe")
    fun getConsentDetails(
        @Header("X-Os-Version") osVersion: String = "Android ${Build.VERSION.SDK_INT}",
        @Header("X-Model-Name") modelName: String = Build.MODEL,
        @Header("X-Device-Id") deviceId: String,
        @Header("X-Started") started: Long = System.currentTimeMillis(),
        @Header("X-Requested") requested: Long = System.currentTimeMillis(),
        @Query("applicationRegion") applicationRegion: String = Locale_getDefaultWithCountry().country,
        @Query("region") region: String = Locale_getDefaultWithCountry().country,
        @Query("language") language: String = Locale_getDefaultWithCountry().language
    ): Call<List<ConsentDetails>>

}