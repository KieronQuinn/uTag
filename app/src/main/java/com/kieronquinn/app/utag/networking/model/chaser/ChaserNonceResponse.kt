package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserNonceResponse(
    @SerializedName("nonce")
    val nonce: String
)
