package com.kieronquinn.app.utag.networking.services

import com.kieronquinn.app.utag.model.ChaserRegion
import com.kieronquinn.app.utag.networking.model.chaser.ChaserAccessTokenResponse
import com.kieronquinn.app.utag.networking.model.chaser.ChaserLocationsRequest
import com.kieronquinn.app.utag.networking.model.chaser.ChaserNonceResponse
import com.kieronquinn.app.utag.networking.model.chaser.ChaserPublicKeyResponse
import com.kieronquinn.app.utag.utils.extensions.defaultUserAgentClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ChaserService {

    companion object {
        fun createService(retrofit: Retrofit): ChaserService {
            return retrofit.newBuilder()
                .client(defaultUserAgentClient())
                .build()
                .create(ChaserService::class.java)
        }

        fun ChaserService.getAccessToken(
            region: ChaserRegion,
            version: Long,
            publicKey: String,
            signature: String,
            certificate: String,
            nonce: String
        ): Call<ChaserAccessTokenResponse> {
            return getAccessTokenInternal(
                url = "https://${region.url}/accesstoken",
                version = version,
                signature = signature,
                publicKey = publicKey,
                certificate = certificate,
                nonce = nonce
            )
        }

        fun ChaserService.getNonce(region: ChaserRegion): Call<ChaserNonceResponse> {
            return getNonceInternal("https://${region.url}/nonce")
        }

        fun ChaserService.getPublicKeys(
            region: ChaserRegion,
            token: String,
            vararg privacyId: String
        ): Call<ChaserPublicKeyResponse> {
            return getPublicKeysInternal(
                url = "https://${region.url}/v2/pubkeys",
                authorization = "Bearer $token",
                pid = privacyId.toList()
            )
        }

        fun ChaserService.sendLocations(
            region: ChaserRegion,
            token: String,
            body: ChaserLocationsRequest
        ): Call<Unit> {
            return sendLocationsInternal(
                url = "https://${region.url}/geolocations",
                authorization = "Bearer $token",
                body = body
            )
        }

    }

    @POST
    fun getAccessTokenInternal(
        @Url
        url: String,
        @Body
        body: Any = Object(), //Body should be empty
        @Header("x-iot-findnode-version")
        version: Long,
        @Header("signature")
        signature: String,
        @Header("certificate")
        certificate: String,
        @Header("X-Iot-Findnode-Publickey")
        publicKey: String,
        @Header("x-iot-findnode-type")
        type: String = "MOVING",
        @Header("x-iot-findnode-host")
        host: String = "GALAXY_PHONE",
        @Header("nonce")
        nonce: String
    ): Call<ChaserAccessTokenResponse>

    @GET
    fun getNonceInternal(@Url url: String): Call<ChaserNonceResponse>

    @GET
    fun getPublicKeysInternal(
        @Url
        url: String,
        @Header("Authorization")
        authorization: String,
        @Query("pid", encoded = true)
        pid: List<String>
    ): Call<ChaserPublicKeyResponse>

    @POST
    fun sendLocationsInternal(
        @Url
        url: String,
        @Header("Authorization")
        authorization: String,
        @Body
        body: ChaserLocationsRequest
    ): Call<Unit>

}