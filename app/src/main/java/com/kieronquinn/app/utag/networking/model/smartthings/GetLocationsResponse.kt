package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetLocationsResponse(
    @SerializedName("items")
    val items: List<Location>
) {

    data class Location(
        @SerializedName("locationId")
        val locationId: String
    )

}
