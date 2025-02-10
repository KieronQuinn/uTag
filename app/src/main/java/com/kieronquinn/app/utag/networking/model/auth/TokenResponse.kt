package com.kieronquinn.app.utag.networking.model.auth

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("refresh_token")
    val refreshToken: String,
    @SerializedName("userId") //Only provided on initial call
    val userId: String?
)
