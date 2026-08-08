package com.example.aida64remote.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.aida64remote.MainActivity
import com.example.aida64remote.R

private val ColorBg = Color(0xF0121216)
private val ColorPanel = Color(0xFF222228)
private val ColorText = Color(0xFFF5F5F7)
private val ColorMuted = Color(0xFF9A9AA3)
private val ColorYellow = Color(0xFFFFE14D)
private val ColorCyan = Color(0xFF7EC8FF)
private val ColorGreen = Color(0xFF4CD964)
private val ColorRed = Color(0xFFFF5C5C)
private val ColorTrack = Color(0xFF3A3A42)

class MonitorGlanceWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Seed Glance prefs from DataStore once when a session starts.
        val seed = WidgetStateStore(context).current()
        provideContent {
            val prefs = currentState<Preferences>()
            val state = WidgetGlancePrefs.run { prefs.toUiState() }
                .takeUnless { it.status == WidgetConnStatus.Idle && it.updatedAtEpochMs == 0L }
                ?: seed
            GlanceTheme {
                WidgetContent(state = state, context = context)
            }
        }
    }
}

@Composable
private fun WidgetContent(state: WidgetUiState, context: Context) {
    val statusColor = when (state.status) {
        WidgetConnStatus.Ok -> ColorGreen
        WidgetConnStatus.Error -> ColorRed
        WidgetConnStatus.Updating,
        WidgetConnStatus.Idle -> ColorYellow
    }
    val statusLabel = when (state.status) {
        WidgetConnStatus.Ok -> context.getString(R.string.widget_status_ok)
        WidgetConnStatus.Error -> context.getString(R.string.widget_status_error)
        WidgetConnStatus.Updating -> context.getString(R.string.widget_status_updating)
        WidgetConnStatus.Idle -> context.getString(R.string.widget_status_idle)
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(ColorProvider(ColorBg, ColorBg))
            .padding(14.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_title),
                style = TextStyle(
                    color = ColorProvider(ColorYellow, ColorYellow),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "● $statusLabel",
                style = TextStyle(
                    color = ColorProvider(statusColor, statusColor),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            Text(
                text = context.getString(R.string.widget_refresh),
                style = TextStyle(
                    color = ColorProvider(ColorText, ColorText),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = GlanceModifier
                    .cornerRadius(8.dp)
                    .background(ColorProvider(ColorPanel, ColorPanel))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clickable(actionRunCallback<WidgetRefreshAction>()),
            )
        }

        if (state.updatedAtEpochMs > 0L) {
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_updated_at, formatClock(state.updatedAtEpochMs)),
                style = TextStyle(
                    color = ColorProvider(ColorMuted, ColorMuted),
                    fontSize = 11.sp,
                ),
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.Top,
        ) {
            MetricCard(
                title = "CPU",
                accent = ColorYellow,
                usage = formatUsage(state.cpuUsage),
                temp = formatTemp(state.cpuTemp),
                progress = state.cpuUsageBar,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
            Spacer(modifier = GlanceModifier.width(10.dp))
            MetricCard(
                title = "GPU",
                accent = ColorCyan,
                usage = formatUsage(state.gpuUsage),
                temp = formatTemp(state.gpuTemp),
                progress = state.gpuUsageBar,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
        }

        if (state.status == WidgetConnStatus.Error && state.message.isNotBlank()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = state.message,
                style = TextStyle(
                    color = ColorProvider(ColorMuted, ColorMuted),
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    accent: Color,
    usage: String,
    temp: String,
    progress: Float,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier
            .cornerRadius(14.dp)
            .background(ColorProvider(ColorPanel, ColorPanel))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(
                color = ColorProvider(accent, accent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = usage,
            style = TextStyle(
                color = ColorProvider(ColorText, ColorText),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            text = temp,
            style = TextStyle(
                color = ColorProvider(ColorMuted, ColorMuted),
                fontSize = 13.sp,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = GlanceModifier.fillMaxWidth().height(6.dp),
            color = ColorProvider(accent, accent),
            backgroundColor = ColorProvider(ColorTrack, ColorTrack),
        )
    }
}

private fun formatUsage(raw: String): String {
    if (raw.isBlank() || raw == "—") return "—"
    val cleaned = raw.replace("%", "").trim()
    val num = cleaned.toFloatOrNull()
    return if (num != null) {
        val shown = if (num == num.toInt().toFloat()) num.toInt().toString() else cleaned
        "$shown%"
    } else {
        if (raw.contains('%')) raw else "$raw%"
    }
}

private fun formatTemp(raw: String): String {
    if (raw.isBlank() || raw == "—") return "— ℃"
    return if (raw.contains('°') || raw.contains('℃')) raw else "$raw℃"
}

private fun formatClock(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    val s = cal.get(java.util.Calendar.SECOND)
    return "%02d:%02d:%02d".format(h, m, s)
}

class MonitorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitorGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetRefreshWorker.enqueue(context)
    }

    override fun onDisabled(context: Context) {
        WidgetRefreshScheduler.cancel(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_REFRESH,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            android.content.Intent.ACTION_BOOT_COMPLETED,
            -> WidgetRefreshWorker.enqueue(context)
        }
    }
}
