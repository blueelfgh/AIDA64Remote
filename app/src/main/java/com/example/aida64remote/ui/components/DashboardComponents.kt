package com.example.aida64remote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Toys
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aida64remote.R
import com.example.aida64remote.model.DashboardSnapshot
import com.example.aida64remote.ui.theme.DashColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HexBackground(modifier: Modifier = Modifier) {
    val bg = DashColors.bg
    val hex = DashColors.hex
    val hexGlow = DashColors.hexGlow
    Canvas(modifier = modifier.fillMaxSize().background(bg)) {
        val hexSize = 28.dp.toPx()
        val h = hexSize * 1.73205f
        val w = hexSize * 2f
        val rows = (size.height / (h * 0.75f)).toInt() + 2
        val cols = (size.width / (w * 0.75f)).toInt() + 2
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = col * w * 0.75f + if (row % 2 == 0) 0f else w * 0.375f
                val y = row * h * 0.75f
                val path = hexPath(Offset(x, y), hexSize * 0.92f)
                val edge = row == 0 || col == 0 || row == rows - 1 || col == cols - 1
                drawPath(
                    path = path,
                    color = if (edge) hexGlow else hex,
                    style = Stroke(width = 1.2f),
                )
            }
        }
    }
}

private fun hexPath(center: Offset, radius: Float): Path {
    val path = Path()
    for (i in 0 until 6) {
        val angle = Math.toRadians((60.0 * i - 30.0))
        val point = Offset(
            center.x + radius * cos(angle).toFloat(),
            center.y + radius * sin(angle).toFloat(),
        )
        if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    return path
}

@Composable
fun DashPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .border(1.dp, DashColors.border, RoundedCornerShape(10.dp))
            .background(DashColors.panel, RoundedCornerShape(10.dp))
            .padding(10.dp),
    ) {
        content()
    }
}

@Composable
fun MetricBarRow(
    label: String,
    value: String,
    unit: String = "",
    progress: Float,
    labelSize: TextUnit = 13.sp,
    valueSize: TextUnit = 14.sp,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = DashColors.text,
                fontSize = labelSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (unit.isEmpty()) value else "$value $unit",
                color = DashColors.text,
                fontSize = valueSize,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        ThinProgressBar(progress = progress)
    }
}

@Composable
fun ThinProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(DashColors.track, RoundedCornerShape(2.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(DashColors.fill, RoundedCornerShape(2.dp)),
        )
    }
}

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    maxY: Float = 100f,
    lineColor: Color = Color.Unspecified,
) {
    val resolvedLine = if (lineColor == Color.Unspecified) DashColors.fill else lineColor
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val maxVal = maxOf(maxY, values.maxOrNull() ?: maxY).coerceAtLeast(1f)
        val stepX = if (values.size <= 1) size.width else size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value / maxVal) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = resolvedLine,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun TempValue(
    temp: String,
    tempSize: TextUnit = 32.sp,
    modifier: Modifier = Modifier,
) {
    Text(
        text = temp,
        color = DashColors.text,
        fontSize = tempSize,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
fun CpuPanel(
    data: DashboardSnapshot,
    modifier: Modifier = Modifier,
    showKeepScreenOn: Boolean = false,
) {
    DashPanel(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (showKeepScreenOn) {
                Text(
                    text = stringResource(R.string.keep_screen_on_badge),
                    color = DashColors.badgeText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .background(DashColors.badgeBg, RoundedCornerShape(999.dp))
                        .border(1.dp, DashColors.badgeText, RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = if (showKeepScreenOn) 22.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TempValue(temp = data.cpuTemp, tempSize = 36.sp, modifier = Modifier.padding(horizontal = 8.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = data.cpuName,
                        color = DashColors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    MetricBarRow(
                        label = stringResource(R.string.cpu_clock),
                        value = data.cpuClock,
                        unit = stringResource(R.string.unit_mhz),
                        progress = data.cpuClockBar,
                        labelSize = 14.sp,
                        valueSize = 16.sp,
                    )
                    MetricBarRow(
                        label = stringResource(R.string.cpu_usage),
                        value = data.cpuUsage,
                        unit = stringResource(R.string.unit_percent),
                        progress = data.cpuUsageBar,
                        labelSize = 14.sp,
                        valueSize = 16.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun GpuPanel(data: DashboardSnapshot, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TempValue(
                temp = data.gpuTemp,
                tempSize = 30.sp,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricBarRow(stringResource(R.string.vram_used), data.vramUsed, stringResource(R.string.unit_mb), data.vramUsedBar, 12.sp, 13.sp)
                MetricBarRow(stringResource(R.string.vram_free), data.vramFree, stringResource(R.string.unit_mb), data.vramFreeBar, 12.sp, 13.sp)
                MetricBarRow(stringResource(R.string.gpu_clock), data.gpuClock, stringResource(R.string.unit_mhz), data.gpuClockBar, 12.sp, 13.sp)
                MetricBarRow(stringResource(R.string.gpu_mem_clock), data.gpuMemClock, stringResource(R.string.unit_mhz), data.gpuMemClockBar, 12.sp, 13.sp)
                MetricBarRow(stringResource(R.string.gpu_usage), data.gpuUsage, stringResource(R.string.unit_percent), data.gpuUsageBar, 12.sp, 13.sp)
                MetricBarRow(
                    label = stringResource(R.string.gpu_temp),
                    value = data.gpuTemp.filter { it.isDigit() || it == '.' },
                    unit = stringResource(R.string.unit_celsius),
                    progress = data.gpuTempBar,
                    labelSize = 12.sp,
                    valueSize = 13.sp,
                )
            }
        }
    }
}

@Composable
fun FpsPanel(data: DashboardSnapshot, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "120",
                color = DashColors.muted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Text(
                text = "0",
                color = DashColors.muted,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomStart),
            )
            Text(
                text = "${data.fps} FPS",
                color = DashColors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd),
            )
            Sparkline(
                values = data.fpsHistory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, top = 18.dp, end = 8.dp, bottom = 8.dp),
                maxY = 120f,
                lineColor = DashColors.sparkline,
            )
        }
    }
}

@Composable
fun RamPanel(data: DashboardSnapshot, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1.35f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Memory, null, tint = DashColors.text, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = data.ramType,
                        color = DashColors.text,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                MetricBarRow(stringResource(R.string.ram_used), data.ramUsed, "", data.ramUsedBar, 13.sp, 14.sp)
                MetricBarRow(stringResource(R.string.ram_free), data.ramFree, "", data.ramFreeBar, 13.sp, 14.sp)
                MetricBarRow(stringResource(R.string.ram_usage), data.ramUsage, "", data.ramUsageBar, 13.sp, 14.sp)
            }
            Column(
                modifier = Modifier.weight(0.65f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                TempValue(temp = data.boardTemp, tempSize = 32.sp)
            }
        }
    }
}

@Composable
fun StoragePanel(data: DashboardSnapshot, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            DriveRow(stringResource(R.string.drive_c), data.driveCUsage, data.driveCBar, data.driveCTemp)
            DriveRow(stringResource(R.string.drive_d), data.driveDUsage, data.driveDBar, data.driveDTemp)
            DriveRow(stringResource(R.string.drive_e), data.driveEUsage, data.driveEBar, data.driveETemp)
        }
    }
}

