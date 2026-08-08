package com.example.aida64remote.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll

/**
 * Push [state] into Glance Preferences and request recomposition.
 * Calling update() alone does not re-enter provideGlance while a session is alive;
 * UI must observe Glance currentState() fed by updateAppWidgetState.
 */
suspend fun pushWidgetUi(context: Context, state: WidgetUiState) {
    val appContext = context.applicationContext
    val glanceManager = GlanceAppWidgetManager(appContext)
    val glanceIds = glanceManager.getGlanceIds(MonitorGlanceWidget::class.java)
    val classicIds = AppWidgetManager.getInstance(appContext)
        .getAppWidgetIds(ComponentName(appContext, MonitorWidgetReceiver::class.java))

    val widget = MonitorGlanceWidget()
    if (glanceIds.isNotEmpty()) {
        for (id in glanceIds) {
            updateAppWidgetState(appContext, id) { prefs ->
                WidgetGlancePrefs.run { prefs.write(state) }
            }
            widget.update(appContext, id)
        }
    } else if (classicIds.isNotEmpty()) {
        widget.updateAll(appContext)
    }
}
