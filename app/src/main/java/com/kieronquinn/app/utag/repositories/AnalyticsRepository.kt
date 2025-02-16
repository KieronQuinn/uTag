package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.os.Build
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.ktx.Firebase
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 *  Controls whether Firebase Crashlytics + Analytics are enabled based on the setting, which is
 *  disabled by default. If the user opts in, this is automatically enabled and will re-run on
 *  start, unless they disable the setting.
 */
interface AnalyticsRepository {

    fun logEvent(event: String, params: Bundle = Bundle.EMPTY)
    fun recordNonFatal(throwable: Throwable, error: String? = null)

}

class AnalyticsRepositoryImpl(
    private val context: Context,
    settingsRepository: SettingsRepository
): AnalyticsRepository {

    init {
        //Non-main processes do not get Firebase auto-started, so we need to do it here.
        if(!isMainProcess()) {
            FirebaseApp.initializeApp(context)
        }
    }

    private val scope = MainScope()
    private val analyticsEnabled = settingsRepository.analyticsEnabled.asFlow()
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun logEvent(event: String, params: Bundle) {
        runBlocking {
            if(!analyticsEnabled.firstNotNull()) return@runBlocking
            try {
                Firebase.analytics.logEvent(event, params)
            }catch (e: Exception) {
                //Can't send, ignore
            }
        }
    }

    override fun recordNonFatal(throwable: Throwable, error: String?) {
        runBlocking {
            if(!analyticsEnabled.firstNotNull()) return@runBlocking
            try {
                if(error != null) {
                    Firebase.crashlytics.log(error)
                }
                Firebase.crashlytics.recordException(throwable)
            }catch (e: Exception) {
                //Can't send, ignore
            }
        }
    }

    private fun setupState() = scope.launch {
        analyticsEnabled.filterNotNull().collect {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(it)
            FirebaseCrashlytics.getInstance().apply {
                isCrashlyticsCollectionEnabled = it
                setupCrashlytics()
            }
        }
    }

    private fun FirebaseCrashlytics.setupCrashlytics() {
        setCustomKeys {
            "fingerprint" to Build.FINGERPRINT
            "version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
        }
    }

    init {
        setupState()
    }

}