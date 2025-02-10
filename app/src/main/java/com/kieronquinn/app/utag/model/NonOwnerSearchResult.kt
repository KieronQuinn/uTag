package com.kieronquinn.app.utag.model

import com.google.gson.annotations.SerializedName

data class NonOwnerSearchResult(
    @SerializedName("advertisementType")
    val advertisementType: Int,
    @SerializedName("arguments")
    val arguments: String,
    @SerializedName("batteryLevel")
    val batteryLevel: Int,
    @SerializedName("bleMac")
    val bleMac: String,
    @SerializedName("effectFile")
    val effectFile: String,
    @SerializedName("imageUrl")
    val imageUrl: String,
    @SerializedName("privacyId")
    val privacyId: String,
    @SerializedName("rssi")
    val rssi: Int,
    @SerializedName("uwbFlag")
    val uwbFlag: Int
)
