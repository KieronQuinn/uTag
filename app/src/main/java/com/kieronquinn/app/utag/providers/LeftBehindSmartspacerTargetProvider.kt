package com.kieronquinn.app.utag.providers

import android.content.ComponentName
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.LeftBehindRepository
import com.kieronquinn.app.utag.xposed.extensions.TagActivity_createIntent
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon
import dev.oneuiproject.oneui.R as OneuiR

class LeftBehindSmartspacerTargetProvider: SmartspacerTargetProvider() {

    private val leftBehindRepository by inject<LeftBehindRepository>()

    private val icon by lazy {
        AndroidIcon.createWithResource(provideContext(), R.drawable.ic_notification)
    }

    private val configIcon by lazy {
        AndroidIcon.createWithResource(provideContext(), OneuiR.drawable.ic_oui_error)
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        return leftBehindRepository.leftBehindTags.values.map {
            val id = "${smartspacerId}_${it.deviceId}"
            if(it.map != null) {
                TargetTemplate.Image(
                    context = provideContext(),
                    id = id,
                    componentName = ComponentName(
                        provideContext(),
                        LeftBehindSmartspacerTargetProvider::class.java
                    ),
                    title = Text(it.title),
                    subtitle = Text(it.subtitle),
                    icon = Icon(icon),
                    image = Icon(AndroidIcon.createWithBitmap(it.map)),
                    onClick = TapAction(intent = TagActivity_createIntent(it.deviceId))
                )
            }else{
                TargetTemplate.Basic(
                    id = id,
                    componentName = ComponentName(
                        provideContext(),
                        LeftBehindSmartspacerTargetProvider::class.java
                    ),
                    title = Text(it.title),
                    subtitle = Text(it.subtitle),
                    icon = Icon(icon),
                    onClick = TapAction(intent = TagActivity_createIntent(it.deviceId))
                )
            }.create()
        }
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.smartspacer_plugin_left_behind_title),
            description = resources.getString(R.string.smartspacer_plugin_left_behind_description),
            icon = if(smartspacerId == null) configIcon else icon,
            allowAddingMoreThanOnce = true
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        val deviceId = targetId.removePrefix("${smartspacerId}_")
        leftBehindRepository.dismissLeftBehindTag(deviceId)
        return true
    }

}