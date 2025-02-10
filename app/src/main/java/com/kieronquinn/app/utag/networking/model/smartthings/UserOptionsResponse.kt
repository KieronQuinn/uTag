package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class UserOptionsResponse(
    @SerializedName("fmmAgreement")
    val fmmAgreement: Boolean,
    @SerializedName("fmmAgreementUrls")
    val fmmAgreementUrls: List<Agreement>
)
