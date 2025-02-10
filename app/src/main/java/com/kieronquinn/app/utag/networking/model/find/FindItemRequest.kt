package com.kieronquinn.app.utag.networking.model.find

import com.google.gson.annotations.SerializedName

data class FindItemRequest<T>(
    @SerializedName("item")
    val item: T
)
