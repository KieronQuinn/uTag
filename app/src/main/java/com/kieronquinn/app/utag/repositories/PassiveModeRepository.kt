package com.kieronquinn.app.utag.repositories

import android.content.Context
import com.kieronquinn.app.utag.Application.Companion.isMainProcess
import com.kieronquinn.app.utag.model.database.PassiveModeConfig
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.providers.PassiveModeProvider
import com.kieronquinn.app.utag.utils.extensions.firstNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

interface PassiveModeRepository {
    val passiveModeTemporaryDisable: StateFlow<Boolean?>
    val passiveModeConfigs: StateFlow<List<PassiveModeConfig>?>
    fun isInPassiveMode(
        deviceId: String,
        ignoreBypass: Boolean = false,
        bypassOnly: Boolean = false
    ): Boolean
    fun isInPassiveModeAsFlow(deviceId: String): Flow<Boolean>
    fun setPassiveMode(deviceId: String, enabled: Boolean)
    suspend fun getBackups(): List<PassiveModeConfig.Backup>
    suspend fun restoreBackup(backups: List<PassiveModeConfig.Backup>)

    /**
     *  Set whether a Tag should be allowed to connect, without the app open. This is used for
     *  preventing overmature offline states, and can handle multiple device IDs at once
     */
    fun setTemporaryOverride(deviceId: String, enabled: Boolean)

    /**
     *  SmartThings has closed, disconnect all Passive Mode Tags
     */
    fun onSmartThingsPaused()

    /**
     *  SmartThings has opened, allow connecting to Passive Mode Tags
     */
    fun onSmartThingsResumed()

    /**
     *  Check the current state of the SmartThings app and update the local flow
     */
    suspend fun checkSmartThingsForeground()
}

class PassiveModeRepositoryImpl(
    private val smartThingsRepository: SmartThingsRepository,
    private val context: Context,
    database: UTagDatabase
): PassiveModeRepository {

    companion object {
        //Delay after an app is background before checking if it's in the foreground
        private const val DELAY_PAUSE = 2500L
    }

    private val table = database.passiveModeConfigTable()
    private val scope = MainScope()
    private val smartThingsForeground = MutableStateFlow(false)
    private val temporaryDeviceIds = HashSet<String>()
    private var smartThingsResumeJob: Job? = null
    private var smartThingsPauseJob: Job? = null

    override val passiveModeTemporaryDisable = smartThingsForeground
        .stateIn(scope, SharingStarted.Eagerly, null)

    override val passiveModeConfigs = table.getConfigs()
        .flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Eagerly, null)

    override fun isInPassiveMode(
        deviceId: String,
        ignoreBypass: Boolean,
        bypassOnly: Boolean
    ): Boolean {
        if(isMainProcess()) {
            return PassiveModeProvider.isInPassiveMode(context, deviceId, ignoreBypass, bypassOnly)
        }else{
            if(!ignoreBypass) {
                //Override for if SmartThings or uTag are in the foreground
                if (passiveModeTemporaryDisable.value == true) return false
                //Override if the device is temporarily allowlisted
                synchronized(temporaryDeviceIds) {
                    if(temporaryDeviceIds.contains(deviceId)) return false
                }
            }
            if(bypassOnly) return false
            val hash = deviceId.hashCode()
            return runBlocking {
                passiveModeConfigs.firstNotNull().firstOrNull { it.deviceIdHash == hash }
                    ?.enabled ?: false
            }
        }
    }

    override fun isInPassiveModeAsFlow(deviceId: String): Flow<Boolean> {
        val hash = deviceId.hashCode()
        return passiveModeConfigs.filterNotNull().map {
            it.firstOrNull { item -> item.deviceIdHash == hash }?.enabled ?: false
        }
    }

    override fun setPassiveMode(deviceId: String, enabled: Boolean) {
        scope.launch {
            withContext(Dispatchers.IO) {
                table.insert(PassiveModeConfig(deviceId.hashCode(), enabled))
            }
            PassiveModeProvider.notifyChange(context, deviceId.hashCode().toString(), enabled)
        }
    }

    override suspend fun getBackups(): List<PassiveModeConfig.Backup> {
        return withContext(Dispatchers.IO) {
            table.getConfigs().first().map {
                it.toBackup()
            }
        }
    }

    override suspend fun restoreBackup(backups: List<PassiveModeConfig.Backup>) {
        val configs = backups.map { it.toConfig() }
        withContext(Dispatchers.IO) {
            configs.forEach {
                table.insert(it)
                PassiveModeProvider.notifyChange(context, it.deviceIdHash.toString(), it.enabled)
            }
        }
    }

    override fun setTemporaryOverride(deviceId: String, enabled: Boolean) {
        synchronized(temporaryDeviceIds) {
            if(enabled) {
                temporaryDeviceIds.add(deviceId)
            }else{
                temporaryDeviceIds.remove(deviceId)
            }
        }
    }

    override fun onSmartThingsPaused() {
        smartThingsPauseJob?.cancel()
        smartThingsResumeJob?.cancel()
        smartThingsPauseJob = scope.launch {
            //Give app time to leave foreground
            delay(DELAY_PAUSE)
            checkSmartThingsForeground()
        }
    }

    override fun onSmartThingsResumed() {
        smartThingsResumeJob?.cancel()
        smartThingsPauseJob?.cancel()
        smartThingsResumeJob = scope.launch {
            checkSmartThingsForeground()
        }
    }

    override suspend fun checkSmartThingsForeground() {
        val foreground = smartThingsRepository.isForeground()
        if(foreground != smartThingsForeground.value) {
            smartThingsForeground.emit(foreground)
        }
    }

}