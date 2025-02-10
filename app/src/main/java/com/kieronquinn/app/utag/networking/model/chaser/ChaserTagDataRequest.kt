package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserTagDataRequest(
    @SerializedName("items")
    val items: List<Item>
) {
    data class Item(
        @SerializedName("serviceData")
        val serviceData: String
    )
}
