package com.example.aida64remote

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aida64remote.data.ConnectionConfig
import com.example.aida64remote.model.ConnectionStatus
import com.example.aida64remote.ui.MonitorScreen
import com.example.aida64remote.ui.SensorViewModel
import com.example.aida64remote.ui.SettingsScreen
import com.example.aida64remote.ui.theme.AIDA64RemoteTheme
import com.example.aida64remote.widget.WidgetRefreshWorker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SensorViewModel = viewModel()
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            AIDA64RemoteTheme(themeMode = appSettings.themeMode) {
                KeepScreenOnEffect(enabled = appSettings.keepScreenOn)
                FullscreenEffect(enabled = uiState.isFullscreen)
                Aida64App(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        WidgetRefreshWorker.enqueue(this)
    }

    @Composable
    private fun KeepScreenOnEffect(enabled: Boolean) {
        DisposableEffect(enabled) {
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    @Composable
    private fun FullscreenEffect(enabled: Boolean) {
        DisposableEffect(enabled) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (enabled) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            onDispose {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

@Composable
private fun Aida64App(viewModel: SensorViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedConfig by viewModel.savedConfig.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    var autoConnected by remember { mutableStateOf(false) }

    LaunchedEffect(settingsLoaded, savedConfig) {
        if (!settingsLoaded || autoConnected) return@LaunchedEffect
        if (uiState.status == ConnectionStatus.Idle && savedConfig.host.isNotBlank()) {
            autoConnected = true
            viewModel.connect(savedConfig)
            navController.navigate("monitor") {
                popUpTo("settings") { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "settings") {
        composable("settings") {
            val canCancel = navController.previousBackStackEntry != null
            SettingsScreen(
                initialHost = savedConfig.host.ifBlank { ConnectionConfig.DEFAULT_HOST },
                initialPort = if (savedConfig.host.isBlank()) {
                    appSettings.serviceType.defaultPort
                } else {
                    savedConfig.port
                },
                serviceType = appSettings.serviceType,
                keepScreenOn = appSettings.keepScreenOn,
                themeMode = appSettings.themeMode,
                canCancel = canCancel,
                onServiceTypeChange = viewModel::setServiceType,
                onKeepScreenOnChange = viewModel::setKeepScreenOn,
                onThemeModeChange = viewModel::setThemeMode,
                onConnect = { host, port, serviceType ->
                    autoConnected = true
                    viewModel.saveAndConnect(host, port, serviceType)
                    navController.navigate("monitor") {
                        popUpTo("settings") { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable("monitor") {
            MonitorScreen(
                state = uiState,
                onDisconnect = {
                    viewModel.disconnect()
                    navController.navigate("settings") {
                        popUpTo("monitor") { inclusive = true }
                    }
                },
                onRetry = { viewModel.connect(uiState.config) },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onToggleFullscreen = viewModel::toggleFullscreen,
                onExitFullscreen = { viewModel.setFullscreen(false) },
            )
        }
    }
}
