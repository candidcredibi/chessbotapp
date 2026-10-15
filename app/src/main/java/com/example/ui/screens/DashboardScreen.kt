package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.EngineStyle
import com.example.ui.MainViewModel
import com.example.ui.theme.ArrowAlternative2
import com.example.ui.theme.ArrowAlternative3
import com.example.ui.theme.ArrowBestMove
import com.example.ui.theme.ChessGreenOnPrimary
import com.example.ui.theme.ChessGreenPrimary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceContainer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TacticalAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onLaunchOverlayRequested: () -> Unit,
    onLaunchScreenCaptureRequested: () -> Unit,
    onStopOverlayRequested: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isRunning by viewModel.isOverlayRunning.collectAsState()
    val liveEvals by viewModel.overlayEvaluations.collectAsState()
    val thermalStatus = viewModel.getThermalStatus()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // App Title & Status Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Chess Helper",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Помощник реального времени",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }

            // Thermal & Status Pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DarkSurfaceVariant,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Thermostat,
                        contentDescription = null,
                        tint = if (thermalStatus.contains("нагрев", true)) TacticalAmber else ChessGreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = thermalStatus,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Big Main Hero Status Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRunning) Color(0xFF0D2517) else DarkSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isRunning) ChessGreenPrimary.copy(alpha = 0.5f) else Color(0xFF222C3E),
                    shape = RoundedCornerShape(24.dp)
                )
                .shadow(12.dp, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isRunning) ChessGreenPrimary else Color(0xFF64748B))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Оверлей активен" else "Оверлей остановлен",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isRunning) ChessGreenPrimary else Color(0xFF94A3B8)
                        )
                    }

                    if (isRunning) {
                        Text(
                            text = "FPS: 2–5",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isRunning) {
                        "Оверлей запущен поверх других окон! Экран НЕ блокируется — вы можете свободно играть в Chess.com, а подсказки отображаются прямо поверх."
                    } else {
                        "Запустите оверлей поверх всех окон. Он работает прозрачно и не мешает касаниям в сторонних приложениях."
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Launch / Stop Button
                if (!isRunning) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onLaunchOverlayRequested,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChessGreenPrimary,
                                contentColor = ChessGreenOnPrimary
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("launch_overlay_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Запустить оверлей", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = onLaunchScreenCaptureRequested,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Включить авто-сканирование экрана", fontSize = 13.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = onStopOverlayRequested,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5252),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("stop_overlay_button")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Остановить оверлей", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Open Chess.com quick launcher
        OutlinedButton(
            onClick = {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.chess")
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    val storeIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.chess"))
                    runCatching { context.startActivity(storeIntent) }
                }
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Открыть приложение Chess.com")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Engine Preset Styles
        Text(
            text = "Стиль игры (эвристика анализа)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Настраивает приоритеты ранжирования ходов движка",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            EngineStyle.values().forEach { styleItem ->
                val isSelected = settings.style == styleItem
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setStyle(styleItem) },
                    label = { Text(styleItem.titleRu, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChessGreenPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sliders Card: Depth & MultiPV
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Depth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Глубина анализа (полуходов)", color = TextPrimary, fontSize = 14.sp)
                    Text("${settings.engineDepth}", color = ChessGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Slider(
                    value = settings.engineDepth.toFloat(),
                    onValueChange = { viewModel.setEngineDepth(it.roundToInt()) },
                    valueRange = 6f..16f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = ChessGreenPrimary,
                        activeTrackColor = ChessGreenPrimary,
                        inactiveTrackColor = DarkSurfaceContainer
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // MultiPV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Количество линий (стрелок)", color = TextPrimary, fontSize = 14.sp)
                    Text("${settings.multiPv}", color = TacticalAmber, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Slider(
                    value = settings.multiPv.toFloat(),
                    onValueChange = { viewModel.setMultiPv(it.roundToInt()) },
                    valueRange = 1f..3f,
                    steps = 1,
                    colors = SliderDefaults.colors(
                        thumbColor = TacticalAmber,
                        activeTrackColor = TacticalAmber,
                        inactiveTrackColor = DarkSurfaceContainer
                    )
                )
            }
        }

        // Live evaluations if active
        if (liveEvals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Текущие рекомендации оверлея",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            liveEvals.forEachIndexed { index, item ->
                val badgeColor = when (index) {
                    0 -> ArrowBestMove
                    1 -> ArrowAlternative2
                    else -> ArrowAlternative3
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Линия #${index + 1}: ${item.move.uci}",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = item.formattedScore,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
