package com.example.aida64remote.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.aida64remote.data.SettingsRepository
import com.example.aida64remote.data.SnapshotFetcher
import kotlinx.coroutines.flow.first

object WidgetRefreshCoordinator {
    private val fetcher = SnapshotFetcher()

    suspend fun refresh(context: Context, scheduleNext: Boolean = true) {
        val appContext = context.applicationContext
        val store = WidgetStateStore(appContext)
        store.saveUpdating()
        MonitorGlanceWidget().updateAll(appContext)

        try {
            val config = SettingsRepository(appContext).settingsFlow.first().connection
            if (config.host.isBlank()) {
                store.saveError("未配置主机")
            } else {
                val snapshot = fetcher.fetch(config)
                store.saveSuccess(snapshot)
            }
        } catch (e: Exception) {
            store.saveError(e.message ?: e.javaClass.simpleName)
        }

        MonitorGlanceWidget().updateAll(appContext)
        if (scheduleNext) {
            WidgetRefreshScheduler.scheduleNext(appContext)
        }
    }
}
