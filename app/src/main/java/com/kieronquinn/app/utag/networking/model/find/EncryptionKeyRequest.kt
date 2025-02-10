package com.kieronquinn.app.utag.networking.model.find

import com.google.gson.annotations.SerializedName

data class EncryptionKeyRequest(
    @SerializedName("privateKey")
    val privateKey: String,
    @SerializedName("publicKey")
    val publicKey: String,
    @SerializedName("iv")
    val iv: String
)
