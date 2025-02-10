package com.kieronquinn.app.utag.networking.model.chaser

import com.google.gson.annotations.SerializedName

data class ChaserFindNode(
    @SerializedName("type")
    val type: String,
    @SerializedName("host")
    val host: String,
    @SerializedName("version")
    val version: String,
    @SerializedName("id")
    val id: String?,
    @SerializedName("policyVersion")
    val policyVersion: String?,
    @SerializedName("configuration")
    val configuration: Configuration
) {
    data class Configuration(
        @SerializedName("allowManualGeolocation")
        val allowManualGeolocation: Boolean,
        @SerializedName("allowedNlpGap")
        val allowedNlpGap: Int,
        @SerializedName("src")
        val src: String?
    )
}