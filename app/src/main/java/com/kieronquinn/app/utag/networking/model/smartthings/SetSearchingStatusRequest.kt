package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class SetSearchingStatusRequest(
    @SerializedName("searchingStatus")
    val searchingStatus: SearchingStatus
) {
    enum class SearchingStatus {
        @SerializedName("searching")
        SEARCHING,
        @SerializedName("stop")
        STOP
    }
}
