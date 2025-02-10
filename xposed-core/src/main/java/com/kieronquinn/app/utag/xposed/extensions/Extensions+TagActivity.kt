package com.kieronquinn.app.utag.xposed.extensions

import android.content.ComponentName
import android.content.Intent
import com.kieronquinn.app.utag.xposed.Xposed.Companion.APPLICATION_ID

const val EXTRA_TARGET_ID = "target_id"

fun TagActivity_createIntent(targetId: String?): Intent {
    return Intent("${APPLICATION_ID}.MAP").apply {
        component = ComponentName(
            APPLICATION_ID,
            "${APPLICATION_ID}.ui.activities.TagActivity"
        )
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if(targetId != null) {
            putExtra(EXTRA_TARGET_ID, targetId)
        }
    }
}