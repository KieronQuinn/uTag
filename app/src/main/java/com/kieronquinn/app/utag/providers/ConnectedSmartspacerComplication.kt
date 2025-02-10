package com.kieronquinn.app.utag.providers

import android.content.Intent
import com.kieronquinn.app.smartspacer.sdk.model.SmartspaceAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Icon
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.TapAction
import com.kieronquinn.app.smartspacer.sdk.model.uitemplatedata.Text
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerComplicationProvider
import com.kieronquinn.app.smartspacer.sdk.utils.ComplicationTemplate
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SmartTagRepository
import com.kieronquinn.app.utag.ui.activities.TagActivity
import org.koin.android.ext.android.inject
import android.graphics.drawable.Icon as AndroidIcon

class ConnectedSmartspacerComplication: SmartspacerComplicationProvider() {

    private val smartTagRepository by inject<SmartTagRepository>()

    private val icon by lazy {
        AndroidIcon.createWithResource(provideContext(), R.drawable.ic_notification)
    }

    override fun getSmartspaceActions(smartspacerId: String): List<SmartspaceAction> {
        return listOf(
            ComplicationTemplate.Basic(
                id = smartspacerId,
                content = Text(smartTagRepository.getConnectedTagCount().toString()),
                icon = Icon(icon),
                onClick = TapAction(intent = Intent(provideContext(), TagActivity::class.java))
            ).create()
        )
    }

    override fun getConfig(smartspacerId: String?): Config {
        return Config(
            label = resources.getString(R.string.smartspacer_plugin_connected_title),
            description = resources.getString(R.string.smartspacer_plugin_connected_description),
            icon = icon,
            allowAddingMoreThanOnce = true
        )
    }

}