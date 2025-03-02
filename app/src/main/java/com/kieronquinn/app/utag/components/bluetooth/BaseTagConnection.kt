package com.kieronquinn.app.utag.components.bluetooth

import android.os.Build
import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.D2DStatus
import com.kieronquinn.app.utag.networking.model.smartthings.SendLocationRequest
import com.kieronquinn.app.utag.repositories.ApiRepository
import com.kieronquinn.app.utag.repositories.AuthRepository
import com.kieronquinn.app.utag.repositories.EncryptedSettingsRepository
import com.kieronquinn.app.utag.repositories.SmartThingsRepository
import com.kieronquinn.app.utag.repositories.UserRepository
import com.kieronquinn.app.utag.utils.extensions.runCatchingOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BaseTagConnection(
    open val deviceId: String,
    open val shouldAutoSync: Boolean,
    open val onAutoSyncStarted: ((deviceId: String) -> Unit)?,
    open val onAutoSyncFinished: ((deviceId: String, result: SyncResult) -> Unit)?
): KoinComponent {

    private val apiRepository by inject<ApiRepository>()
    private val smartThingsRepository by inject<SmartThingsRepository>()
    private val userRepository by inject<UserRepository>()
    private val encryptedSettingsRepository by inject<EncryptedSettingsRepository>()
    private val authRepository by inject<AuthRepository>()
    private val scope = MainScope()
    private var autoRefreshJob: Job? = null
    private var syncLocationJob: Job? = null

    var isAutoSyncing = false
    private val syncLock = Mutex()

    private fun autoSyncLocation() {
        autoRefreshJob = scope.launch {
            val result = when {
                !shouldAutoSync -> SyncResult.FAILED_AUTO_SYNC_NOT_REQUIRED
                !isAutoSyncing -> {
                    syncLock.withLock {
                        isAutoSyncing = true
                        onAutoSyncStarted?.invoke(deviceId)
                        val result = syncLocation(false)
                        isAutoSyncing = false
                        result
                    }
                }
                else -> SyncResult.FAILED_ALREADY_SYNCING
            }
            onAutoSyncFinished?.invoke(deviceId, result)
        }.apply {
            invokeOnCompletion { cause ->
                //If the job was cancelled, send a result back still
                if(cause is CancellationException) {
                    isAutoSyncing = false
                    onAutoSyncFinished?.invoke(deviceId, SyncResult.FAILED_DISCONNECTED)
                }
            }
        }
    }

    private suspend fun syncLocation(onDemand: Boolean): SyncResult = runCatchingOrNull {
        //Getting the battery level ensures the device is local
        var batteryLevel = getBatteryLevel()
            ?: return@runCatchingOrNull SyncResult.FAILED_TO_CONNECT
        if(batteryLevel == BatteryLevel.UNKNOWN) {
            //The tag may still be booting up, wait a few seconds and try again
            delay(15_000L)
            batteryLevel = getBatteryLevel()
                ?: return@runCatchingOrNull SyncResult.FAILED_TO_CONNECT
        }
        val location = smartThingsRepository.getLocation()
            ?: return@runCatchingOrNull SyncResult.FAILED_TO_GET_LOCATION
        val userName = userRepository.getUserInfo()?.fullName
            ?: return@runCatchingOrNull SyncResult.FAILED_TO_SEND
        val userId = encryptedSettingsRepository.userId.getOrNull()?.takeIf {
            it.isNotEmpty()
        } ?: return@runCatchingOrNull SyncResult.FAILED_TO_SEND
        val status = if(isConnectedForLocation()) {
            D2DStatus.GATT_CONNECTED
        }else{
            D2DStatus.BLE_SCANNED
        }
        val sendLocationRequest = SendLocationRequest(
            onDemand = onDemand,
            connectedDevice = SendLocationRequest.ConnectedDevice(
                id = authRepository.getDeviceId(),
                name = Build.MODEL
            ),
            connectedUser = SendLocationRequest.ConnectedUser(
                id = userId,
                name = userName
            ),
            d2dStatus = status,
            geolocation = SendLocationRequest.GeoLocation(
                latitude = location.latitude.toString(),
                longitude = location.longitude.toString(),
                accuracy = location.accuracy.toString(),
                timestamp = System.currentTimeMillis(),
                method = location.provider ?: "",
                battery = batteryLevel
            )
        )
        if(apiRepository.sendLocation(deviceId, sendLocationRequest)) {
            SyncResult.SUCCESS
        }else{
            SyncResult.FAILED_TO_SEND
        }
    } ?: SyncResult.FAILED_OTHER

    fun syncLocation(onDemand: Boolean, resultCallback: (SyncResult) -> Unit = {}) {
        syncLocationJob = scope.launch {
            resultCallback(syncLocation(onDemand))
        }.apply {
            invokeOnCompletion { cause ->
                //If the job was cancelled, send a result back still
                if(cause is CancellationException) {
                    resultCallback.invoke(SyncResult.FAILED_DISCONNECTED)
                }
            }
        }
    }

    suspend fun syncLocationAndWait(onDemand: Boolean): SyncResult {
        var hasResumed = false
        return suspendCoroutine { resume ->
            syncLocation(onDemand) {
                if(!hasResumed) {
                    hasResumed = true
                    resume.resume(it)
                }
            }
        }
    }

    fun close() {
        autoRefreshJob?.cancel()
        syncLocationJob?.cancel()
        scope.cancel()
    }

    abstract suspend fun getBatteryLevel(): BatteryLevel?
    abstract fun isConnectedForLocation(): Boolean

    init {
        autoSyncLocation()
    }

    enum class SyncResult {
        SUCCESS,
        FAILED_TO_CONNECT,
        FAILED_TO_GET_LOCATION,
        FAILED_TO_SEND,
        FAILED_ALREADY_SYNCING,
        FAILED_DISCONNECTED,
        FAILED_AUTO_SYNC_NOT_REQUIRED,
        FAILED_OTHER
    }

}