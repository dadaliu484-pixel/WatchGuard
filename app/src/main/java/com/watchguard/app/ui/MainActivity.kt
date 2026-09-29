package com.watchguard.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.watchguard.app.service.WatchGuardService
import com.watchguard.app.ui.screens.HomeScreen
import com.watchguard.app.ui.screens.HyperOsGuideScreen
import com.watchguard.app.ui.screens.SettingsScreen
import com.watchguard.app.ui.theme.DarkBackground
import com.watchguard.app.ui.theme.WatchGuardTheme
import com.watchguard.app.ui.viewmodel.MainViewModel
import com.watchguard.app.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    SETTINGS,
    HYPEROS_GUIDE
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var boundService: WatchGuardService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? WatchGuardService.LocalBinder
            boundService = binder?.getService()
            isBound = true

            boundService?.let { ws ->
                lifecycleScope.launch {
                    ws.guardStatus.collectLatest { status ->
                        viewModel.updateServiceStatus(status, ws.debounceCountdown.value)
                    }
                }
                lifecycleScope.launch {
                    ws.debounceCountdown.collectLatest { count ->
                        viewModel.updateServiceStatus(ws.guardStatus.value, count)
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WatchGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    WatchGuardMainContent(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 绑定前台守护服务以同步状态
        val intent = Intent(this, WatchGuardService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions()
        viewModel.refreshPairedDevices()
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

@Composable
fun WatchGuardMainContent(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.checkPermissions()
        viewModel.refreshPairedDevices()
    }

    LaunchedEffect(Unit) {
        val missing = uiState.missingPermissions
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            Screen.HOME -> {
                HomeScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onRequestPermissions = {
                        val missing = PermissionHelper.getMissingPermissions(viewModel.getApplication())
                        if (missing.isNotEmpty()) {
                            permissionLauncher.launch(missing.toTypedArray())
                        }
                    },
                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                    onNavigateToHyperOsGuide = { currentScreen = Screen.HYPEROS_GUIDE }
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    config = uiState.guardConfig,
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = Screen.HOME }
                )
            }
            Screen.HYPEROS_GUIDE -> {
                HyperOsGuideScreen(
                    onNavigateBack = { currentScreen = Screen.HOME }
                )
            }
        }
    }
}
