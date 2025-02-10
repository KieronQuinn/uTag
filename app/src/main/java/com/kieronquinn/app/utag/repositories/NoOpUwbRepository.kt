package com.kieronquinn.app.utag.repositories

import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbEvent
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 *  No-op handling for UWB, for when unsupported by a device.
 */
class NoOpUwbRepository: UwbRepository {

    override val permission = null

    override suspend fun getUwbState(): UwbState {
        return UwbState.UNAVAILABLE
    }

    override suspend fun isUwbAvailable(): Boolean {
        return false
    }

    override fun startRanging(
        scope: CoroutineScope,
        tagConnection: RemoteTagConnection,
        onEnd: () -> Unit
    ): Flow<UwbEvent?> {
        return flowOf(UwbEvent.Failed(null))
    }

}