package com.kieronquinn.app.utag.utils.extensions

import `in`.aabhasjindal.otptextview.OTPListener
import `in`.aabhasjindal.otptextview.OtpTextView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

fun OtpTextView.onEvent() = callbackFlow {
    otpListener = object: OTPListener {
        override fun onOTPComplete(otp: String) {
            trySend(OTPEvent.Complete(otp))
        }

        override fun onInteractionListener() {
            trySend(OTPEvent.Changed(otp ?: ""))
        }
    }
    awaitClose {
        otpListener = null
    }
}

sealed class OTPEvent(open val value: String) {
    data class Changed(override val value: String): OTPEvent(value)
    data class Complete(override val value: String): OTPEvent(value)
}