package com.kieronquinn.app.utag.utils.extensions

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Size

fun AppWidgetManager.getWidgetSizes(context: Context, appWidgetId: Int): Pair<Size, Size>? {
    val options = getAppWidgetOptions(appWidgetId) ?: return null
    val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, -1)
        .takeIf { it >= 0 } ?: return null
    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, -1)
        .takeIf { it >= 0 } ?: return null
    val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, -1)
        .takeIf { it >= 0 } ?: return null
    val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, -1)
        .takeIf { it >= 0 } ?: return null
    val portrait = Size(context.resources.dip(minWidth), context.resources.dip(maxHeight))
    val landscape = Size(context.resources.dip(maxWidth), context.resources.dip(minHeight))
    return Pair(portrait, landscape)
}