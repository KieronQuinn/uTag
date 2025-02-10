package com.kieronquinn.app.utag.networking.model.find

import com.google.gson.annotations.SerializedName

data class FindItemResponse<T>(
    @SerializedName("requestId")
    val requestId: String,
    @SerializedName("timeCurrent")
    val timeCurrent: Long,
    @SerializedName("item")
    val item: T
)
