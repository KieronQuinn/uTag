package com.kieronquinn.app.utag.networking.model.auth

import com.google.gson.annotations.SerializedName

data class AuthoriseResponse(
    @SerializedName("code")
    val code: String?
)
