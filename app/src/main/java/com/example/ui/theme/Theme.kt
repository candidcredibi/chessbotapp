package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ChessGreenPrimary,
    onPrimary = ChessGreenOnPrimary,
    primaryContainer = Color(0xFF005329),
    onPrimaryContainer = Color(0xFF6BFF9E),
    secondary = TacticalAmber,
    onSecondary = TacticalAmberOn,
    secondaryContainer = Color(0xFF564300),
    onSecondaryContainer = Color(0xFFFFE082),
    tertiary = CyanAccent,
    onTertiary = CyanAccentOn,
    tertiaryContainer = Color(0xFF004F58),
    onTertiaryContainer = Color(0xFF80F3FF),
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B)
)

private val LightColorScheme = DarkColorScheme // Default to high-contrast dark theme as requested

@Composable
fun ChessHelperTheme(
    darkTheme: Boolean = true, // default dark theme as requested
    dynamicColor: Boolean = false, // Keep sleek intentional theme by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
