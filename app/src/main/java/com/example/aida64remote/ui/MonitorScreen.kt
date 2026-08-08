package com.example.aida64remote.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aida64remote.R
import com.example.aida64remote.model.ConnectionStatus
import com.example.aida64remote.ui.components.CpuPanel
import com.example.aida64remote.ui.components.FpsPanel
import com.example.aida64remote.ui.components.GpuPanel
import com.example.aida64remote.ui.components.HexBackground
import com.example.aida64remote.ui.components.LogoTimePanel
import com.example.aida64remote.ui.components.NetFanPanel
import com.example.aida64remote.ui.components.RamPanel
import com.example.aida64remote.ui.components.StoragePanel
import com.example.aida64remote.ui.theme.DashColors

private val LeftColWeight = 1f
private val RightColWeight = 1.15f
private const val DoubleBackExitMs = 2000L

@Composable
fun MonitorScreen(
    state: MonitorUiState,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
) {
    val context = LocalContext.current
    val exitHint = stringResource(R.string.press_again_to_exit)
    var lastBackPressedAt by remember { mutableLongStateOf(0L) }

    BackHandler {
        when {
            state.isFullscreen -> onExitFullscreen()
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressedAt < DoubleBackExitMs) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressedAt = now
                    Toast.makeText(context, exitHint, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleFullscreen() })
            },
    ) {
        HexBackground()

        when {
            state.status == ConnectionStatus.Error -> {
                CenterMessage(
                    message = state.errorMessage ?: stringResource(R.string.status_error),
                    actionLabel = stringResource(R.string.retry),
                    onAction = onRetry,
                    onSettings = onOpenSettings,
                )
            }
            state.status == ConnectionStatus.Connecting ||
                (state.status == ConnectionStatus.Connected &&
                    state.dashboard.cpuTemp == "—" &&
                    state.dashboard.cpuClock == "—") -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DashColors.accentYellow)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (state.status) {
                                ConnectionStatus.Reconnecting -> stringResource(R.string.status_reconnecting)
                                else -> stringResource(R.string.waiting_data)
                            },
                            color = DashColors.text,
                        )
                        state.errorMessage?.let {
                            Text(it, color = DashColors.muted, fontSize = 12.sp)
                        }
                    }
                }
                if (!state.isFullscreen) {
                    TopActions(state = state, onDisconnect = onDisconnect, onOpenSettings = onOpenSettings)
                }
            }
            else -> {
                DashboardGrid(
                    state = state,
                    onDisconnect = onDisconnect,
                    onOpenSettings = onOpenSettings,
                    onToggleFullscreen = onToggleFullscreen,
                )
            }
        }
    }
}

@Composable
private fun DashboardGrid(
    state: MonitorUiState,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    val data = state.dashboard
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (state.isFullscreen) Modifier else Modifier.statusBarsPadding())
            .padding(if (state.isFullscreen) 6.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!state.isFullscreen) {
            TopActions(state = state, onDisconnect = onDisconnect, onOpenSettings = onOpenSettings)
        }

        Row(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CpuPanel(
                data = data,
                showKeepScreenOn = state.keepScreenOn,
                modifier = Modifier.weight(LeftColWeight).fillMaxSize(),
            )
            GpuPanel(
                data = data,
                modifier = Modifier.weight(RightColWeight).fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FpsPanel(
                data = data,
                modifier = Modifier.weight(LeftColWeight).fillMaxSize(),
            )
            RamPanel(
                data = data,
                modifier = Modifier.weight(RightColWeight).fillMaxSize(),
            )
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoragePanel(data = data, modifier = Modifier.weight(1.2f).fillMaxSize())
            LogoTimePanel(data = data, modifier = Modifier.weight(0.85f).fillMaxSize())
            NetFanPanel(
                data = data,
                isFullscreen = state.isFullscreen,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.weight(1f).fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TopActions(
    state: MonitorUiState,
    onDisconnect: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = statusLabel(state.status),
            color = when (state.status) {
                ConnectionStatus.Connected -> DashColors.accentYellow
                ConnectionStatus.Error -> DashColors.accentRed
                else -> DashColors.muted
            },
            fontSize = 12.sp,
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onDisconnect) {
            Text(stringResource(R.string.disconnect), color = DashColors.text)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings), tint = DashColors.text)
        }
    }
}

@Composable
private fun CenterMessage(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    onSettings: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = DashColors.text)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) { Text(actionLabel) }
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onSettings) {
                Text(stringResource(R.string.settings), color = DashColors.muted)
            }
        }
    }
}

@Composable
private fun statusLabel(status: ConnectionStatus): String = when (status) {
    ConnectionStatus.Idle -> stringResource(R.string.status_idle)
    ConnectionStatus.Connecting -> stringResource(R.string.status_connecting)
    ConnectionStatus.Connected -> stringResource(R.string.status_connected)
    ConnectionStatus.Reconnecting -> stringResource(R.string.status_reconnecting)
    ConnectionStatus.Error -> stringResource(R.string.status_error)
}
