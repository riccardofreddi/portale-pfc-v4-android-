package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GeoAccentLavender,
    onPrimary = GeoOnPrimaryContainer,
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = GeoPrimaryContainer,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = GeoBackgroundDark,
    onBackground = GeoTextPrimaryDark,
    surface = GeoSurfaceDark,
    onSurface = GeoTextPrimaryDark,
    surfaceVariant = GeoSurfaceCardDark,
    onSurfaceVariant = GeoTextSecondaryDark,
    surfaceContainer = GeoSurfaceCardDark,
    surfaceContainerHigh = GeoSurfaceAltDark,
    outline = GeoBorderDark,
    outlineVariant = GeoBorderStrongDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeoPrimary,
    onPrimary = GeoOnPrimary,
    primaryContainer = GeoPrimaryContainer,
    onPrimaryContainer = GeoOnPrimaryContainer,
    secondary = GeoSecondary,
    onSecondary = Color.White,
    secondaryContainer = GeoSecondaryContainer,
    onSecondaryContainer = GeoOnSecondaryContainer,
    tertiary = GeoTertiary,
    onTertiary = Color.White,
    tertiaryContainer = GeoTertiaryContainer,
    onTertiaryContainer = Color(0xFF31111D),
    background = GeoBackgroundLight,
    onBackground = GeoTextPrimaryLight,
    surface = GeoSurfaceLight,
    onSurface = GeoTextPrimaryLight,
    surfaceVariant = GeoSurfaceCardLight,
    onSurfaceVariant = GeoTextSecondaryLight,
    surfaceContainer = GeoSurfaceCardLight,
    surfaceContainerHigh = GeoSurfaceAltLight,
    outline = GeoBorderLight,
    outlineVariant = GeoBorderVariantLight
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
