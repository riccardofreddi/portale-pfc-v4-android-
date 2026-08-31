package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PfcAmber,
    onPrimary = Color.Black,
    primaryContainer = PfcNavyMid,
    onPrimaryContainer = PfcAmberSoft,
    secondary = PfcSlateLight,
    onSecondary = PfcNavyDark,
    secondaryContainer = PfcNavyMid,
    onSecondaryContainer = PfcTextPrimaryDark,
    tertiary = PfcInfo,
    background = PfcBackgroundDark,
    onBackground = PfcTextPrimaryDark,
    surface = PfcSurfaceDark,
    onSurface = PfcTextPrimaryDark,
    surfaceVariant = PfcSurfaceAltDark,
    onSurfaceVariant = PfcTextSecondaryDark,
    outline = PfcBorderDark,
    outlineVariant = PfcBorderStrongDark
)

private val LightColorScheme = lightColorScheme(
    primary = PfcNavyDark,
    onPrimary = Color.White,
    primaryContainer = PfcAmberSoft,
    onPrimaryContainer = PfcAmberDark,
    secondary = PfcAmber,
    onSecondary = Color.White,
    secondaryContainer = PfcSurfaceAltLight,
    onSecondaryContainer = PfcNavyDark,
    tertiary = PfcInfo,
    background = PfcBackgroundLight,
    onBackground = PfcTextPrimary,
    surface = PfcSurfaceLight,
    onSurface = PfcTextPrimary,
    surfaceVariant = PfcSurfaceAltLight,
    onSurfaceVariant = PfcTextSecondary,
    outline = PfcBorderLight,
    outlineVariant = PfcBorderStrongLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
