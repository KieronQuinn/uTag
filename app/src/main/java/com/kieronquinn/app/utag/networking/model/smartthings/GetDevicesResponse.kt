package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetDevicesResponse(
    @SerializedName("items")
    val items: List<GetDeviceResponse>,
    @SerializedName("_links")
    val links: Links
) {
    data class Links(
        @SerializedName("next")
        val next: Link?,
        @SerializedName("previous")
        val previous: Link?
    ) {
        data class Link(
            @SerializedName("href")
            val href: String
        )
    }
}