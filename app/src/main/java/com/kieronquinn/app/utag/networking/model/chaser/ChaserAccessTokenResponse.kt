package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserAccessTokenResponse(
    @SerializedName("accessToken")
    val accessToken: String,
    @SerializedName("expirationTime")
    val expirationTime: Long,
    @SerializedName("findNode")
    val findNode: ChaserFindNode
)