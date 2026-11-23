package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryCobalt,
    onPrimary = Color.White,
    secondary = InkSlate,
    onSecondary = Color.White,
    tertiary = GoldCommodity,
    background = BgLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = ElevatedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryCobalt,
    onPrimary = Color.White,
    secondary = Color(0xFF38BDF8),
    onSecondary = InkSlate,
    tertiary = GoldCommodity,
    background = BgDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
