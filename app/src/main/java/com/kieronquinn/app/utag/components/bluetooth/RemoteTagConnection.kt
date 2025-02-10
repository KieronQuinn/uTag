package com.kieronquinn.app.utag.components.bluetooth

import androidx.core.uwb.UwbAddress
import com.kieronquinn.app.utag.components.bluetooth.BaseTagConnection.SyncResult
import com.kieronquinn.app.utag.components.uwb.UwbConfig
import com.kieronquinn.app.utag.model.ButtonVolumeLevel
import com.kieronquinn.app.utag.model.TagStatusChangeEvent
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.HistoryWidgetRepository
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.repositories.UTagServiceRepository
import com.kieronquinn.app.utag.service.IUTagService
import com.kieronquinn.app.utag.service.callback.IBooleanCallback
import com.kieronquinn.app.utag.service.callback.IStringCallback
import com.kieronquinn.app.utag.service.callback.ITagAutoSyncLocationCallback
import com.kieronquinn.app.utag.service.callback.ITagStateCallback
import com.kieronquinn.app.utag.service.callback.ITagStatusCallback
import com.samsung.android.oneconnect.smarttag.service.IGattRssiCallback
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class RemoteTagConnection(
    private val deviceId: String
): KoinComponent {

    private val service by inject<UTagServiceRepository>()
    private val apiRepository by inject<ApiRepository>()
    private val smartTagRepository by inject<SmartTagRepository>()
    private val historyWidgetRepository by inject<HistoryWidgetRepository>()
    private val scope = MainScope()

    private val connectionState = service.service.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(ConnectionState.DISCONNECTED)
        it.getConnectionState()
    }.distinctUntilChanged().onStart {
        service.assertReady()
    }.stateIn(scope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    private val tagStatusEvents = service.service.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(null)
        it.onTagStatusChanged()
    }.shareIn(scope, SharingStarted.Eagerly).filterNotNull()

    private val autoSyncState = service.service.flatMapLatest {
        if(it == null) return@flatMapLatest flowOf(false)
        it.autoSyncState()
    }.onEach {
        if(it is AutoSyncState.NotSyncing && it.lastResult == SyncResult.SUCCESS) {
            //Auto sync succeeded, refresh the tag states & history widget
            smartTagRepository.refreshTagStates()
            historyWidgetRepository.updateWidgets()
        }
    }.shareIn(scope, SharingStarted.Eagerly)

    fun getIsConnected() = connectionState.map { it == ConnectionState.CONNECTED }
    fun getIsScannedOrConnected() = connectionState.map { it != ConnectionState.DISCONNECTED }
    fun getIsAutoSyncing() = autoSyncState.map { it is AutoSyncState.Syncing }

    suspend fun syncLocation(onDemand: Boolean): SyncResult {
        return runString { syncLocation(deviceId, onDemand, it) }?.let {
            SyncResult.valueOf(it)
        } ?: SyncResult.FAILED_TO_CONNECT
    }

    suspend fun startRinging(bluetoothOnly: Boolean = false): RingResult {
        val volume = runString {
            startTagRinging(deviceId, it)
        }?.let {
            VolumeLevel.valueOf(it)
        }
        return when {
            volume != null -> RingResult.SuccessBluetooth(volume)
            !bluetoothOnly && apiRepository.setRinging(deviceId, true) -> {
                RingResult.SuccessNetwork
            }
            else -> RingResult.Failed
        }
    }

    fun onRingStop(): Flow<Unit> {
        return tagStatusEvents.filter { it == TagStatusChangeEvent.RING_STOP }.map { }
    }

    suspend fun setRingVolume(volume: VolumeLevel): Boolean {
        return runBoolean { setTagRingVolume(deviceId, volume.name, it) }
    }

    suspend fun stopRingingBluetooth(): Boolean {
        return runBoolean {
            stopTagRinging(deviceId, it)
        }
    }

    suspend fun stopRingingNetwork(): Boolean {
        return apiRepository.setRinging(deviceId, false)
    }

    suspend fun startRanging(config: UwbConfig, address: UwbAddress): Boolean {
        return runBoolean { startTagRanging(deviceId, config.getBytes(address), it) }
    }

    suspend fun stopRanging(): Boolean {
        return runBoolean { stopTagRanging(deviceId, it) }
    }

    suspend fun setSearchingMode(): Boolean {
        return apiRepository.setSearching(deviceId, true)
    }

    suspend fun setButtonConfig(pressEnabled: Boolean, holdEnabled: Boolean): Boolean {
        return runBoolean { setButtonConfig(deviceId, pressEnabled, holdEnabled, it) }
    }

    suspend fun getButtonVolume(): ButtonVolumeLevel? {
        return runString { getButtonVolumeLevel(deviceId, it) }?.let {
            ButtonVolumeLevel.valueOf(it)
        }
    }

    suspend fun setButtonVolume(level: ButtonVolumeLevel): Boolean {
        return runBoolean { setButtonVolumeLevel(deviceId, level.value, it) }
    }

    suspend fun getLostModeUrl(): String? {
        return runHexString { getLostModeUrl(deviceId, it) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun setLostModeUrl(url: String): Boolean {
        return runBoolean { setLostModeUrl(deviceId, url.toByteArray().toHexString(), it) }
    }

    suspend fun isE2EEnabled(): Boolean {
        return runBoolean { getE2EEnabled(deviceId, it) }
    }

    suspend fun setE2EEnabled(enabled: Boolean): Boolean {
        return runBoolean { setE2EEnabled(deviceId, enabled, it) }
    }

    fun readRssi(refreshRate: Long): Flow<Int> {
        return service.service.flatMapLatest {
            if(it == null) return@flatMapLatest flowOf()
            it.readRssi(refreshRate)
        }
    }

    private suspend fun runString(block: IUTagService.(IStringCallback) -> Unit): String? {
        return service.runWithService { service ->
            service.string(block)
        }.unwrap()
    }

    private suspend fun runHexString(block: IUTagService.(IStringCallback) -> Unit): String? {
        return service.runWithService { service ->
            service.hexString(block)
        }.unwrap()
    }

    private suspend fun runBoolean(block: IUTagService.(IBooleanCallback) -> Unit): Boolean {
        return service.runWithService { service ->
            service.boolean(block)
        }.unwrap()?: false
    }

    fun close() {
        scope.cancel()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun IUTagService.hexString(
        block: IUTagService.(IStringCallback) -> Unit
    ) = suspendCoroutine {
        val callback = object : IStringCallback.Stub() {
            override fun onResult(result: String?) {
                it.resume(result?.hexToByteArray()?.let { bytes -> String(bytes) })
            }
        }
        block(this, callback)
    }

    private suspend fun IUTagService.string(
        block: IUTagService.(IStringCallback) -> Unit
    ) = suspendCoroutine {
        val callback = object : IStringCallback.Stub() {
            override fun onResult(result: String?) {
                it.resume(result)
            }
        }
        block(this, callback)
    }

    private suspend fun IUTagService.boolean(
        block: IUTagService.(IBooleanCallback) -> Unit
    ) = suspendCoroutine {
        val callback = object : IBooleanCallback.Stub() {
            override fun onResult(result: Boolean) {
                it.resume(result)
            }
        }
        block(this, callback)
    }

    private fun IUTagService.getConnectionState() = callbackFlow {
        val callback = object: ITagStateCallback.Stub() {
            override fun onConnectedTagsChanged(
                connectedDeviceIds: Array<out String>,
                scannedDeviceIds: Array<out String>
            ) {
                val connectionState = when {
                    connectedDeviceIds.contains(deviceId) -> ConnectionState.CONNECTED
                    scannedDeviceIds.contains(deviceId) -> ConnectionState.SCANNED
                    else -> ConnectionState.DISCONNECTED
                }
                trySend(connectionState)
            }
        }
        val id = addTagStateCallback(callback)
        awaitClose {
            try {
                removeTagStateCallback(id ?: return@awaitClose)
            }catch (e: Exception) {
                //Ignore, service will clean this up
            }
        }
    }

    private fun IUTagService.readRssi(refreshRate: Long) = callbackFlow {
        val callback = object: IGattRssiCallback.Stub() {
            override fun onRssiChanged(rssi: Int) {
                trySend(rssi)
            }
        }
        startTagReadRssi(deviceId, refreshRate, callback)
        awaitClose {
            try {
                stopTagReadRssi(deviceId)
            }catch (e: Exception) {
                //Ignore, service will clean this up
            }
        }
    }

    private fun IUTagService.onTagStatusChanged() = callbackFlow {
        val callback = object: ITagStatusCallback.Stub() {
            override fun onTagStatusChanged(deviceId: String, status: TagStatusChangeEvent) {
                if(deviceId != this@RemoteTagConnection.deviceId) return
                trySend(status)
            }
        }
        val id = addTagStatusCallback(callback)
        awaitClose {
            try {
                removeTagStatusCallback(id)
            }catch (e: Exception) {
                //Ignore, service will clean this up
            }
        }
    }

    private fun IUTagService.autoSyncState() = callbackFlow {
        val callback = object: ITagAutoSyncLocationCallback.Stub() {
            override fun onStartSync(deviceId: String) {
                if(deviceId != this@RemoteTagConnection.deviceId) return
                trySend(AutoSyncState.Syncing)
            }

            override fun onSyncFinished(deviceId: String, result: String) {
                if(deviceId != this@RemoteTagConnection.deviceId) return
                trySend(AutoSyncState.NotSyncing(SyncResult.valueOf(result)))
            }
        }
        val id = addAutoSyncLocationCallback(callback)
        awaitClose {
            try {
                removeAutoSyncLocationCallback(id)
            }catch (e: Exception) {
                //Ignore, service will clean this up
            }
        }
    }

    sealed class RingResult {
        data class SuccessBluetooth(val volume: VolumeLevel): RingResult()
        data object SuccessNetwork: RingResult()
        data object Failed: RingResult()
    }

    private sealed class AutoSyncState {
        data class NotSyncing(val lastResult: SyncResult?) : AutoSyncState()
        data object Syncing : AutoSyncState()
    }

    enum class ConnectionState {
        DISCONNECTED, SCANNED, CONNECTED
    }

}