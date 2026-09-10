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
import com.example.ui.NightModeOption

private val CinemaDarkColorScheme = darkColorScheme(
    primary = CinemaPrimary,
    onPrimary = Color.Black,
    primaryContainer = CinemaPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = CinemaSecondary,
    onSecondary = Color.White,
    tertiary = CinemaAccent,
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = CinemaPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0284C7),
    onPrimaryContainer = Color.White,
    secondary = CinemaSecondary,
    onSecondary = Color.White,
    tertiary = CinemaAccent,
    background = AmoledBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = AmoledSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private val CinemaLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = CinemaSecondary,
    onSecondary = Color.White,
    tertiary = CinemaAccent,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun MoviLishTheme(
    nightMode: NightModeOption = NightModeOption.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (nightMode) {
        NightModeOption.DARK -> CinemaDarkColorScheme
        NightModeOption.AMOLED_BLACK -> AmoledDarkColorScheme
        NightModeOption.LIGHT -> CinemaLightColorScheme
        NightModeOption.SYSTEM -> if (isSystemInDarkTheme()) CinemaDarkColorScheme else CinemaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MoviLishTheme(
        nightMode = if (darkTheme) NightModeOption.DARK else NightModeOption.LIGHT,
        content = content
    )
}
