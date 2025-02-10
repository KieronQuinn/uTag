package com.kieronquinn.app.utag.components.bluetooth

import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import org.koin.core.component.inject

/**
 *  Tag Connection that only supports syncing location.
 */
class ScannedTagConnection(
    override val deviceId: String,
    override val shouldAutoSync: Boolean,
    override val onAutoSyncStarted: ((deviceId: String) -> Unit)?,
    override val onAutoSyncFinished: ((deviceId: String, result: SyncResult) -> Unit)?,
): BaseTagConnection(deviceId, shouldAutoSync, onAutoSyncStarted, onAutoSyncFinished) {

    private val smartTagRepository by inject<SmartTagRepository>()

    override suspend fun getBatteryLevel(): BatteryLevel? {
        return smartTagRepository.getCachedTagData(deviceId)?.batteryLevel
    }

    override fun isConnectedForLocation(): Boolean {
        return false //When syncing location, Tag is never connected
    }

}