package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetLocationResponse(
    @SerializedName("geolocations")
    val geoLocations: List<GeoLocation>
)
