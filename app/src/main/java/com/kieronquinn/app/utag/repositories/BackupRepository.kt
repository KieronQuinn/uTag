package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.database.AcknowledgedUnknownTag
import com.kieronquinn.app.utag.model.database.AutomationConfig
import com.kieronquinn.app.utag.model.database.FindMyDeviceConfig
import com.kieronquinn.app.utag.model.database.LocationSafeArea
import com.kieronquinn.app.utag.model.database.NotifyDisconnectConfig
import com.kieronquinn.app.utag.model.database.PassiveModeConfig
import com.kieronquinn.app.utag.model.database.WiFiSafeArea
import com.kieronquinn.app.utag.repositories.BackupRepository.LoadBackupResult
import com.kieronquinn.app.utag.repositories.BackupRepository.RestoreConfig
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackup
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackupProgress
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagBackupProgress.ErrorReason
import com.kieronquinn.app.utag.repositories.BackupRepository.UTagRestoreProgress
import com.kieronquinn.app.utag.utils.extensions.gzip
import com.kieronquinn.app.utag.utils.extensions.hasBackgroundLocationPermission
import com.kieronquinn.app.utag.utils.extensions.hasLocationPermissions
import com.kieronquinn.app.utag.utils.extensions.ungzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import com.kieronquinn.app.utag.repositories.BackupRepository.LoadBackupResult.ErrorReason as LoadErrorReason

interface BackupRepository {

    fun createBackup(toUri: Uri): Flow<UTagBackupProgress>
    suspend fun loadBackup(fromUri: Uri): LoadBackupResult
    fun restoreBackup(backup: UTagBackup, config: RestoreConfig): Flow<UTagRestoreProgress>

    @Parcelize
    data class UTagBackup(
        @SerializedName("version")
        val version: Int = 1,
        @SerializedName("timestamp")
        val timestamp: Long = System.currentTimeMillis(),
        @SerializedName("automation_configs")
        val automationConfigs: List<AutomationConfig.Backup>?,
        @SerializedName("find_my_device_configs")
        val findMyDeviceConfigs: List<FindMyDeviceConfig.Backup>?,
        @SerializedName("location_safe_areas")
        val locationSafeAreas: List<LocationSafeArea.Backup>?,
        @SerializedName("notify_disconnect_configs")
        val notifyDisconnectConfigs: List<NotifyDisconnectConfig.Backup>?,
        @SerializedName("passive_mode_configs")
        val passiveModeConfigs: List<PassiveModeConfig.Backup>?,
        @SerializedName("wifi_safe_areas")
        val wifiSafeAreas: List<WiFiSafeArea.Backup>?,
        @SerializedName("unknown_tags")
        val unknownTags: List<AcknowledgedUnknownTag.Backup>?,
        @SerializedName("settings")
        val settings: Map<String, String>?,
        @SerializedName("encrypted_settings")
        val encryptedSettings: Map<String, String>?
    ): Parcelable

    sealed class UTagBackupProgress {
        data object CreatingBackup: UTagBackupProgress()
        data object CreatingAutomationConfigsBackup: UTagBackupProgress()
        data object CreatingFindMyDeviceConfigsBackup: UTagBackupProgress()
        data object CreatingLocationSafeAreasBackup: UTagBackupProgress()
        data object CreatingNotifyDisconnectConfigsBackup: UTagBackupProgress()
        data object CreatingPassiveModeConfigsBackup: UTagBackupProgress()
        data object CreatingWiFiSafeAreasBackup: UTagBackupProgress()
        data object CreatingSettingsBackup: UTagBackupProgress()
        data object CreatingUnknownTagsBackup: UTagBackupProgress()
        data object WritingFile: UTagBackupProgress()
        data class Finished(val filename: String?): UTagBackupProgress()
        data class Error(val reason: ErrorReason): UTagBackupProgress()

        enum class ErrorReason(@StringRes val description: Int) {
            FAILED_TO_CREATE_FILE(R.string.backup_error_failed_to_create),
            FAILED_TO_WRITE_FILE(R.string.backup_error_failed_to_write)
        }
    }

    sealed class UTagRestoreProgress {
        data object RestoringBackup: UTagRestoreProgress()
        data object RestoringAutomationConfigsBackup: UTagRestoreProgress()
        data object RestoringFindMyDeviceConfigsBackup: UTagRestoreProgress()
        data object RestoringLocationSafeAreasBackup: UTagRestoreProgress()
        data object RestoringNotifyDisconnectConfigsBackup: UTagRestoreProgress()
        data object RestoringPassiveModeConfigsBackup: UTagRestoreProgress()
        data object RestoringWiFiSafeAreasBackup: UTagRestoreProgress()
        data object RestoringSettingsBackup: UTagRestoreProgress()
        data object RestoringUnknownTagsBackup: UTagRestoreProgress()
        data object Finished: UTagRestoreProgress()
    }

