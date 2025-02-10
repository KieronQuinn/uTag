package com.kieronquinn.app.utag.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.utag.service.UTagForegroundService

class BootReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if(intent.action != Intent.ACTION_BOOT_COMPLETED) return
        UTagForegroundService.startIfNeeded(context)
    }

}