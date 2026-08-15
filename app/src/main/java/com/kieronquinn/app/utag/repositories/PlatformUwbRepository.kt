package com.kieronquinn.app.utag.repositories

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.ranging.DataNotificationConfig
import android.ranging.RangingCapabilities
import android.ranging.RangingData
import android.ranging.RangingDevice
import android.ranging.RangingManager
import android.ranging.RangingPreference
import android.ranging.RangingSession
import android.ranging.SessionConfig
import android.ranging.raw.RawRangingDevice
import android.ranging.raw.RawResponderRangingConfig
import android.ranging.uwb.UwbAddress
import android.ranging.uwb.UwbComplexChannel
import android.ranging.uwb.UwbRangingParams
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.UwbAddress as JetpackUwbAddress
import com.google.uwb.support.fira.FiraOpenSessionParams
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.components.uwb.UwbConfig
import com.kieronquinn.app.utag.repositories.UwbRepository.Companion.randomCodeIndex
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbEvent
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.coroutines.resume

/**
 * UWB handling using Android 16's platform ranging API, without a Google Play Services backend.
 */
@RequiresApi(36)
class PlatformUwbRepository(private val context: Context): UwbRepository {

    private val rangingManager = context.getSystemService(RangingManager::class.java)
    private val uwbScope = MainScope()

    override val permission = Manifest.permission.RANGING

