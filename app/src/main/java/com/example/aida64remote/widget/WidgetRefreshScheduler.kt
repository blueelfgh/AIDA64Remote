package com.example.aida64remote.widget

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetRefreshScheduler {
    const val ACTION_REFRESH = "com.example.aida64remote.action.WIDGET_REFRESH"
    const val INTERVAL_OK_MS = 5_000L
    const val INTERVAL_FAIL_MS = 300_000L
    /** @deprecated use INTERVAL_OK_MS */
    const val INTERVAL_MS = INTERVAL_OK_MS

    private const val UNIQUE_WORK = "widget_refresh_chain"

    fun scheduleNext(context: Context, delayMs: Long = INTERVAL_OK_MS) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMs.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun enqueueNow(context: Context) {
        scheduleNext(context, delayMs = 0L)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK)
    }
}
