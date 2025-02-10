package com.kieronquinn.app.utag.networking.interceptors

import android.os.Build
import com.kieronquinn.app.utag.Application.Companion.CLIENT_ID_LOGIN
import com.kieronquinn.app.utag.Application.Companion.PACKAGE_NAME_ONECONNECT
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OspInterceptor: Interceptor, KoinComponent {

    companion object {
        private const val HEADER_OS_VERSION = "X-Osp-Clientosversion"
        private const val HEADER_MODEL = "X-Osp-Clientmodel"
        private const val HEADER_APP_ID = "X-Osp-Appid"
        private const val HEADER_PACKAGE_NAME = "X-Osp-Packagename"
        private const val HEADER_PACKAGE_VERSION = "X-Osp-Packageversion"
        private const val HEADER_USER_AGENT = "User-Agent"
    }

    private val smartThingsRepository by inject<SmartThingsRepository>()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().withHeaders()
        return chain.proceed(request)
    }

    private fun Request.withHeaders(): Request {
        return newBuilder().apply {
            removeHeader(HEADER_OS_VERSION)
            addHeader(HEADER_OS_VERSION, Build.VERSION.SDK_INT.toString())
            removeHeader(HEADER_MODEL)
            addHeader(HEADER_MODEL, Build.MODEL)
            removeHeader(HEADER_APP_ID)
            addHeader(HEADER_APP_ID, CLIENT_ID_LOGIN)
            removeHeader(HEADER_PACKAGE_NAME)
            addHeader(HEADER_PACKAGE_NAME, PACKAGE_NAME_ONECONNECT)
            removeHeader(HEADER_PACKAGE_VERSION)
            addHeader(HEADER_PACKAGE_VERSION, smartThingsRepository.smartThingsVersion)
            removeHeader(HEADER_USER_AGENT)
            addHeader(HEADER_USER_AGENT, smartThingsRepository.ospUserAgent)
        }.build()
    }

}