    override suspend fun isUwbAvailable(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_UWB)
    }

    override suspend fun getUwbState(): UwbState {
        if(!isUwbAvailable()) return UwbState.UNAVAILABLE
        return when(getUwbAvailability()) {
            RangingCapabilities.ENABLED -> UwbState.AVAILABLE
            RangingCapabilities.DISABLED_REGULATORY,
            RangingCapabilities.DISABLED_USER,
            RangingCapabilities.DISABLED_USER_RESTRICTIONS -> UwbState.DISABLED
            else -> UwbState.UNAVAILABLE
        }
    }

    override fun startRanging(
        scope: CoroutineScope,
        tagConnection: RemoteTagConnection,
        onEnd: () -> Unit
    ): Flow<UwbEvent?> {
        return callbackFlow {
            debugLog("Starting Android platform UWB ranging")
            val finished = AtomicBoolean(false)
            var angleUnit = AngleUnit.RADIANS

            fun finish(event: UwbEvent, notifyEnd: Boolean = true) {
                if(!finished.compareAndSet(false, true)) return
                if(notifyEnd) onEnd()
                trySend(event)
                close()
            }

            val config = UwbConfig(randomCodeIndex())
            val localAddress = UwbAddress.createRandomShortAddress()
            val preference = try {
                config.getParams().toRangingPreference(localAddress)
            }catch (e: Exception) {
                debugLog("Failed to create ranging preference", e)
                finish(UwbEvent.Failed(null))
                return@callbackFlow
            }

            val callback = object: RangingSession.Callback {
                override fun onOpened() {
                    debugLog("Platform ranging session opened; requesting tag ranging start")
                    launch {
                        val jetpackAddress = JetpackUwbAddress(localAddress.addressBytes)
                        val startResult = runCatching {
                            tagConnection.startRanging(config, jetpackAddress)
                        }
                        val didStart = startResult.getOrDefault(false)
                        startResult.exceptionOrNull()?.let {
                            debugLog("Tag ranging start threw an exception", it)
                        }
                        debugLog("Tag ranging start acknowledged=$didStart")
                        if(didStart) {
                            trySend(UwbEvent.Started)
                        }else{
                            finish(UwbEvent.Failed(null))
                        }
                    }
                }

                override fun onOpenFailed(reason: Int) {
                    debugLog("Platform ranging session failed to open: reason=$reason")
                    finish(UwbEvent.Failed(reason))
                }

                override fun onStarted(peer: RangingDevice, technology: Int) {
                    debugLog("Platform ranging started: technology=$technology")
                }

                override fun onResults(peer: RangingDevice, data: RangingData) {
                    val distance = data.distance ?: return
                    if(angleUnit == AngleUnit.RADIANS && data.hasAngleOutsideRadianRange()) {
                        angleUnit = AngleUnit.DEGREES
                        debugLog("Raw angle exceeded +/-PI; switching session to degrees mode")
                    }
                    if(BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "Raw result: distance=${distance.toDebugString("m")}, " +
                                    "azimuth=${data.azimuth.toDebugString(angleUnit.label)}, " +
                                    "elevation=${data.elevation.toDebugString(angleUnit.label)}, " +
                                    "angleUnit=$angleUnit, " +
                                    "technology=${data.rangingTechnology}, " +
                                    "timestampMs=${data.timestampMillis}"
                        )
                    }
                    trySend(
                        UwbEvent.Report(
                            data.azimuth?.toJetpackAngle(angleUnit),
                            data.elevation?.toJetpackAngle(angleUnit),
                            RangingMeasurement(distance.measurement.toFloat())
                        )
                    )
                }

                override fun onStopped(peer: RangingDevice, technology: Int) {
                    debugLog("Platform ranging stopped: technology=$technology")
                    finish(UwbEvent.Ended)
                }

                override fun onClosed(reason: Int) {
                    debugLog("Platform ranging session closed: reason=$reason")
                    if(reason == RangingSession.Callback.REASON_LOCAL_REQUEST) {
                        finished.compareAndSet(false, true)
                        close()
                    }else{
                        finish(UwbEvent.Ended)
                    }
                }
            }

            val cancellationSignal = try {
                val session = rangingManager.createRangingSession(context.mainExecutor, callback)
                    ?: error("Unable to create ranging session")
                session.start(preference)
            }catch (e: Exception) {
                debugLog("Failed to create or start platform ranging session", e)
                finish(UwbEvent.Failed(null))
                null
            }

            awaitClose {
                cancellationSignal?.cancel()
            }
        }.onCompletion {
            uwbScope.launch {
                tagConnection.stopRanging()
            }
        }
    }

    private suspend fun getUwbAvailability(): Int? {
        return withTimeoutOrNull(CAPABILITIES_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val completed = AtomicBoolean(false)
                lateinit var callback: RangingManager.RangingCapabilitiesCallback
                callback = RangingManager.RangingCapabilitiesCallback { capabilities ->
                    if(completed.compareAndSet(false, true)) {
                        runCatching {
                            rangingManager.unregisterCapabilitiesCallback(callback)
                        }
                        continuation.resume(
                            capabilities.technologyAvailability[RangingManager.UWB]
                        )
                    }
                }
                continuation.invokeOnCancellation {
                    if(completed.compareAndSet(false, true)) {
                        runCatching {
                            rangingManager.unregisterCapabilitiesCallback(callback)
                        }
                    }
                }
                try {
                    rangingManager.registerCapabilitiesCallback(context.mainExecutor, callback)
                }catch (_: Exception) {
                    if(completed.compareAndSet(false, true)) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }

    private fun FiraOpenSessionParams.toRangingPreference(
        localAddress: UwbAddress
    ): RangingPreference {
        val peerAddress = UwbAddress.fromBytes(destAddressList.first().address)
        val rangingParams = UwbRangingParams.Builder(
            sessionId,
            UwbRangingParams.CONFIG_UNICAST_DS_TWR,
            localAddress,
            peerAddress
        ).setComplexChannel(
            UwbComplexChannel.Builder()
                .setChannel(channelNumber)
                .setPreambleIndex(preambleCodeIndex)
                .build()
        ).setRangingUpdateRate(
            RawRangingDevice.UPDATE_RATE_NORMAL
        ).setSessionKeyInfo(
            staticStsIV!! + vendorId!!
        ).setSlotDuration(
            UwbRangingParams.DURATION_2_MS
        ).build()

        val rangingDevice = RawRangingDevice.Builder()
            .setRangingDevice(RangingDevice.Builder().build())
            .setUwbRangingParams(rangingParams)
            .build()
        val rangingConfig = RawResponderRangingConfig.Builder()
            .setRawRangingDevice(rangingDevice)
            .build()
        val sessionConfig = SessionConfig.Builder()
            .setAngleOfArrivalNeeded(true)
            .setDataNotificationConfig(
                DataNotificationConfig.Builder()
                    .setNotificationConfigType(
                        DataNotificationConfig.NOTIFICATION_CONFIG_ENABLE
                    )
                    .build()
            ).build()
        return RangingPreference.Builder(
            RangingPreference.DEVICE_ROLE_RESPONDER,
            rangingConfig
        ).setSessionConfig(sessionConfig).build()
    }

    private fun RangingData.hasAngleOutsideRadianRange(): Boolean {
        return listOfNotNull(azimuth, elevation).any {
            abs(it.measurement) > Math.PI
        }
    }

    private fun android.ranging.RangingMeasurement.toJetpackAngle(
        angleUnit: AngleUnit
    ): RangingMeasurement {
        val radians = when(angleUnit) {
            AngleUnit.RADIANS -> measurement
            AngleUnit.DEGREES -> Math.toRadians(measurement)
        }
        return RangingMeasurement(radians.toFloat())
    }

    private fun android.ranging.RangingMeasurement?.toDebugString(unit: String): String {
        return this?.let {
            "${it.measurement}$unit (confidence=${it.confidence})"
        } ?: "null"
    }

    private fun debugLog(message: String, throwable: Throwable? = null) {
        if(!BuildConfig.DEBUG) return
        if(throwable != null) {
            Log.e(TAG, message, throwable)
        }else{
            Log.d(TAG, message)
        }
    }

    companion object {
        private const val TAG = "uTagUwb"
        private const val CAPABILITIES_TIMEOUT_MILLIS = 2_000L
    }

    private enum class AngleUnit(val label: String) {
        RADIANS("rad"),
        DEGREES("deg")
    }

}
