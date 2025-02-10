package com.kieronquinn.app.utag.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerWidgetProvider
import com.kieronquinn.app.smartspacer.sdk.utils.getClickPendingIntent
import com.kieronquinn.app.utag.BuildConfig
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.repositories.SmartspacerRepository
import com.kieronquinn.app.utag.repositories.SmartspacerRepository.TargetData
import org.koin.android.ext.android.inject

class LocationSmartspacerWidgetProvider: SmartspacerWidgetProvider() {

    companion object {
        internal const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.widget.location"

        internal fun getProviderInfo(context: Context): AppWidgetProviderInfo {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, UTagWidgetProvider::class.java)
            return appWidgetManager.installedProviders.first {
                it.provider == component
            }
        }
    }

    private val smartspacerRepository by inject<SmartspacerRepository>()

    override fun onWidgetChanged(smartspacerId: String, remoteViews: RemoteViews?) {
        val targetData = remoteViews?.load()?.let {
            val mapImage = it.findViewById<ImageView>(R.id.widget_utag_map_image)
            if(mapImage != null) {
                val map = mapImage.drawable as? BitmapDrawable ?: return@let null
                val address = mapImage.contentDescription ?: return@let null
                val label = it.findViewById<ImageView>(R.id.widget_utag_map_logo)?.contentDescription
                    ?: return@let null
                val onClick = it.findViewById<View>(android.R.id.background)?.getClickPendingIntent()
                    ?: return@let null
                TargetData(
                    title = label.toString(),
                    subtitle = address.toString(),
                    image = map.bitmap,
                    onClick = onClick,
                    remoteViews = remoteViews
                )
            }else{
                val label = it.findViewById<TextView>(R.id.widget_utag_map_error)?.text
                    ?: return@let null
                val onClick = it.findViewById<View>(android.R.id.background)?.getClickPendingIntent()
                    ?: return@let null
                TargetData(
                    title = label.toString(),
                    subtitle = resources.getString(R.string.app_name),
                    image = null,
                    onClick = onClick,
                    remoteViews = remoteViews
                )
            }
        }
        smartspacerRepository.setTargetData(smartspacerId, targetData, false)
    }

    override fun getConfig(smartspacerId: String): Config {
        return Config(
            width = resources.getDimensionPixelSize(R.dimen.widget_smartspacer_width),
            height = resources.getDimensionPixelSize(R.dimen.widget_smartspacer_height)
        )
    }

    override fun getAppWidgetProviderInfo(smartspacerId: String): AppWidgetProviderInfo? {
        return getProviderInfo(provideContext())
    }

}