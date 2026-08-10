package com.example.aida64remote.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState

/**
 * Push [state] into Glance Preferences and request recomposition for every form factor.
 */
suspend fun pushWidgetUi(context: Context, state: WidgetUiState) {
    val appContext = context.applicationContext
    val glanceManager = GlanceAppWidgetManager(appContext)
    val targets: List<Pair<GlanceAppWidget, Class<out GlanceAppWidget>>> = listOf(
        MonitorGlanceWidgetCompact() to MonitorGlanceWidgetCompact::class.java,
        MonitorGlanceWidget() to MonitorGlanceWidget::class.java,
        MonitorGlanceWidgetWide() to MonitorGlanceWidgetWide::class.java,
    )

    for ((widget, clazz) in targets) {
        val glanceIds = glanceManager.getGlanceIds(clazz)
        for (id in glanceIds) {
            updateAppWidgetState(appContext, id) { prefs ->
                WidgetGlancePrefs.run { prefs.write(state) }
            }
            widget.update(appContext, id)
        }
    }
}
