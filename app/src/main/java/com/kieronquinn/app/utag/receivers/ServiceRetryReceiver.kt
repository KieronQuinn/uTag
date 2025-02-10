package com.kieronquinn.app.utag.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.utag.service.UTagForegroundService
import org.koin.core.component.KoinComponent

class ServiceRetryReceiver: BroadcastReceiver(), KoinComponent {

    override fun onReceive(context: Context, intent: Intent) {
        UTagForegroundService.startIfNeeded(context)
    }

}