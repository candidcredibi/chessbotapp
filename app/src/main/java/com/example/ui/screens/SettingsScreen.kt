package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
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

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    var isScriptExpanded by remember { mutableStateOf(false) }
    var isLicenseExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = ChessGreenPrimary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text("Настройки", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Параметры движка, оверлея и лицензии", fontSize = 13.sp, color = TextSecondary)
            }
        }

        // Section 1: Engine Parameters
        Text("Движок и анализ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ChessGreenPrimary, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Think First Delay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Режим «Сначала подумай»", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = if (settings.thinkFirstSeconds == 0) "Подсказка появляется сразу" else "Задержка: ${settings.thinkFirstSeconds} сек.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Text("${settings.thinkFirstSeconds} с", color = TacticalAmber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Slider(
                    value = settings.thinkFirstSeconds.toFloat(),
                    onValueChange = { viewModel.setThinkFirstSeconds(it.roundToInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = TacticalAmber,
                        activeTrackColor = TacticalAmber,
                        inactiveTrackColor = DarkSurfaceContainer
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Power Saving
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Энергосбережение", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text("Снижает частоту анализа при нагреве и заряде <20%", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = settings.powerSavingEnabled,
                        onCheckedChange = { viewModel.setPowerSaving(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ChessGreenPrimary,
                            checkedTrackColor = Color(0xFF0D331D)
                        )
                    )
                }
            }
        }

        // Section 2: Machine Learning & Python Script
        Text("Компьютерное зрение и ML", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CyanAccent, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isScriptExpanded = !isScriptExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Скрипт обучения TFLite классификатора", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Icon(
                        imageVector = if (isScriptExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                AnimatedVisibility(visible = isScriptExpanded) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Text(
                            text = "Инструкция по обучению легковесного TFLite классификатора (13 классов: пустая клетка + 12 фигур):\n\n" +
                                    "1. Сбор датасета: Сделайте 30–50 скриншотов досок разных тем (Classic, Wood, Neon) в Chess.com.\n" +
                                    "2. Нарезка на 64 клетки размером 64×64 px.\n" +
                                    "3. Аугментации: яркость (±15%), контраст, мелкий сдвиг (±2px).\n" +
                                    "4. Обучение MobileNetV3-Small или простой 4-слойной CNN.\n" +
                                    "5. Квантование в INT8 через TFLiteConverter.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF080B10),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = """
# python train_chess_tflite.py
import tensorflow as tf
from tensorflow.keras import layers, models

def create_model():
    model = models.Sequential([
        layers.Input(shape=(64, 64, 3)),
        layers.Conv2D(32, (3,3), activation='relu'),
        layers.MaxPooling2D(2,2),
        layers.Conv2D(64, (3,3), activation='relu'),
        layers.GlobalAveragePooling2D(),
        layers.Dense(64, activation='relu'),
        layers.Dropout(0.2),
        layers.Dense(13, activation='softmax')
    ])
    return model

# Экспорт в квантованный INT8 TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()
open("chess_pieces.tflite", "wb").write(tflite_model)
                                """.trimIndent(),
                                color = Color(0xFF81A1C1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Legal & Licenses
        Text("О приложении и лицензии", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isLicenseExpanded = !isLicenseExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ChessGreenPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Лицензии открытого ПО (GPLv3, Apache 2.0)", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    Icon(
                        imageVector = if (isLicenseExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }

                AnimatedVisibility(visible = isLicenseExpanded) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "• Stockfish Chess Engine: распространяется под лицензией GNU General Public License v3 (GPLv3). Исходный код доступен на официальном сайте stockfishchess.org.\n\n" +
                                    "• OpenCV for Android: лицензия Apache License 2.0.\n\n" +
                                    "• Chess.com является зарегистрированным товарным знаком Chess.com, LLC. Данное приложение создано независимыми разработчиками в образовательных целях и не аффилировано с Chess.com.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