@Composable
private fun DriveRow(name: String, usage: String, bar: Float, temp: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Storage, null, tint = DashColors.text, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "$name: $usage%",
                color = DashColors.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            ThinProgressBar(progress = bar)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.temp_prefix, temp),
            color = DashColors.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun LogoTimePanel(data: DashboardSnapshot, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val logoColor = DashColors.text
                Canvas(modifier = Modifier.size(28.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.85f)
                        lineTo(size.width * 0.5f, size.height * 0.15f)
                        lineTo(size.width * 0.85f, size.height * 0.85f)
                        close()
                    }
                    drawPath(path, logoColor, style = Stroke(width = 3f))
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.brand_name),
                    color = DashColors.text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CalendarMonth, null, tint = DashColors.text, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(data.date, color = DashColors.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = DashColors.text, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(data.time, color = DashColors.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun NetFanPanel(
    data: DashboardSnapshot,
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
) {
    DashPanel(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (onToggleFullscreen != null) 28.dp else 0.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Cloud, null, tint = DashColors.text, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${stringResource(R.string.upload)}  ${data.upload} ${stringResource(R.string.unit_kbs)}",
                            color = DashColors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "${stringResource(R.string.download)}  ${data.download} ${stringResource(R.string.unit_kbs)}",
                            color = DashColors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Speaker, null, tint = DashColors.text, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    ThinProgressBar(progress = data.volumeBar, modifier = Modifier.weight(1f))
                }
                FanRow("CPU/FAN", data.cpuFan, DashColors.accentYellow)
                FanRow(
                    label = "GPU/FAN",
                    value = data.gpuFan,
                    color = if ((data.gpuFan.toIntOrNull() ?: 0) == 0) DashColors.accentRed else DashColors.accentYellow,
                )
            }
            if (onToggleFullscreen != null) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp),
                ) {
                    Icon(
                        imageVector = if (isFullscreen) {
                            Icons.Filled.FullscreenExit
                        } else {
                            Icons.Filled.Fullscreen
                        },
                        contentDescription = stringResource(
                            if (isFullscreen) R.string.exit_fullscreen else R.string.fullscreen,
                        ),
                        tint = DashColors.text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FanRow(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Toys, null, tint = DashColors.text, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = DashColors.text, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(
            text = "$value RPM",
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

