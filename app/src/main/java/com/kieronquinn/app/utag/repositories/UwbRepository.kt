package com.kieronquinn.app.utag.repositories

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.core.uwb.RangingMeasurement
import com.google.uwb.support.fira.FiraParams.UWB_PREAMBLE_CODE_INDEX_10
import com.google.uwb.support.fira.FiraParams.UWB_PREAMBLE_CODE_INDEX_11
import com.google.uwb.support.fira.FiraParams.UWB_PREAMBLE_CODE_INDEX_12
import com.google.uwb.support.fira.FiraParams.UWB_PREAMBLE_CODE_INDEX_9
import com.kieronquinn.app.utag.components.bluetooth.RemoteTagConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface UwbRepository {

    companion object {
        @SuppressLint("NewApi")
        fun getImplementation(context: Context): UwbRepository {
            return when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    JetpackUwbRepository(context)
                }
                else -> NoOpUwbRepository()
            }
        }

        /**
         *  Generates a random **supported** code index. Other code indexes are not supported.
         */
        @JvmStatic
        fun randomCodeIndex(): Int {
            return setOf(
                UWB_PREAMBLE_CODE_INDEX_9,
                UWB_PREAMBLE_CODE_INDEX_10,
                UWB_PREAMBLE_CODE_INDEX_11,
                UWB_PREAMBLE_CODE_INDEX_12
            ).random()
        }
    }

    val permission: String?

    /**
     *  Does a basic check of if UWB hardware is present.
     */
    suspend fun isUwbAvailable(): Boolean

    /**
     *  Returns the [UwbState] of UWB on the device, including if it's enabled.
     */
    suspend fun getUwbState(): UwbState

    /**
     *  Starts UWB ranging for a given [RemoteTagConnection]
     */
    fun startRanging(
        scope: CoroutineScope,
        tagConnection: RemoteTagConnection,
        onEnd: () -> Unit = {}
    ): Flow<UwbEvent?>

    enum class UwbState {
        /**
         *  UWB is not available on this device. Either the hardware is not present, or the OS does
         *  not support it.
         */
        UNAVAILABLE,

        /**
         *  UWB is available on this device, but it is disabled. User should be prompted to open
         *  settings to enable it.
         */
        DISABLED,

        /**
         *  UWB is available and ready to use.
         */
        AVAILABLE
    }

    sealed class UwbEvent {
        /**
         *  Ranging failed to start. No further updates will be sent.
         */
        data class Failed(val reason: Int? = null): UwbEvent()

        /**
         *  Ranging has started, reports should follow
         */
        data object Started: UwbEvent()

        /**
         *  Report from `onReportReceived` has been received
         */
        data class Report(
            val azimuth: RangingMeasurement?,
            val altitude: RangingMeasurement?,
            val distance: RangingMeasurement,
            val corrected: Boolean = false
        ): UwbEvent()

        /**
         *  Ranging has ended. No further updates will be sent.
         */
        data object Ended: UwbEvent()
    }

}