package com.kieronquinn.app.utag.networking.model.auth

import com.google.gson.annotations.SerializedName

data class AuthenticateResponse(
    @SerializedName("userauth_token")
    val userAuthToken: String,
    @SerializedName("userId")
    val userId: String
)
