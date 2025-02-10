package com.kieronquinn.app.utag.components.bluetooth

import com.kieronquinn.app.utag.model.BatteryLevel
import com.kieronquinn.app.utag.model.ButtonVolumeLevel
import com.kieronquinn.app.utag.model.TagStateChangeEvent
import com.kieronquinn.app.utag.model.TagStatusChangeEvent
import com.kieronquinn.app.utag.model.VolumeLevel
import com.kieronquinn.app.utag.utils.extensions.suspendCoroutineWithTimeout
import com.samsung.android.oneconnect.smarttag.service.IGattReadCallback
import com.samsung.android.oneconnect.smarttag.service.IGattRssiCallback
import com.samsung.android.oneconnect.smarttag.service.IGattWriteCallback
import com.samsung.android.oneconnect.smarttag.service.ISmartTagSupportService
import org.koin.core.component.KoinComponent
import kotlin.coroutines.resume

class ConnectedTagConnection(
    override val deviceId: String,
    override val shouldAutoSync: Boolean,
    override val onAutoSyncStarted: ((deviceId: String) -> Unit)?,
    override val onAutoSyncFinished: ((deviceId: String, result: SyncResult) -> Unit)?,
    private val onTagStateChanged: ((deviceId: String, event: TagStatusChangeEvent) -> Unit)?,
    private val service: ISmartTagSupportService
): BaseTagConnection(deviceId, shouldAutoSync, onAutoSyncStarted, onAutoSyncFinished), KoinComponent {

    companion object {
        const val SERVICE_ID = "0000FD5A-0000-1000-8000-00805F9B34FB"
        private const val CHARACTERISTIC_ID_BATTERY = "DEE30004-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_RING = "DEE30001-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_AUDIO_VOLUME = "DEE30002-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_E2E = "DEE30007-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_UWB = "DEE30008-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_UWB_RANGING = "DEE30009-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_BUTTON = "DEE30003-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_BUTTON_VOLUME = "DEE3001F-182D-5496-B1AD-14F216324184"
        private const val CHARACTERISTIC_ID_LOST_URL = "DEE3001B-182D-5496-B1AD-14F216324184"

        private const val TIMEOUT_COMMAND = 10_000L // 10 seconds
    }

    override suspend fun getBatteryLevel(): BatteryLevel? {
        return suspendRunWithService {
            readCharacteristic(CHARACTERISTIC_ID_BATTERY)?.let { level ->
                BatteryLevel.fromLevel(level)
            }
        }
    }

    override fun isConnectedForLocation(): Boolean {
        return true //When syncing location, Tag is always connected
    }

    suspend fun disconnect() {
        suspendRunWithService {
            disconnect(deviceId)
        }
    }

    suspend fun startRinging(): VolumeLevel? {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_RING, true.asHexString())
            readCharacteristic(CHARACTERISTIC_ID_AUDIO_VOLUME)?.let { level ->
                VolumeLevel.fromValue(level)
            }
        }
    }

    suspend fun setRingVolume(volume: VolumeLevel): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_AUDIO_VOLUME, volume.value)
        } ?: false
    }

    suspend fun stopRinging(): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_RING, false.asHexString())
        } ?: false
    }

    suspend fun setButtonConfig(pressEnabled: Boolean, holdEnabled: Boolean): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_BUTTON, "00" + pressEnabled.asHexString())
            writeCharacteristic(CHARACTERISTIC_ID_BUTTON, "01" + holdEnabled.asHexString())
        } ?: false
    }

    suspend fun getButtonVolume(): ButtonVolumeLevel? {
        return suspendRunWithService {
            readCharacteristic(CHARACTERISTIC_ID_BUTTON_VOLUME)?.let { value ->
                ButtonVolumeLevel.fromValue(value)
            }
        }
    }

    suspend fun setButtonVolume(volumeLevel: ButtonVolumeLevel): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_BUTTON_VOLUME, volumeLevel.value)
        } ?: false
    }

    suspend fun getLostModeUrl(): String? {
        return suspendRunWithService {
            readCharacteristic(CHARACTERISTIC_ID_LOST_URL)
        }
    }

    suspend fun setLostModeUrl(url: String): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_LOST_URL, url)
        } ?: false
    }

    suspend fun isE2EEnabled(): Boolean {
        return suspendRunWithService {
            readCharacteristic(CHARACTERISTIC_ID_E2E) == "01"
        } ?: false
    }

    suspend fun setE2EEnabled(enabled: Boolean): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_E2E, enabled.asHexString())
        } ?: false
    }

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun startRanging(config: ByteArray): Boolean {
        return suspendRunWithService {
            if(!writeCharacteristic(CHARACTERISTIC_ID_UWB, true.asHexString())) {
                return@suspendRunWithService false
            }
            writeCharacteristic(CHARACTERISTIC_ID_UWB_RANGING, config.toHexString())
        } ?: false
    }

    suspend fun stopRanging(): Boolean {
        return suspendRunWithService {
            writeCharacteristic(CHARACTERISTIC_ID_UWB, false.asHexString())
        } ?: false
    }

    fun startReadRemoteRssi(refreshRate: Long, callback: IGattRssiCallback): Boolean {
        return runWithService {
            startReadRemoteRssi(deviceId, refreshRate, callback)
            true
        } ?: false
    }

    fun stopReadRemoteRssi(): Boolean {
        return runWithService {
            stopReadRemoteRssi(deviceId)
            true
        } ?: false
    }

    fun onTagStateChanged(event: TagStateChangeEvent) {
        val change = when(event.characteristics.uppercase()) {
            CHARACTERISTIC_ID_BUTTON -> event.toButtonClickEvent()
            CHARACTERISTIC_ID_RING -> event.toRingEvent()
            else -> null
        } ?: return
        onTagStateChanged?.invoke(deviceId, change)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun TagStateChangeEvent.toButtonClickEvent(): TagStatusChangeEvent? {
        val hex = value.toHexString()
        return listOf(
            TagStatusChangeEvent.BUTTON_CLICK,
            TagStatusChangeEvent.BUTTON_LONG_CLICK,
            TagStatusChangeEvent.BUTTON_DOUBLE_CLICK
        ).firstOrNull { it.value == hex }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun TagStateChangeEvent.toRingEvent(): TagStatusChangeEvent? {
        val hex = value.toHexString()
        return listOf(
            TagStatusChangeEvent.RING_START,
            TagStatusChangeEvent.RING_STOP
        ).firstOrNull { it.value == hex }
    }

    private suspend fun ISmartTagSupportService.writeCharacteristic(
        characteristicId: String,
        valueAsHex: String
    ): Boolean = suspendCoroutineWithTimeout(TIMEOUT_COMMAND) {
        writeCharacteristic(
            deviceId,
            SERVICE_ID,
            characteristicId,
            valueAsHex,
            object : IGattWriteCallback.Stub() {
                override fun writeCharacteristic(value: Int) {
                    it.resume(value == 1)
                }
            }
        )
    } ?: false

    /**
     *  Due to a bug in SmartThings, sometimes calling [ISmartTagSupportService.readCharacteristic]
     *  will return the result for an already ongoing read rather than the one we request. If this
     *  happens, [tryReadCharacteristic] will return null and it will try a second time. Currently
     *  this seems to be enough, but it may need tweaking if the bug proves to happen sequentially.
     */
    private suspend fun ISmartTagSupportService.readCharacteristic(
        characteristicId: String
    ): String? {
        tryReadCharacteristic(characteristicId)?.let {
            return it
        }
        return tryReadCharacteristic(characteristicId)
    }

    private suspend fun ISmartTagSupportService.tryReadCharacteristic(
        characteristicId: String
    ): String? = suspendCoroutineWithTimeout(TIMEOUT_COMMAND) {
        readCharacteristic(
            deviceId,
            SERVICE_ID,
            characteristicId,
            object : IGattReadCallback.Stub() {
                override fun onCharacteristicRead(characteristics: String, value: String) {
                    if (characteristics.equals(characteristicId, true)) {
                        it.resume(value)
                    } else {
                        it.resume(null)
                    }
                }
            }
        )
    }

    private suspend fun <T> suspendRunWithService(
        block: suspend ISmartTagSupportService.() -> T
    ): T? {
        return try {
            block(service)
        }catch (e: Exception) {
            null
        }
    }

    private fun <T> runWithService(block: ISmartTagSupportService.() -> T): T? {
        return try {
            block(service)
        }catch (e: Exception) {
            null
        }
    }

    enum class TagConnectionState(val value: Int) {
        D2D_CONNECTED(1),
        D2D_SCANNED(2),
        DEFAULT(3);

        companion object {
            fun fromValue(value: Int): TagConnectionState {
                return entries.firstOrNull { it.value == value } ?: DEFAULT
            }
        }
    }

    private fun Boolean.asHexString(): String {
        return if(this) "01" else "00"
    }

}