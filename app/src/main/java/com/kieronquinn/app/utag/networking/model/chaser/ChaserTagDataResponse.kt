package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserTagDataResponse(
    @SerializedName("items")
    val items: List<Item>?
) {
    data class Item(
        @SerializedName("serviceData")
        val serviceData: String,
        @SerializedName("result")
        val result: String,
        @SerializedName("contents")
        val contents: List<Resource>
    ) {
        data class Resource(
            @SerializedName("resourceType")
            val resourceType: String,
            @SerializedName("type")
            val type: String,
            @SerializedName("file")
            val file: String
        )
    }
}