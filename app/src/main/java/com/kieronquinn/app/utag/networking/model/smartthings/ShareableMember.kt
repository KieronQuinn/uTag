package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class ShareableMember(
    @SerializedName("saGuid")
    val saGuid: String,
    @SerializedName("agreementVersion")
    val agreementVersion: String = "1.0.0"
)
