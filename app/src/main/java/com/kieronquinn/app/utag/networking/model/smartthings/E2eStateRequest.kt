package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class E2eStateRequest(
    @SerializedName("enabled")
    val enabled: Boolean
)