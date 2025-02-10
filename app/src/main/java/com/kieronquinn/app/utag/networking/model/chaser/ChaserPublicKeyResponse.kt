package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserPublicKeyResponse(
    @SerializedName("encryptedData")
    val encryptedData: String,
    @SerializedName("encryptedSecretKey")
    val encryptedSecretKey: String,
    @SerializedName("encryptedIv")
    val encryptedIv: String
)