package com.kieronquinn.app.utag.ui.activities

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieCompositionFactory
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.SettingsRepository
import com.kieronquinn.app.utag.utils.extensions.whenCreated
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import org.koin.android.ext.android.inject
import java.util.Locale

/**
 *  Clears Lottie cache when Activity is no longer needed. Lottie usually keeps compositions in
 *  memory, and since we have a constantly-running background service, this adds up, so by clearing
 *  when the app is closed, we reduce the memory footprint significantly.
 */
abstract class BaseActivity: AppCompatActivity() {

    private val encryptedSettingsRepository by inject<EncryptedSettingsRepository>()
    private val settingsRepository by inject<SettingsRepository>()

    open val disableAuth = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupHideInRecents()
        setupLanguageRecreate()
    }

    override fun onResume() {
        super.onResume()
        if(disableAuth) {
            AuthResponseActivity.setEnabled(false)
        }
    }

    override fun onDestroy() {
        LottieCompositionFactory.clearCache(applicationContext)
        super.onDestroy()
    }

    private fun setupLanguageRecreate() = whenCreated {
        settingsRepository.locale.asFlow().distinctUntilChanged().drop(1).collect {
            recreate()
        }
    }

    private fun setupHideInRecents() = whenCreated {
        encryptedSettingsRepository.biometricPromptEnabled.asFlow().collect { enabled ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                setRecentsScreenshotEnabled(!enabled)
            }else{
                if(enabled) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }else{
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.getLocalisedContext())
    }

    private fun Context.getLocalisedContext(): Context {
        //Handled by system on Android 13+
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return this
        val locale = settingsRepository.locale.getSync().takeIf { it.isNotEmpty() }?.let {
            Locale.forLanguageTag(it)
        } ?: Resources.getSystem().configuration.locales.get(0)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return ContextWrapper(createConfigurationContext(config))
    }

}