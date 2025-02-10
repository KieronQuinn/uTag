package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class SetFavouritesItem(
    @SerializedName("order")
    val order: Int,
    @SerializedName("stDid")
    val stDid: String
)
