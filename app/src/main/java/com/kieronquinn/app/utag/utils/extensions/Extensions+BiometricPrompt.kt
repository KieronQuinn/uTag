package com.kieronquinn.app.utag.utils.extensions

import androidx.annotation.StringRes
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun Fragment.showBiometricPrompt(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    @StringRes negativeButtonText: Int,
    allowedAuthenticators: Int = Authenticators.BIOMETRIC_STRONG
) = callbackFlow {
    val executor = ContextCompat.getMainExecutor(requireContext())
    val callback = object: BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            trySend(BiometricEvent.Success(result))
        }

        override fun onAuthenticationFailed() {
            trySend(BiometricEvent.Failed)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            trySend(BiometricEvent.Error(errorCode, errString))
        }
    }
    val prompt = BiometricPrompt(this@showBiometricPrompt, executor, callback).apply {
        authenticate(PromptInfo.Builder()
            .setTitle(getString(title))
            .setSubtitle(getString(subtitle))
            .let {
                //Can't use negative button with device credential
                if(allowedAuthenticators and Authenticators.DEVICE_CREDENTIAL == 0) {
                    it.setNegativeButtonText(getString(negativeButtonText))
                }else it
            }
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()
        )
    }
    awaitClose {
        prompt.cancelAuthentication()
    }
}

sealed class BiometricEvent {
    data class Success(val result: BiometricPrompt.AuthenticationResult): BiometricEvent()
    data object Failed: BiometricEvent()
    data class Error(val errorCode: Int, val errorMessage: CharSequence): BiometricEvent()
}