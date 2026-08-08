package com.example.aida64remote.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Runs a single refresh off the main thread, then schedules the next WorkManager delay.
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
            WidgetRefreshScheduler.scheduleNext(
                applicationContext,
                WidgetRefreshScheduler.INTERVAL_FAIL_MS,
            )
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            WidgetRefreshScheduler.enqueueNow(context)
        }
    }
}
