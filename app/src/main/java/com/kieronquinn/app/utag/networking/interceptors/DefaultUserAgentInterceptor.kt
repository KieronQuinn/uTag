package com.kieronquinn.app.utag.networking.interceptors

import okhttp3.Interceptor
import okhttp3.Response

class DefaultUserAgentInterceptor: Interceptor {

    companion object {
        private const val HEADER_USER_AGENT = "User-Agent"
    }

    private val userAgent = System.getProperty("http.agent") ?: ""

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            removeHeader(HEADER_USER_AGENT)
            addHeader(HEADER_USER_AGENT, userAgent)
        }.build()
        return chain.proceed(request)
    }

}