package com.kieronquinn.app.utag.networking.interceptors

import android.content.Context
import com.google.gson.Gson
import com.kieronquinn.app.utag.networking.model.smartthings.InstalledAppsRequest
import com.kieronquinn.app.utag.providers.AuthProvider
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SmartThingsAuthInterceptor(
    private val context: Context,
    private val updateBody: Boolean
): Interceptor, KoinComponent {

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val HEADER_APP_VERSION = "X-St-Client-Appversion"
        private const val HEADER_DEVICE_MODEL = "X-St-Client-Devicemodel"
        private const val HEADER_OS = "X-St-Client-Os"
        private const val HEADER_CORRELATION = "X-St-Correlation"
        private const val HEADER_USER_AGENT = "User-Agent"
        private const val HEADER_ACCEPT = "Accept"
        private const val HEADER_ACCEPT_LANGUAGE = "Accept-Language"
        private const val FIELD_NAME_TOKEN = "requesterToken"
        private const val FIELD_NAME_REQUESTER = "requester"
        private const val HEADER_ACCEPT_V1 = "application/vnd.smartthings+json;v=1"
        const val HEADER_ACCEPT_V6 = "application/vnd.smartthings+json;v=6"
    }

    private val smartThingsRepository by inject<SmartThingsRepository>()
    private val analyticsRepository by inject<AnalyticsRepository>()
    private val gson by inject<Gson>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().withAuthToken()
        val response = chain.proceed(request)
        if(response.code == 401) {
            if(!AuthProvider.refreshSmartThingsToken(context)) {
                analyticsRepository.recordNonFatal(LogoutNonFatalException(request.url))
                //Failed to refresh token, logout and return errored response
                AuthProvider.clearCredentials(context)
                return response
            }else{
                response.close()
                //Apply the new token and proceed
                return chain.proceed(request.withAuthToken())
            }
        }
        return response
    }

    private fun Request.withAuthToken(): Request {
        return newBuilder().apply {
            //V6 header always overrides the default
            if(headers[HEADER_ACCEPT] != HEADER_ACCEPT_V6) {
                removeHeader(HEADER_ACCEPT)
                addHeader(HEADER_ACCEPT, HEADER_ACCEPT_V1)
            }
            removeHeader(HEADER_ACCEPT_LANGUAGE)
            addHeader(HEADER_ACCEPT_LANGUAGE, Locale_getDefaultWithCountry().toLanguageTag())
            removeHeader(HEADER_AUTHORIZATION)
            val token = AuthProvider.getSmartThingsToken(context)
            if(token != null) {
                addHeader(HEADER_AUTHORIZATION, "Bearer $token")
            }
            removeHeader(HEADER_USER_AGENT)
            addHeader(HEADER_USER_AGENT, smartThingsRepository.userAgent)
            removeHeader(HEADER_APP_VERSION)
            addHeader(HEADER_APP_VERSION, smartThingsRepository.smartThingsVersion)
            removeHeader(HEADER_DEVICE_MODEL)
            addHeader(HEADER_DEVICE_MODEL, smartThingsRepository.deviceModel)
            removeHeader(HEADER_OS)
            addHeader(HEADER_OS, smartThingsRepository.os)
            removeHeader(HEADER_CORRELATION)
            addHeader(HEADER_CORRELATION, smartThingsRepository.correlationId)
            val userId = AuthProvider.getUserId(context)
            if(updateBody && token != null) {
                body?.updateBody(token, userId)?.let {
                    post(it)
                }
            }
        }.build()
    }

    private fun RequestBody.updateBody(authToken: String, userId: String?): RequestBody? {
        val body = gson.fromJson(bodyToString(), InstalledAppsRequest::class.java) ?: return null
        val updatedBody = body.copy(
            parameters = body.parameters.apply {
                remove(FIELD_NAME_TOKEN)
                addProperty(FIELD_NAME_TOKEN, authToken)
                remove(FIELD_NAME_REQUESTER)
                addProperty(FIELD_NAME_REQUESTER, userId)
            }
        )
        return gson.toJson(updatedBody).toRequestBody(contentType())
    }

    private fun RequestBody.bodyToString(): String {
        val buffer = okio.Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }

    private data class LogoutNonFatalException(val url: HttpUrl):
            Exception("Logged out trying to access $url")

}