package com.kieronquinn.app.utag.utils.extensions

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun getAuthIntent(url: String): Intent {
    return CustomTabsIntent.Builder().apply {
        setShowTitle(true)
        setShareState(CustomTabsIntent.SHARE_STATE_OFF)
        setBookmarksButtonEnabled(false)
        setDownloadButtonEnabled(false)
    }.build().intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        data = Uri.parse(url)
    }
}