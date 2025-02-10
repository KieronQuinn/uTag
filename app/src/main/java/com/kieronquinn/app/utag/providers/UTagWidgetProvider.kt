package com.kieronquinn.app.utag.providers

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle
import com.kieronquinn.app.utag.repositories.WidgetRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class UTagWidgetProvider: AppWidgetProvider(), KoinComponent {

    private val widgetRepository by inject<WidgetRepository>()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        widgetRepository.updateWidgets()
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        widgetRepository.onWidgetSizeChanged(appWidgetId)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        widgetRepository.onWidgetsDeleted(appWidgetIds.toList())
    }

}