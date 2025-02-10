package com.kieronquinn.app.utag.networking.model.find

import com.google.gson.annotations.SerializedName

data class FindItemsRequest<T>(
    @SerializedName("item")
    val items: List<T>
)
