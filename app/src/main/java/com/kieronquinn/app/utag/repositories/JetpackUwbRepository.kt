package com.kieronquinn.app.utag.repositories

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.uwb.RangingMeasurement
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbComplexChannel
import androidx.core.uwb.UwbControleeSessionScope
import androidx.core.uwb.UwbDevice
import androidx.core.uwb.UwbManager
import androidx.core.uwb.exceptions.UwbHardwareNotAvailableException
import com.google.uwb.support.fira.FiraOpenSessionParams
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import com.kieronquinn.app.utag.components.uwb.UwbConfig
import com.kieronquinn.app.utag.repositories.UwbRepository.Companion.randomCodeIndex
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbEvent
import com.kieronquinn.app.utag.repositories.UwbRepository.UwbState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

/**
 *  UWB handling using AndroidX, which runs via Google Play Services
 */
@RequiresApi(Build.VERSION_CODES.S)
class JetpackUwbRepository(context: Context): UwbRepository {

    private val uwbManager = UwbManager.createInstance(context)
    private val uwbScope = MainScope()

    override val permission = Manifest.permission.UWB_RANGING

    override suspend fun isUwbAvailable(): Boolean {
        return uwbManager.isHardwarePresent()
    }

    override suspend fun getUwbState(): UwbState {
        return when {
            !isUwbAvailable() -> UwbState.UNAVAILABLE
            !uwbManager.isAvailable() -> UwbState.DISABLED
            else -> UwbState.AVAILABLE
        }
    }

    override fun startRanging(
        scope: CoroutineScope,
        tagConnection: RemoteTagConnection,
        onEnd: () -> Unit
    ): Flow<UwbEvent?> {
        return flow {
            emit(uwbManager.controleeSessionScope())
        }.flatMapConcat {
            val config = UwbConfig(randomCodeIndex())
            it.range(config, tagConnection, onEnd)
        }
    }

    private fun UwbControleeSessionScope.range(
        config: UwbConfig,
        tagConnection: RemoteTagConnection,
        onEnd: () -> Unit
    ): Flow<UwbEvent> {
        return prepareSession(config.getParams().toRangingParameters()).mapNotNull {
            when(it) {
                is RangingResult.RangingResultInitialized -> {
                    if(tagConnection.startRanging(config, it.device.address)) {
                        UwbEvent.Started
                    }else{
                        onEnd()
                        UwbEvent.Failed(null)
                    }
                }
                is RangingResult.RangingResultPosition -> {
                    UwbEvent.Report(
                        it.position.azimuth?.toRadians(),
                        it.position.elevation?.toRadians(),
                        it.position.distance ?: return@mapNotNull null
                    )
                }
                is RangingResult.RangingResultPeerDisconnected -> {
                    onEnd()
                    UwbEvent.Ended
                }
                else -> null
            }
        }.takeWhile {
            it is UwbEvent.Started || it is UwbEvent.Report
        }.onCompletion {
            uwbScope.launch {
                tagConnection.stopRanging()
            }
        }
    }

    private fun FiraOpenSessionParams.toRangingParameters(): RangingParameters {
        return RangingParameters(
            RangingParameters.CONFIG_UNICAST_DS_TWR,
            sessionId,
            0,
            staticStsIV!! + vendorId!!,
            null,
            UwbComplexChannel(channelNumber, preambleCodeIndex),
            listOf(UwbDevice.createForAddress(destAddressList.first().address)),
            RangingParameters.RANGING_UPDATE_RATE_AUTOMATIC,
            isAoaDisabled = false
        )
    }

    private suspend fun UwbManager.isHardwarePresent(): Boolean {
        return try {
            isAvailable()
            true
        }catch (e: UwbHardwareNotAvailableException) {
            false
        }
    }

    private fun RangingMeasurement.toRadians(): RangingMeasurement {
        return RangingMeasurement(Math.toRadians(value.toDouble()).toFloat())
    }

}