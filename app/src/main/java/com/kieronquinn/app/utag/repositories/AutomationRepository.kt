package com.kieronquinn.app.utag.repositories

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.model.database.AutomationConfig
import com.kieronquinn.app.utag.model.database.UTagDatabase
import com.kieronquinn.app.utag.repositories.RulesRepository.TagButtonAction
import com.kieronquinn.app.utag.service.UTagLaunchIntentService.Companion.ACTION_CAMERA
import com.kieronquinn.app.utag.utils.extensions.wasInstalledWithSession
import com.kieronquinn.app.utag.utils.tasker.TaskerIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface AutomationRepository {

    /**
     *  Whether the user has granted the overlay permission to uTag
     */
    fun hasOverlayPermission(): Boolean

    /**
     *  On Android 15, the "Display over other apps" permission will at some point require
     *  restricted settings access. It's not possible to know whether the user has granted
     *  restricted settings, but we can check for scenarios where they *don't* need to - if the
     *  permission is already granted, if the app was installed using a session-based installer or
     *  ADB.
     *
     *  If the prompt is maybe required, a screen asking them to grant will be shown as usual with
     *  extra info on restricted settings and how to grant it.
     */
    suspend fun maybeRequiresRestrictedSettingsPrompt(): Boolean

    /**
     *  Gets the remote automation rules and caches whether they're enabled for each Tag in the
     *  local [AutomationConfig] database. This is run in both the service when the Tag button is
     *  clicked, and in the settings, but if it fails in the service we don't fail the action.
     */
    suspend fun syncRemoteRuleStates(): Boolean

    /**
     *  Gets the current, local [AutomationConfig] for a [deviceId]. Call [syncRemoteRuleStates]
     *  to update the local state with the remote setup first.
     */
    suspend fun getAutomationConfig(deviceId: String): AutomationConfig?

    /**
     *  As [getAutomationConfig] but backed by the database. [syncRemoteRuleStates] is still
     *  required to sync the remote state.
     */
    fun getAutomationConfigAsFlow(deviceId: String): Flow<AutomationConfig>

    /**
     *  Updates the [AutomationConfig] for a given [deviceId] in the database.
     */
    suspend fun updateAutomationConfig(
        deviceId: String,
        block: AutomationConfig.() -> AutomationConfig
    )

    /**
     *  Get backups from the database
     */
    suspend fun getBackups(): List<AutomationConfig.Backup>

    /**
     *  Restore backups to the database
     */
    suspend fun restoreBackup(backups: List<AutomationConfig.Backup>)

    /**
     *  Gets the label of the Intent attached to a given [config]'s [action].
     */
    fun resolveIntentTitle(config: AutomationConfig, action: TagButtonAction): CharSequence?

}

class AutomationRepositoryImpl(
    private val context: Context,
    private val rulesRepository: RulesRepository,
    settingsRepository: SettingsRepository,
    database: UTagDatabase
): AutomationRepository {

    private val hasGrantedRestrictedSettings = settingsRepository.hasGrantedRestrictedSettings
    private val automationConfigs = database.automationConfigTable()
    private val updateLock = Mutex()

    override fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    override suspend fun maybeRequiresRestrictedSettingsPrompt(): Boolean {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return false
        if(hasOverlayPermission()) {
            //Permission is already granted, set the flag and return
            hasGrantedRestrictedSettings.set(true)
            return false
        }
        //If the user has previously granted it, return true (you can't easily revoke it)
        if(hasGrantedRestrictedSettings.get()) return false
        //If the user installed the app using a session-based installer or ADB, not required
        if(context.wasInstalledWithSession()) return false
        return true
    }

    override suspend fun syncRemoteRuleStates(): Boolean {
        val inUseActions = rulesRepository.getInUseActions() ?: return false
        inUseActions.forEach { (id, actions) ->
            updateAutomationConfig(id) {
                copy(
                    pressRemoteEnabled = actions.contains(TagButtonAction.PRESS),
                    holdRemoteEnabled = actions.contains(TagButtonAction.HOLD)
                )
            }
        }
        return true
    }

    override suspend fun getAutomationConfig(deviceId: String): AutomationConfig? {
        return withContext(Dispatchers.IO) {
            automationConfigs.getConfig(deviceId.hashCode())
        }
    }

    override fun getAutomationConfigAsFlow(deviceId: String): Flow<AutomationConfig> {
        return automationConfigs.getConfigAsFlow(deviceId.hashCode()).map {
            it ?: AutomationConfig(deviceId.hashCode())
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateAutomationConfig(
        deviceId: String,
        block: AutomationConfig.() -> AutomationConfig
    ) = updateLock.withLock {
        withContext(Dispatchers.IO) {
            val current = getAutomationConfig(deviceId) ?: AutomationConfig(deviceId.hashCode())
            automationConfigs.insert(block(current))
        }
    }

    override suspend fun getBackups(): List<AutomationConfig.Backup> {
        return withContext(Dispatchers.IO) {
            automationConfigs.getAllConfigs().map { it.toBackup() }
        }
    }

    override suspend fun restoreBackup(backups: List<AutomationConfig.Backup>) {
        withContext(Dispatchers.IO) {
            backups.forEach {
                automationConfigs.insert(it.toConfig())
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun resolveIntentTitle(config: AutomationConfig, action: TagButtonAction): CharSequence? {
        val intentUri = when(action) {
            TagButtonAction.PRESS -> config.pressIntent
            TagButtonAction.HOLD -> config.holdIntent
        }
        val intent = Intent.parseUri(intentUri ?: return null, 0)
        //Use the shortcut label if available
        intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME)?.let {
            return it
        }
        //Use the Tasker Task name if it's a Tasker task
        if(intent.action == TaskerIntent.ACTION_TASK) {
            return intent.getStringExtra(TaskerIntent.EXTRA_TASK_NAME)
        }
        //Use the generic Camera name if it's the Camera intent
        if(intent.action == ACTION_CAMERA) {
            return context.getString(R.string.tag_more_automation_type_camera_title_short)
        }
        return context.packageManager.resolveActivity(intent, 0)
            ?.loadLabel(context.packageManager)
    }

}