    sealed class LoadBackupResult {
        data class Success(val backup: UTagBackup, val config: RestoreConfig): LoadBackupResult()
        data class Error(val reason: LoadErrorReason): LoadBackupResult()

        enum class ErrorReason(@StringRes val description: Int) {
            FAILED_TO_READ_FILE(R.string.restore_error_failed_to_read),
            FAILED_TO_LOAD_BACKUP(R.string.restore_error_failed_to_load)
        }
    }

    @Parcelize
    data class RestoreConfig(
        val hasAutomationConfigs: Boolean,
        val hasFindMyDeviceConfigs: Boolean,
        val hasLocationSafeAreas: Boolean,
        val hasNotifyDisconnectConfigs: Boolean,
        val hasPassiveModeConfigs: Boolean,
        val hasWifiSafeAreas: Boolean,
        val hasSettings: Boolean,
        val hasUnknownTags: Boolean,
        val shouldRestoreAutomationConfigs: Boolean = hasAutomationConfigs,
        val shouldRestoreFindMyDeviceConfigs: Boolean = hasFindMyDeviceConfigs,
        val shouldRestoreLocationSafeAreas: Boolean = hasLocationSafeAreas,
        val shouldRestoreNotifyDisconnectConfigs: Boolean = hasNotifyDisconnectConfigs,
        val shouldRestorePassiveModeConfigs: Boolean = hasPassiveModeConfigs,
        val shouldRestoreWifiSafeAreas: Boolean = hasWifiSafeAreas,
        val shouldRestoreSettings: Boolean = hasSettings,
        val shouldRestoreUnknownTags: Boolean = hasUnknownTags
    ): Parcelable

}

