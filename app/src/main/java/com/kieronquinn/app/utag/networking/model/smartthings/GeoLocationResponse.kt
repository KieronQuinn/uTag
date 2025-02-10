package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GeoLocationResponse(
    @SerializedName("items")
    val items: List<GeoLocationItem>,
    @SerializedName("keyPairs")
    val keyPairs: List<KeyPair>,
    //Not from server, only used internally to know if this response was from the cache
    var cached: Boolean? = null
) {
    data class GeoLocationItem(
        @SerializedName("deviceId")
        val deviceId: String,
        @SerializedName("resultCode")
        val resultCode: Int,
        @SerializedName("geolocations")
        val geoLocations: List<GeoLocation>
    )

    data class KeyPair(
        @SerializedName("userId")
        val userId: String?,
        @SerializedName("privateKey")
        val privateKey: String?,
        @SerializedName("publicKey")
        val publicKey: String?,
        @SerializedName("iv")
        val iv: String?,
        @SerializedName("regDate")
        val regDate: String?
    )

    fun wasFromCache() = cached == true
}
