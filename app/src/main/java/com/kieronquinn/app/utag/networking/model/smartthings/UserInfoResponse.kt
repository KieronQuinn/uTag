package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class UserInfoResponse(
    @SerializedName("uuid")
    val uuid: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("fullName")
    val fullName: String,
    @SerializedName("givenName")
    val givenName: String,
    @SerializedName("familyName")
    val familyName: String,
    @SerializedName("countryCode")
    val countryCode: String
)
