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

internal val ColorBg = Color(0xF0121216)
internal val ColorPanel = Color(0xFF1C1C22)
internal val ColorText = Color(0xFFF5F5F7)
internal val ColorMuted = Color(0xFF8B8B96)
internal val ColorYellow = Color(0xFFFFE14D)
internal val ColorCyan = Color(0xFF5EC8FF)
internal val ColorGreen = Color(0xFF3DDC84)
internal val ColorRed = Color(0xFFFF5C5C)
internal val ColorTrack = Color(0xFF2E2E36)
internal val ColorRam = Color(0xFFB39DFF)
internal val ColorNet = Color(0xFF7EE0C3)

enum class WidgetFormFactor {
    /** 约 2×1：仅 CPU/GPU 占用 */
    Compact,
    /** 约 3×2：CPU/GPU 占用 + 温度 */
    Standard,
    /** 约 4×3：CPU/GPU/内存/网络 */
    Wide,
}

abstract class MonitorGlanceWidgetBase(
    private val form: WidgetFormFactor,
) : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val seed = WidgetStateStore(context).current()
        provideContent {
            val prefs = currentState<Preferences>()
            val state = WidgetGlancePrefs.run { prefs.toUiState() }
                .takeUnless { it.status == WidgetConnStatus.Idle && it.updatedAtEpochMs == 0L }
                ?: seed
            GlanceTheme {
                when (form) {
                    WidgetFormFactor.Compact -> CompactWidgetContent(state, context)
                    WidgetFormFactor.Standard -> StandardWidgetContent(state, context)
                    WidgetFormFactor.Wide -> WideWidgetContent(state, context)
                }
            }
        }
    }
}

/** 中号（默认，兼容已添加的挂件） */
class MonitorGlanceWidget : MonitorGlanceWidgetBase(WidgetFormFactor.Standard)

class MonitorGlanceWidgetCompact : MonitorGlanceWidgetBase(WidgetFormFactor.Compact)

class MonitorGlanceWidgetWide : MonitorGlanceWidgetBase(WidgetFormFactor.Wide)

@Composable
internal fun statusColor(status: WidgetConnStatus): Color = when (status) {
    WidgetConnStatus.Ok -> ColorGreen
    WidgetConnStatus.Error -> ColorRed
    WidgetConnStatus.Updating,
    WidgetConnStatus.Idle -> ColorYellow
}

@Composable
internal fun statusLabel(status: WidgetConnStatus, context: Context): String = when (status) {
    WidgetConnStatus.Ok -> context.getString(R.string.widget_status_ok)
    WidgetConnStatus.Error -> context.getString(R.string.widget_status_error)
    WidgetConnStatus.Updating -> context.getString(R.string.widget_status_updating)
    WidgetConnStatus.Idle -> context.getString(R.string.widget_status_idle)
}

@Composable
private fun CompactWidgetContent(state: WidgetUiState, context: Context) {
    val accent = statusColor(state.status)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(ColorProvider(ColorBg, ColorBg))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = context.getString(R.string.widget_title),
                    style = TextStyle(
                        color = ColorProvider(ColorCyan, ColorCyan),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "● ${statusLabel(state.status, context)}",
                    style = TextStyle(
                        color = ColorProvider(accent, accent),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = context.getString(
                    R.string.widget_compact_line,
                    formatUsage(state.cpuUsage),
                    formatUsage(state.gpuUsage),
                ),
                style = TextStyle(
                    color = ColorProvider(ColorText, ColorText),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
        }
        Text(
            text = context.getString(R.string.widget_refresh),
            style = TextStyle(
                color = ColorProvider(ColorText, ColorText),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(ColorProvider(ColorPanel, ColorPanel))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable(actionRunCallback<WidgetRefreshAction>()),
        )
    }
}

@Composable
private fun StandardWidgetContent(state: WidgetUiState, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(18.dp)
            .background(ColorProvider(ColorBg, ColorBg))
            .padding(12.dp),
    ) {
        HeaderRow(state, context, titleSize = 15.sp, statusSize = 11.sp)
        MetaLine(state, context)
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
                primary = formatUsage(state.cpuUsage),
                secondary = formatTemp(state.cpuTemp),
                progress = state.cpuUsageBar,
                primarySize = 26.sp,
                showBar = true,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            MetricCard(
                title = "GPU",
                accent = ColorCyan,
                primary = formatUsage(state.gpuUsage),
                secondary = formatTemp(state.gpuTemp),
                progress = state.gpuUsageBar,
                primarySize = 26.sp,
                showBar = true,
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            )
        }
        ErrorLine(state)
    }
}

