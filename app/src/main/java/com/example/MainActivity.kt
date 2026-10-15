package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.overlay.ChessOverlayService
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GameReviewScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ChessGreenOnPrimary
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.ChessHelperTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AppTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Главная", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports),
    SIMULATOR("Симулятор", Icons.Filled.GridOn, Icons.Outlined.GridOn),
    REVIEW("Разбор", Icons.Filled.Assessment, Icons.Outlined.Assessment),
    SETTINGS("Настройки", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ChessHelperTheme {
                val context = LocalContext.current
                val settings by viewModel.settings.collectAsState()
                var currentTab by rememberSaveable { mutableStateOf(AppTab.DASHBOARD) }

                // MediaProjection Activity Result Launcher (optional AI camera auto-capture)
                val projectionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                        val serviceIntent = Intent(context, ChessOverlayService::class.java).apply {
                            action = ChessOverlayService.ACTION_START
                            putExtra(ChessOverlayService.EXTRA_RESULT_CODE, result.resultCode)
                            putExtra(ChessOverlayService.EXTRA_RESULT_DATA, result.data)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                        Toast.makeText(context, "Авто-сканирование экрана запущено!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Разрешение на захват экрана отменено", Toast.LENGTH_SHORT).show()
                    }
                }

                // Direct overlay launcher using SYSTEM_ALERT_WINDOW (no screen-share prompt forced!)
                val onLaunchOverlay: () -> Unit = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        Toast.makeText(context, "Разрешите «Отображение поверх других приложений» для Chess Helper", Toast.LENGTH_LONG).show()
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    } else {
                        val serviceIntent = Intent(context, ChessOverlayService::class.java).apply {
                            action = ChessOverlayService.ACTION_START
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                        Toast.makeText(context, "Оверлей запущен! Экран не блокируется.", Toast.LENGTH_SHORT).show()
                    }
                }

                val onLaunchScreenCapture: () -> Unit = {
                    val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
                }

                val onStopOverlay: () -> Unit = {
                    val stopIntent = Intent(context, ChessOverlayService::class.java).apply {
                        action = ChessOverlayService.ACTION_STOP
                    }
                    context.startService(stopIntent)
                    Toast.makeText(context, "Оверлей остановлен", Toast.LENGTH_SHORT).show()
                }

                if (!settings.onboardingCompleted) {
                    OnboardingScreen(
                        onComplete = { viewModel.setOnboardingCompleted() }
                    )
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = DarkSurface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                            ) {
                                AppTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = ChessGreenOnPrimary,
                                            selectedTextColor = ChessGreenPrimary,
                                            indicatorColor = ChessGreenPrimary,
                                            unselectedIconColor = TextSecondary,
                                            unselectedTextColor = TextSecondary
                                        )
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkBackground)
                                .padding(innerPadding)
                        ) {
                            when (currentTab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    viewModel = viewModel,
                                    onLaunchOverlayRequested = onLaunchOverlay,
                                    onLaunchScreenCaptureRequested = onLaunchScreenCapture,
                                    onStopOverlayRequested = onStopOverlay
                                )
                                AppTab.SIMULATOR -> SimulatorScreen(viewModel = viewModel)
                                AppTab.REVIEW -> GameReviewScreen(viewModel = viewModel)
                                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
