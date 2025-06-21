package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class UserOptionsResponse(
    @SerializedName("ppVersion")
    val ppVersion: String?,
    @SerializedName("fmmAgreement")
    val fmmAgreement: Boolean,
    @SerializedName("fmmAgreementUrls")
    val fmmAgreementUrls: List<Agreement>
)