@Composable
private fun WideWidgetContent(state: WidgetUiState, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(20.dp)
            .background(ColorProvider(ColorBg, ColorBg))
            .padding(12.dp),
    ) {
        HeaderRow(state, context, titleSize = 16.sp, statusSize = 11.sp)
        MetaLine(state, context, fontSize = 11.sp)
        Spacer(modifier = GlanceModifier.height(8.dp))
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.Top,
            ) {
                MetricCard(
                    title = "CPU",
                    accent = ColorYellow,
                    primary = formatUsage(state.cpuUsage),
                    secondary = formatTemp(state.cpuTemp),
                    tertiary = formatClockValue(state.cpuClock),
                    progress = state.cpuUsageBar,
                    primarySize = 22.sp,
                    showBar = true,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                MetricCard(
                    title = "GPU",
                    accent = ColorCyan,
                    primary = formatUsage(state.gpuUsage),
                    secondary = formatTemp(state.gpuTemp),
                    tertiary = formatMb(state.vramUsed, context.getString(R.string.widget_vram_label)),
                    progress = state.gpuUsageBar,
                    primarySize = 22.sp,
                    showBar = true,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.Top,
            ) {
                MetricCard(
                    title = context.getString(R.string.widget_ram_title),
                    accent = ColorRam,
                    primary = formatUsage(state.ramUsage),
                    secondary = formatMb(state.ramUsed, context.getString(R.string.widget_used_label)),
                    progress = state.ramUsageBar,
                    primarySize = 22.sp,
                    showBar = true,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                MetricCard(
                    title = context.getString(R.string.widget_net_title),
                    accent = ColorNet,
                    primary = formatNet(state.download, "↓"),
                    secondary = formatNet(state.upload, "↑"),
                    progress = 0f,
                    primarySize = 16.sp,
                    showBar = false,
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                )
            }
        }
        ErrorLine(state)
    }
}

@Composable
private fun HeaderRow(
    state: WidgetUiState,
    context: Context,
    titleSize: androidx.compose.ui.unit.TextUnit,
    statusSize: androidx.compose.ui.unit.TextUnit,
) {
    val accent = statusColor(state.status)
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_title),
            style = TextStyle(
                color = ColorProvider(ColorCyan, ColorCyan),
                fontSize = titleSize,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier.clickable(actionStartActivity<MainActivity>()),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "● ${statusLabel(state.status, context)}",
            style = TextStyle(
                color = ColorProvider(accent, accent),
                fontSize = statusSize,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = context.getString(R.string.widget_refresh),
            style = TextStyle(
                color = ColorProvider(ColorText, ColorText),
                fontSize = statusSize,
                fontWeight = FontWeight.Bold,
            ),
            modifier = GlanceModifier
                .cornerRadius(8.dp)
                .background(ColorProvider(ColorPanel, ColorPanel))
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .clickable(actionRunCallback<WidgetRefreshAction>()),
        )
    }
}

@Composable
private fun MetaLine(state: WidgetUiState, context: Context, fontSize: androidx.compose.ui.unit.TextUnit = 11.sp) {
    if (state.hostLabel.isBlank() && state.updatedAtEpochMs <= 0L) return
    Spacer(modifier = GlanceModifier.height(4.dp))
    val clock = if (state.updatedAtEpochMs > 0L) formatClock(state.updatedAtEpochMs) else "—"
    val meta = if (state.hostLabel.isNotBlank()) {
        context.getString(R.string.widget_meta, state.hostLabel, clock)
    } else {
        context.getString(R.string.widget_meta_time_only, clock)
    }
    Text(
        text = meta,
        style = TextStyle(
            color = ColorProvider(ColorMuted, ColorMuted),
            fontSize = fontSize,
        ),
        maxLines = 1,
    )
}

