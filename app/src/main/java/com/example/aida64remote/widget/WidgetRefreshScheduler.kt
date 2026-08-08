package com.example.aida64remote.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object WidgetRefreshScheduler {
    const val ACTION_REFRESH = "com.example.aida64remote.action.WIDGET_REFRESH"
    const val INTERVAL_MS = 30_000L

    fun scheduleNext(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val triggerAt = SystemClock.elapsedRealtime() + INTERVAL_MS
        am.setAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAt,
            pendingIntent(context),
        )
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(pendingIntent(context))
    }

    fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MonitorWidgetReceiver::class.java).apply {
            action = ACTION_REFRESH
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
