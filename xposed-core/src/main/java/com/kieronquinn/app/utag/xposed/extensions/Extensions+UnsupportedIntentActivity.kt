package com.kieronquinn.app.utag.xposed.extensions

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.utag.xposed.Xposed.Companion.APPLICATION_ID

const val EXTRA_SOURCE = "source"

fun UnsupportedIntentActivity_createIntent(source: String): Intent {
    return Intent().apply {
        component = ComponentName(
            APPLICATION_ID,
            "${APPLICATION_ID}.ui.activities.UnsupportedIntentActivity"
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        putExtra(EXTRA_SOURCE, source)
    }
}