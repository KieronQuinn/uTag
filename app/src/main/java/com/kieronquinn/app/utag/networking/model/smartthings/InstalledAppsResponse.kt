package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class InstalledAppsResponse<T>(
    @SerializedName("statusCode")
    val statusCode: Int,
    @SerializedName("requestId")
    val requestId: String,
    @SerializedName("errorCode")
    val errorCode: String?,
    @SerializedName("message")
    val message: T?
)