@Composable
private fun ErrorLine(state: WidgetUiState) {
    if (state.status != WidgetConnStatus.Error || state.message.isBlank()) return
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

@Composable
private fun MetricCard(
    title: String,
    accent: Color,
    primary: String,
    secondary: String,
    progress: Float,
    primarySize: androidx.compose.ui.unit.TextUnit,
    showBar: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    tertiary: String? = null,
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
            text = primary,
            style = TextStyle(
                color = ColorProvider(ColorText, ColorText),
                fontSize = primarySize,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            text = secondary,
            style = TextStyle(
                color = ColorProvider(ColorMuted, ColorMuted),
                fontSize = 12.sp,
            ),
            maxLines = 1,
        )
        if (!tertiary.isNullOrBlank()) {
            Text(
                text = tertiary,
                style = TextStyle(
                    color = ColorProvider(ColorMuted, ColorMuted),
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
        }
        if (showBar) {
            Spacer(modifier = GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth().height(5.dp),
                color = ColorProvider(accent, accent),
                backgroundColor = ColorProvider(ColorTrack, ColorTrack),
            )
        }
    }
}

internal fun formatUsage(raw: String): String {
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

internal fun formatTemp(raw: String): String {
    if (raw.isBlank() || raw == "—") return "— ℃"
    return if (raw.contains('°') || raw.contains('℃')) raw else "$raw℃"
}

internal fun formatClockValue(raw: String): String {
    if (raw.isBlank() || raw == "—") return "— MHz"
    return if (raw.contains("MHz", ignoreCase = true) || raw.contains("GHz", ignoreCase = true)) {
        raw
    } else {
        "$raw MHz"
    }
}

internal fun formatMb(raw: String, label: String): String {
    if (raw.isBlank() || raw == "—") return "$label —"
    val value = if (raw.contains("MB", ignoreCase = true) || raw.contains("GB", ignoreCase = true)) {
        raw
    } else {
        "$raw MB"
    }
    return "$label $value"
}

internal fun formatNet(raw: String, arrow: String): String {
    if (raw.isBlank() || raw == "—") return "$arrow —"
    return if (raw.contains('/')) "$arrow $raw" else "$arrow $raw KB/s"
}

internal fun formatClock(epochMs: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = epochMs }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    return "%02d:%02d".format(h, m)
}

private fun enqueueRefresh(context: Context) {
    WidgetRefreshWorker.enqueue(context)
}

private fun cancelRefreshIfNoWidgets(context: Context) {
    val am = android.appwidget.AppWidgetManager.getInstance(context)
    val stillPlaced = listOf(
        MonitorWidgetCompactReceiver::class.java,
        MonitorWidgetReceiver::class.java,
        MonitorWidgetWideReceiver::class.java,
    ).any { clazz ->
        am.getAppWidgetIds(android.content.ComponentName(context, clazz)).isNotEmpty()
    }
    if (!stillPlaced) {
        WidgetRefreshScheduler.cancel(context)
    }
}

class MonitorWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitorGlanceWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefreshIfNoWidgets(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_REFRESH,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            android.content.Intent.ACTION_BOOT_COMPLETED,
            -> enqueueRefresh(context)
        }
    }
}

class MonitorWidgetCompactReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitorGlanceWidgetCompact()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefreshIfNoWidgets(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_REFRESH,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            -> enqueueRefresh(context)
        }
    }
}

class MonitorWidgetWideReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MonitorGlanceWidgetWide()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        enqueueRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefreshIfNoWidgets(context)
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetRefreshScheduler.ACTION_REFRESH,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            android.appwidget.AppWidgetManager.ACTION_APPWIDGET_ENABLED,
            -> enqueueRefresh(context)
        }
    }
}
