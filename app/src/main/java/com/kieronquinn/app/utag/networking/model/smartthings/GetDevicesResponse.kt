package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetDevicesResponse(
    @SerializedName("items")
    val items: List<GetDeviceResponse>
)