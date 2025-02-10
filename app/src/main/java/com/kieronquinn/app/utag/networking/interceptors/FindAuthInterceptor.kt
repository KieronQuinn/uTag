package com.kieronquinn.app.utag.networking.interceptors

import android.content.Context
import com.kieronquinn.app.utag.providers.AuthProvider
import com.kieronquinn.app.utag.repositories.AnalyticsRepository
import com.kieronquinn.app.utag.utils.extensions.Locale_getDefaultWithCountry
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class FindAuthInterceptor(private val context: Context): Interceptor, KoinComponent {

    companion object {
        private const val HEADER_USER_ID = "X-Sec-Sa-Userid"
        private const val HEADER_COUNTRY_CODE = "X-Sec-Sa-Countrycode"
        private const val HEADER_AUTH_SERVER_URL = "X-Sec-Sa-Authserverurl"
        private const val HEADER_AUTH_TOKEN = "X-Sec-Sa-Authtoken"
    }

    private val analyticsRepository by inject<AnalyticsRepository>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().withAuthToken()
        val response = chain.proceed(request)
        //A 403 is sometimes returned if the token is not valid
        if(response.code == 401 || response.code == 403) {
            if(!AuthProvider.refreshFindToken(context)) {
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
            removeHeader(HEADER_USER_ID)
            AuthProvider.getUserId(context)?.let {
                addHeader(HEADER_USER_ID, it)
            }
            removeHeader(HEADER_COUNTRY_CODE)
            addHeader(HEADER_COUNTRY_CODE, Locale_getDefaultWithCountry().getISO3Country())
            removeHeader(HEADER_AUTH_SERVER_URL)
            AuthProvider.getAuthServerUrl(context)?.let {
                addHeader(HEADER_AUTH_SERVER_URL, it)
            }
            removeHeader(HEADER_AUTH_TOKEN)
            AuthProvider.getFindToken(context)?.let {
                addHeader(HEADER_AUTH_TOKEN, it)
            }
        }.build()
    }

    private data class LogoutNonFatalException(val url: HttpUrl):
        Exception("Logged out trying to access $url")

}