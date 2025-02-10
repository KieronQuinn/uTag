package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class ConsentDetails(
    @SerializedName("uri")
    val uri: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("language")
    val language: String
)
