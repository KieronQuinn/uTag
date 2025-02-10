package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class DevicesResponse(
    @SerializedName("ownerId")
    val ownerId: String,
    @SerializedName("devices")
    val devices: List<Device>,
    @SerializedName("favorites")
    val favourites: List<Favourite>,
    @SerializedName("favoritesTimestamp")
    val favouritesTimestamp: String,
    @SerializedName("members")
    val members: List<Member>?
) {
    data class Device(
        @SerializedName("saGuid")
        val saGuid: String?,
        @SerializedName("fmmDevId")
        val fmmDevId: String?,
        @SerializedName("locationType")
        val locationType: String,
        @SerializedName("stDevName")
        val stDevName: String?,
        @SerializedName("stOwnerId")
        val stOwnerId: String,
        @SerializedName("stDid")
        val stDid: String?,
        @SerializedName("shareGeolocation")
        val shareGeolocation: Boolean?,
        @SerializedName("mutualAgreement")
        val mutualAgreement: Boolean?
    ) {
        fun isTag() = locationType == "TRACKER"
    }
    data class Favourite(
        @SerializedName("order")
        val order: Int,
        @SerializedName("saGuid")
        val saGuid: String? = null,
        @SerializedName("fmmDevId")
        val fmmDevId: String? = null,
        @SerializedName("stDid")
        val stDid: String? = null
    )
    data class Member(
        @SerializedName("saGuid")
        val saGuid: String,
        @SerializedName("stOwnerId")
        val stOwnerId: String,
        @SerializedName("name")
        val name: String
    )
}
