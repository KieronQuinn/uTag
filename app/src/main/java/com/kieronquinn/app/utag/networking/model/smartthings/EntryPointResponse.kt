package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class EntryPointResponse(
    @SerializedName("signInURI")
    val signInUri: String,
    @SerializedName("signOutURI")
    val signOutUri: String,
    @SerializedName("chkDoNum")
    val chkDoNum: String,
    @SerializedName("pkiPublicKey")
    val publicKey: String
)