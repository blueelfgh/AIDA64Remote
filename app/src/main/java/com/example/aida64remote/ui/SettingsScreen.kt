package com.example.aida64remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.aida64remote.R
import com.example.aida64remote.data.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialHost: String,
    initialPort: Int,
    keepScreenOn: Boolean,
    themeMode: ThemeMode,
    canCancel: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onConnect: (host: String, port: Int) -> Unit,
    onCancel: () -> Unit,
) {
    var host by remember(initialHost) { mutableStateOf(initialHost) }
    var portText by remember(initialPort) { mutableStateOf(initialPort.toString()) }
    var hostError by remember { mutableStateOf<String?>(null) }
    var portError by remember { mutableStateOf<String?>(null) }

    val invalidHost = stringResource(R.string.invalid_host)
    val invalidPort = stringResource(R.string.invalid_port)

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.settings_title)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = host,
                onValueChange = {
                    host = it
                    hostError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.host_label)) },
                singleLine = true,
                isError = hostError != null,
                supportingText = hostError?.let { { Text(it) } },
            )
            OutlinedTextField(
                value = portText,
                onValueChange = {
                    portText = it.filter { ch -> ch.isDigit() }.take(5)
                    portError = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.port_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = portError != null,
                supportingText = portError?.let { { Text(it) } },
            )

            SettingsSwitchRow(
                title = stringResource(R.string.keep_screen_on),
                subtitle = stringResource(R.string.keep_screen_on_hint),
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnChange,
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.theme_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.theme_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = themeMode == ThemeMode.Dark,
                        onClick = { onThemeModeChange(ThemeMode.Dark) },
                        label = { Text(stringResource(R.string.theme_dark)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.Light,
                        onClick = { onThemeModeChange(ThemeMode.Light) },
                        label = { Text(stringResource(R.string.theme_light)) },
                    )
                    FilterChip(
                        selected = themeMode == ThemeMode.System,
                        onClick = { onThemeModeChange(ThemeMode.System) },
                        label = { Text(stringResource(R.string.theme_system)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val trimmedHost = host.trim()
                    val port = portText.toIntOrNull()
                    var ok = true
                    if (trimmedHost.isEmpty()) {
                        hostError = invalidHost
                        ok = false
                    }
                    if (port == null || port !in 1..65535) {
                        portError = invalidPort
                        ok = false
                    }
                    if (ok && port != null) {
                        onConnect(trimmedHost, port)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.connect))
            }
            if (canCancel) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
