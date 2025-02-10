package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetLocationUsersResponse(
    @SerializedName("owner")
    val owner: User,
    @SerializedName("members")
    val members: List<User>?
) {

    data class User(
        @SerializedName("uuid")
        val uuid: String,
        @SerializedName("samsungAccountId")
        val samsungAccountId: String,
        @SerializedName("name")
        val name: String
    )

}