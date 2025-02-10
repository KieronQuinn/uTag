package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class DisableSmartThingsActionsRequest(
    @SerializedName("petWalking")
    val petWalking: BooleanField = BooleanField(false),
    @SerializedName("remoteRing")
    val remoteRing: BooleanField = BooleanField(false)
) {
    data class BooleanField(
        @SerializedName("enabled")
        val enabled: Boolean
    )
}
