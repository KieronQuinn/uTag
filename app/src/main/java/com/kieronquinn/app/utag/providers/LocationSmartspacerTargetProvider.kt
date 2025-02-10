package com.kieronquinn.app.utag.providers

import android.content.ComponentName
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceTarget
import com.kieronquinn.app.smartspacer.sdk.model.expanded.ExpandedState
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.TargetTemplate
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SmartspacerRepository
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon
import dev.oneuiproject.oneui.R as OneuiR

class LocationSmartspacerTargetProvider: SmartspacerTargetProvider() {

    private val smartspacerRepository by inject<SmartspacerRepository>()

    private val icon by lazy {
        AndroidIcon.createWithResource(provideContext(), R.drawable.ic_notification)
    }

    private val configIcon by lazy {
        AndroidIcon.createWithResource(provideContext(), OneuiR.drawable.ic_oui_location_outline)
    }

    override fun getSmartspaceTargets(smartspacerId: String): List<SmartspaceTarget> {
        val target = smartspacerRepository.getTargetData(smartspacerId)?.let {
            if(it.image != null) {
                TargetTemplate.Image(
                    context = provideContext(),
                    id = smartspacerId,
                    componentName = ComponentName(
                        provideContext(), LocationSmartspacerTargetProvider::class.java
                    ),
                    icon = Icon(icon),
                    title = Text(it.title),
                    subtitle = Text(it.subtitle),
                    image = Icon(AndroidIcon.createWithBitmap(it.image)),
                    onClick = TapAction(pendingIntent = it.onClick)
                ).create()
            }else{
                TargetTemplate.Basic(
                    id = smartspacerId,
                    componentName = ComponentName(
                        provideContext(), LocationSmartspacerTargetProvider::class.java
                    ),
                    icon = Icon(icon),
                    title = Text(it.title),
                    subtitle = Text(it.subtitle),
                    onClick = TapAction(pendingIntent = it.onClick)
                ).create()
            }.apply {
                expandedState = ExpandedState(
                    remoteViews = ExpandedState.RemoteViews(it.remoteViews)
                )
            }
        }
        return listOfNotNull(target)
    }

    override fun getConfig(smartspacerId: String?): Config {
        val label = smartspacerId?.let { smartspacerRepository.getTargetData(it) }?.title
        return Config(
            resources.getString(R.string.smartspacer_plugin_location_title),
            if(label != null) {
                resources.getString(R.string.smartspacer_plugin_location_description_set, label)
            }else{
                resources.getString(R.string.smartspacer_plugin_location_description)
            },
            if(smartspacerId == null) configIcon else icon,
            allowAddingMoreThanOnce = true,
            widgetProvider = LocationSmartspacerWidgetProvider.AUTHORITY
        )
    }

    override fun onDismiss(smartspacerId: String, targetId: String): Boolean {
        return false
    }

}