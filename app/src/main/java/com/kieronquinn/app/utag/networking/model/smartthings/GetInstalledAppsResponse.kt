package com.kieronquinn.app.utag.networking.model.smartthings

import com.google.gson.annotations.SerializedName

data class GetInstalledAppsResponse(
    @SerializedName("items")
    val items: List<Item>
) {

    data class Item(
        @SerializedName("installedAppId")
        val installedAppId: String,
        @SerializedName("installedAppType")
        val installedAppType: String,
        @SerializedName("installedAppStatus")
        val installedAppStatus: String,
        @SerializedName("displayName")
        val displayName: String,
        @SerializedName("appId")
        val appId: String,
        @SerializedName("referenceId")
        val referenceId: Any?,
        @SerializedName("locationId")
        val locationId: String,
        @SerializedName("owner")
        val owner: Owner,
        @SerializedName("notices")
        val notices: List<Any?>,
        @SerializedName("createdDate")
        val createdDate: String,
        @SerializedName("lastUpdatedDate")
        val lastUpdatedDate: String,
        @SerializedName("ui")
        val ui: Ui,
        @SerializedName("iconImage")
        val iconImage: IconImage,
        @SerializedName("classifications")
        val classifications: List<String>,
        @SerializedName("principalType")
        val principalType: String,
        @SerializedName("restrictionTier")
        val restrictionTier: Long,
        @SerializedName("singleInstance")
        val singleInstance: Boolean,
        @SerializedName("allowed")
        val allowed: List<String>
    )

    data class Owner(
        @SerializedName("ownerType")
        val ownerType: String,
        @SerializedName("ownerId")
        val ownerId: String,
    )

    data class Ui(
        @SerializedName("pluginId")
        val pluginId: String,
        @SerializedName("pluginUri")
        val pluginUri: Any?,
        @SerializedName("dashboardCardsEnabled")
        val dashboardCardsEnabled: Boolean,
        @SerializedName("preInstallDashboardCardsEnabled")
        val preInstallDashboardCardsEnabled: Boolean,
    )

    data class IconImage(
        @SerializedName("url")
        val url: String,
    )

}