package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserLocationsRequest(
    @SerializedName("items")
    val items: List<ChaserTag>,
    @SerializedName("findNode")
    val findNode: ChaserFindNode
) {
    data class ChaserTag(
        @SerializedName("geolocation")
        val geoLocation: ChaserGeoLocation,
        @SerializedName("tagAdvertisement")
        val tagAdvertisement: TagAdvertisement
    ) {
        data class ChaserGeoLocation(
            @SerializedName("accuracy")
            val accuracy: String,
            @SerializedName("battery")
            val battery: String,
            @SerializedName("latitude")
            val latitude: String,
            @SerializedName("longitude")
            val longitude: String,
            @SerializedName("method")
            val method: String,
            @SerializedName("rssi")
            val rssi: String,
            @SerializedName("speed")
            val speed: String,
            @SerializedName("timeStamp")
            val timestamp: Long,
            @SerializedName("valid")
            val valid: Boolean = true
        )

        data class TagAdvertisement(
            @SerializedName("serviceData")
            val serviceData: String
        )
    }
}
