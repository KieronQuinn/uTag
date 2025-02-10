package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class OfflineInfoResponse(
    @SerializedName("keyRegTimestamp")
    val keyRegTimestamp: String?
)