class BackupRepositoryImpl(
    private val context: Context,
    private val gson: Gson,
    private val automationRepository: AutomationRepository,
    private val findMyDeviceRepository: FindMyDeviceRepository,
    private val safeAreaRepository: SafeAreaRepository,
    private val settingsRepository: SettingsRepository,
    private val encryptedSettingsRepository: EncryptedSettingsRepository,
    private val passiveModeRepository: PassiveModeRepository,
    private val nonOwnerTagRepository: NonOwnerTagRepository
): BackupRepository {

    override fun createBackup(toUri: Uri) = flow {
        emit(UTagBackupProgress.CreatingBackup)
        val outFile = DocumentFile.fromSingleUri(context, toUri) ?: run {
            emit(UTagBackupProgress.Error(ErrorReason.FAILED_TO_CREATE_FILE))
            return@flow
        }
        val outStream = context.contentResolver.openOutputStream(toUri) ?: run {
            emit(UTagBackupProgress.Error(ErrorReason.FAILED_TO_WRITE_FILE))
            return@flow
        }
        emit(UTagBackupProgress.CreatingAutomationConfigsBackup)
        val automationConfigs = automationRepository.getBackups()
        emit(UTagBackupProgress.CreatingFindMyDeviceConfigsBackup)
        val findMyDeviceConfigs = findMyDeviceRepository.getBackups()
        emit(UTagBackupProgress.CreatingLocationSafeAreasBackup)
        val locationSafeAreas = safeAreaRepository.getLocationBackups()
        emit(UTagBackupProgress.CreatingNotifyDisconnectConfigsBackup)
        val notifyDisconnectConfigs = safeAreaRepository.getNotifyDisconnectConfigBackups()
        emit(UTagBackupProgress.CreatingPassiveModeConfigsBackup)
        val passiveModeConfigs = passiveModeRepository.getBackups()
        emit(UTagBackupProgress.CreatingWiFiSafeAreasBackup)
        val wifiSafeAreas = safeAreaRepository.getWiFiBackups()
        emit(UTagBackupProgress.CreatingUnknownTagsBackup)
        val unknownTagsBackup = nonOwnerTagRepository.getBackup()
        emit(UTagBackupProgress.CreatingSettingsBackup)
        val settingsBackup = settingsRepository.getBackup()
        val encryptedSettingsBackup = encryptedSettingsRepository.getBackup()
        val backup = UTagBackup(
            automationConfigs = automationConfigs,
            findMyDeviceConfigs = findMyDeviceConfigs,
            locationSafeAreas = locationSafeAreas,
            notifyDisconnectConfigs = notifyDisconnectConfigs,
            passiveModeConfigs = passiveModeConfigs,
            wifiSafeAreas = wifiSafeAreas,
            settings = settingsBackup,
            encryptedSettings = encryptedSettingsBackup,
            unknownTags = unknownTagsBackup
        )
        emit(UTagBackupProgress.WritingFile)
        outStream.buffered().use {
            it.write(gson.toJson(backup).gzip())
            it.flush()
        }
        outStream.flush()
        outStream.close()
        emit(UTagBackupProgress.Finished(outFile.name))
    }.flowOn(Dispatchers.IO)

    override suspend fun loadBackup(fromUri: Uri) = withContext(Dispatchers.IO) {
        val inStream = context.contentResolver.openInputStream(fromUri)
            ?: return@withContext LoadBackupResult.Error(LoadErrorReason.FAILED_TO_READ_FILE)
        val bytes = inStream.buffered().use {
            it.readBytes()
        }
        val backup = try {
            gson.fromJson(bytes.ungzip(), UTagBackup::class.java)
        }catch (e: Exception){
            inStream.close()
            return@withContext LoadBackupResult.Error(LoadErrorReason.FAILED_TO_LOAD_BACKUP)
        }
        inStream.close()
        val config = RestoreConfig(
            hasAutomationConfigs = !backup.automationConfigs.isNullOrEmpty(),
            hasFindMyDeviceConfigs = !backup.findMyDeviceConfigs.isNullOrEmpty(),
            hasLocationSafeAreas = !backup.locationSafeAreas.isNullOrEmpty(),
            hasNotifyDisconnectConfigs = !backup.notifyDisconnectConfigs.isNullOrEmpty(),
            hasPassiveModeConfigs = !backup.passiveModeConfigs.isNullOrEmpty(),
            hasWifiSafeAreas = !backup.wifiSafeAreas.isNullOrEmpty(),
            hasUnknownTags = !backup.unknownTags.isNullOrEmpty(),
            hasSettings = !backup.settings.isNullOrEmpty() || !backup.encryptedSettings.isNullOrEmpty()
        )
        LoadBackupResult.Success(backup, config)
    }

    override fun restoreBackup(backup: UTagBackup, config: RestoreConfig) = flow {
        val hasLocationPermission = context.hasLocationPermissions() &&
                context.hasBackgroundLocationPermission()
        emit(UTagRestoreProgress.RestoringBackup)
        if(config.hasAutomationConfigs && config.shouldRestoreAutomationConfigs
            && backup.automationConfigs != null) {
            emit(UTagRestoreProgress.RestoringAutomationConfigsBackup)
            automationRepository.restoreBackup(backup.automationConfigs)
        }
        if(config.hasFindMyDeviceConfigs && config.shouldRestoreFindMyDeviceConfigs
            && backup.findMyDeviceConfigs != null) {
            emit(UTagRestoreProgress.RestoringFindMyDeviceConfigsBackup)
            findMyDeviceRepository.restoreBackup(backup.findMyDeviceConfigs)
        }
        if(config.hasLocationSafeAreas && config.shouldRestoreLocationSafeAreas
            && hasLocationPermission && backup.locationSafeAreas != null) {
            emit(UTagRestoreProgress.RestoringLocationSafeAreasBackup)
            safeAreaRepository.restoreLocationBackup(backup.locationSafeAreas)
        }
        if(config.hasNotifyDisconnectConfigs && config.shouldRestoreNotifyDisconnectConfigs
            && backup.notifyDisconnectConfigs != null) {
            emit(UTagRestoreProgress.RestoringNotifyDisconnectConfigsBackup)
            safeAreaRepository.restoreNotifyDisconnectConfigBackup(backup.notifyDisconnectConfigs)
        }
        if(config.hasPassiveModeConfigs && config.shouldRestorePassiveModeConfigs
            && backup.passiveModeConfigs != null) {
            emit(UTagRestoreProgress.RestoringPassiveModeConfigsBackup)
            passiveModeRepository.restoreBackup(backup.passiveModeConfigs)
        }
        if(config.hasWifiSafeAreas && config.shouldRestoreWifiSafeAreas && hasLocationPermission
            && backup.wifiSafeAreas != null) {
            emit(UTagRestoreProgress.RestoringWiFiSafeAreasBackup)
            safeAreaRepository.restoreWiFiBackup(backup.wifiSafeAreas)
        }
        if(config.hasUnknownTags && config.shouldRestoreUnknownTags && backup.unknownTags != null) {
            emit(UTagRestoreProgress.RestoringUnknownTagsBackup)
            nonOwnerTagRepository.restoreBackup(backup.unknownTags)
        }
        if(config.hasSettings && config.shouldRestoreSettings) {
            emit(UTagRestoreProgress.RestoringSettingsBackup)
            if(backup.settings != null) {
                settingsRepository.restoreBackup(backup.settings)
            }
            if(backup.encryptedSettings != null) {
                encryptedSettingsRepository.restoreBackup(backup.encryptedSettings)
            }
        }
        emit(UTagRestoreProgress.Finished)
    }.flowOn(Dispatchers.IO)

}