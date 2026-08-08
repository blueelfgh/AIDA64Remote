package com.example.aida64remote.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Runs a single refresh off the main thread, then reschedules AlarmManager.
 */
class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            WidgetRefreshCoordinator.refresh(applicationContext, scheduleNext = true)
            Result.success()
        } catch (_: Exception) {
            WidgetRefreshScheduler.scheduleNext(applicationContext)
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "widget_refresh_once"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
