package com.kieronquinn.app.utag.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GeofenceReceiver: BroadcastReceiver(), KoinComponent {

    private val serviceRepository by inject<UTagServiceRepository>()
    private val scope = MainScope()

    override fun onReceive(context: Context, intent: Intent) {
        scope.launch {
            serviceRepository.runWithService {
                it.onGeofenceIntentReceived(intent)
            }
            cancel()
        }
    }

}