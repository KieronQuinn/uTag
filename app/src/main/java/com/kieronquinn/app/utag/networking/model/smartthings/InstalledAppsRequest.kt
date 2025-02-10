package com.kieronquinn.app.utag.networking.model.smartthings

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.networking.model.smartthings.InstalledAppsRequest.Client.DisplayMode
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import com.kieronquinn.app.utag.utils.extensions.getUtcOffset
import com.kieronquinn.app.utag.utils.extensions.isDarkMode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.ZonedDateTime

data class InstalledAppsRequest(
    @SerializedName("client")
    val client: Client,
    @SerializedName("parameters")
    val parameters: JsonObject
) {

    companion object: KoinComponent {
        private val gson by inject<Gson>()
        private val authRepository by inject<AuthRepository>()
        private val smartThingsRepository by inject<SmartThingsRepository>()
        private val context by inject<Context>()

        fun createRequest(
            method: Method,
            uri: String,
            extraUri: String? = null,
            extraParameters: Map<String, String> = emptyMap(),
            headers: Any? = null,
            body: Any? = null
        ): InstalledAppsRequest {
            val encodedHeaders = headers?.let { gson.toJson(it) }?.toBase64()
            val encodedBody = body?.let { gson.toJson(body) }?.toBase64()
            val displayMode = if(context.isDarkMode) {
                DisplayMode.DARK
            }else{
                DisplayMode.LIGHT
            }
            val timeZoneOffset = ZonedDateTime.now().getUtcOffset()
            val client = Client(
                displayMode = displayMode,
                language = Locale_getDefaultWithCountry().toLanguageTag(),
                mobileDeviceId = authRepository.getDeviceId(),
                samsungAccountId = authRepository.getUserId(),
                timeZoneOffset = timeZoneOffset,
                version = smartThingsRepository.smartThingsVersion
            )
            val parameters = Parameters(
                //User ID & token are injected in the interceptor
                requester = "",
                requesterToken = "",
                method = method.value,
                uri = uri,
                extraUri = extraUri,
                encodedHeaders = encodedHeaders ?: "",
                encodedBody = encodedBody ?: ""
            ).asJsonObject(gson).apply {
                extraParameters.forEach { (key, value) ->
                    addProperty(key, value)
                }
            }
            return InstalledAppsRequest(client, parameters)
        }

        private fun Any.asJsonObject(gson: Gson): JsonObject {
            return gson.toJsonTree(this).asJsonObject
        }

        private fun String.toBase64(): String {
            return Base64.encodeToString(this.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }

        enum class Method(internal val value: String) {
            GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE")
        }
    }

    data class Client(
        @SerializedName("displayMode")
        val displayMode: DisplayMode,
        @SerializedName("language")
        val language: String,
        @SerializedName("mobileDeviceId")
        val mobileDeviceId: String,
        @SerializedName("os")
        val os: String = "Android",
        @SerializedName("samsungAccountId")
        val samsungAccountId: String?,
        @SerializedName("supportedTemplates")
        val supportedTemplates: List<String> = listOf(
            "BASIC_V1","BASIC_V2","BASIC_V3","BASIC_V4","BASIC_V5","BASIC_V6","BASIC_V7"
        ),
        @SerializedName("timeZoneOffset")
        val timeZoneOffset: String,
        @SerializedName("version")
        val version: String
    ) {
        enum class DisplayMode {
            @SerializedName("LIGHT")
            LIGHT,
            @SerializedName("DARK")
            DARK
        }
    }

    data class Parameters(
        //User ID
        @SerializedName("requester")
        val requester: String?,
        @SerializedName("clientType")
        val clientType: String = "aPlugin",
        @SerializedName("extraUri")
        val extraUri: String? = null,
        @SerializedName("method")
        val method: String,
        @SerializedName("encodedHeaders")
        val encodedHeaders: String = "",
        //Auth token
        @SerializedName("requesterToken")
        val requesterToken: String,
        @SerializedName("encodedBody")
        val encodedBody: String = "",
        @SerializedName("clientVersion")
        val clientVersion: String = "1",
        @SerializedName("uri")
        val uri: String
    )
}
