package com.silifton.textwright.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.silifton.textwright.data.AppSettings

// The palette follows the app icon: indigo into violet, with the pencil's amber as the accent.
private val BrandLight = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF2E1065),
    tertiary = Color(0xFFB45309),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF451A03),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE4E4F0),
    onSurfaceVariant = Color(0xFF5A5A66),
    outline = Color(0xFF777684),
    outlineVariant = Color(0xFFD5D4E0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F6FC),
    surfaceContainer = Color(0xFFF1F1F8),
    surfaceContainerHigh = Color(0xFFEBEBF3),
    surfaceContainerHighest = Color(0xFFE5E5EE),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFB4B9FF),
    onPrimary = Color(0xFF15137A),
    primaryContainer = Color(0xFF4338CA),
    onPrimaryContainer = Color(0xFFEEF0FF),
    secondary = Color(0xFFD0BCFF),
    onSecondary = Color(0xFF381E72),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFEDE9FE),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFF5B3F00),
    onTertiaryContainer = Color(0xFFFEF3C7),
    background = Color(0xFF111117),
    onBackground = Color(0xFFE6E4EC),
    surface = Color(0xFF111117),
    onSurface = Color(0xFFE6E4EC),
    surfaceVariant = Color(0xFF3A3A46),
    onSurfaceVariant = Color(0xFFB9B8C6),
    outline = Color(0xFF8B8A98),
    outlineVariant = Color(0xFF3A3A46),
    surfaceContainerLowest = Color(0xFF0C0C11),
    surfaceContainerLow = Color(0xFF18181F),
    surfaceContainer = Color(0xFF1D1D25),
    surfaceContainerHigh = Color(0xFF27272F),
    surfaceContainerHighest = Color(0xFF32323B),
)

@Composable
fun TextwrightTheme(content: @Composable () -> Unit) {
    val dark = when (AppSettings.themeMode) {
        AppSettings.ThemeMode.System -> isSystemInDarkTheme()
        AppSettings.ThemeMode.Light -> false
        AppSettings.ThemeMode.Dark -> true
    }
    val colors = when {
        AppSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        dark -> BrandDark
        else -> BrandLight
    }
    // The system bars are drawn edge to edge, so their icons must follow the app's theme, not the system's.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
