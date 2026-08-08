package com.example.aida64remote.widget

import android.content.Context
import com.example.aida64remote.data.SettingsRepository
import com.example.aida64remote.data.SnapshotFetcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

object WidgetRefreshCoordinator {
    private val fetcher = SnapshotFetcher()
    private val refreshMutex = Mutex()

    suspend fun refresh(context: Context, scheduleNext: Boolean = true) {
        if (!refreshMutex.tryLock()) return
        try {
            refreshLocked(context, scheduleNext)
        } finally {
            refreshMutex.unlock()
        }
    }

    private suspend fun refreshLocked(context: Context, scheduleNext: Boolean) {
        val appContext = context.applicationContext
        val store = WidgetStateStore(appContext)
        var connected = false
        // Keep prior Ok/Error status during fetch — only push UI after success or failure.

        try {
            val config = SettingsRepository(appContext).settingsFlow.first().connection
            if (config.host.isBlank()) {
                store.saveError("未配置主机")
            } else {
                val snapshot = fetcher.fetch(config)
                store.saveSuccess(snapshot)
                connected = true
            }
        } catch (e: Exception) {
            store.saveError(e.message ?: e.javaClass.simpleName)
        }

        pushWidgetUi(appContext, store.current())

        if (scheduleNext) {
            val delayMs = if (connected) {
                WidgetRefreshScheduler.INTERVAL_OK_MS
            } else {
                WidgetRefreshScheduler.INTERVAL_FAIL_MS
            }
            WidgetRefreshScheduler.scheduleNext(appContext, delayMs)
        }
    }
}
