package com.kieronquinn.app.utag.ui.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.core.view.WindowCompat
import com.kieronquinn.app.utag.R
import com.kieronquinn.app.utag.ui.screens.widget.container.WidgetContainerViewModel.WidgetType

abstract class BaseWidgetConfigurationActivity: BaseActivity() {

    companion object {
        fun getWidgetType(activity: Activity): WidgetType {
            return WidgetType.entries.firstOrNull { it.clazz == activity::class.java }
                ?: throw RuntimeException("Unknown widget configuration activity ${activity::class.java}")
        }

        fun getAppWidgetId(activity: Activity): Int {
            return activity.intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                .takeIf { it >= 0 } ?: throw RuntimeException("Invalid app widget ID")
        }

        fun getCallingPackage(activity: Activity): String {
            return activity.callingPackage ?: throw RuntimeException("Invalid calling package")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_widget_configuration)
    }

}