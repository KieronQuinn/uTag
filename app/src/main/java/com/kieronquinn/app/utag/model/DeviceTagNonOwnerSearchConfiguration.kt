package com.kieronquinn.app.utag.model

import com.google.gson.annotations.SerializedName

data class DeviceTagNonOwnerSearchConfiguration(
    @SerializedName("scanDuration")
    val scanDuration: Int = 20, //in seconds
    @SerializedName("numOfScanTimes")
    val numOfScanTimes: Int = 3,
    @SerializedName("rssiFilter")
    val rssiFilter: Int = -85,
    @SerializedName("brand")
    val brand: String = "GALAXY" //unused
) {
    fun getTimeout() = (scanDuration * numOfScanTimes * 1000L) + 2500L //Duration + buffer
}