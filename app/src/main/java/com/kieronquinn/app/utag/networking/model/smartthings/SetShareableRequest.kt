package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class SetShareableRequest(
    @SerializedName("enabled")
    val enabled: Boolean
)
