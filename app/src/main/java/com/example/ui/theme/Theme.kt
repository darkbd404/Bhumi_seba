package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class NeuPalette(
    val canvas: Color,
    val surface: Color,
    val lightShadow: Color,
    val darkShadow: Color,
    val isDark: Boolean
)

val LocalNeuPalette = staticCompositionLocalOf {
    NeuPalette(
        canvas = NeuCanvasLight,
        surface = NeuSurfaceLight,
        lightShadow = NeuHighlightLight,
        darkShadow = NeuShadowLight,
        isDark = false
    )
}

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = OnTealContainer,
    tertiary = SurveyGold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = OnGoldContainer,
    background = NeuCanvasLight,
    onBackground = TextPrimaryLight,
    surface = NeuSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFDDE6EE),
    onSurfaceVariant = TextSecondaryLight,
    error = CoralError,
    onError = Color.White,
    outline = Color(0xFFB5C4D2)
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A4738),
    onPrimaryContainer = Color(0xFFB7F0DE),
    secondary = Color(0xFF4AB7C9),
    onSecondary = Color(0xFF03262C),
    secondaryContainer = Color(0xFF0E4550),
    onSecondaryContainer = Color(0xFFC5F2FA),
    tertiary = Color(0xFFF0BC42),
    onTertiary = Color(0xFF332200),
    tertiaryContainer = Color(0xFF533900),
    onTertiaryContainer = Color(0xFFFFE4A6),
    background = NeuCanvasDark,
    onBackground = TextPrimaryDark,
    surface = NeuSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF232F3A),
    onSurfaceVariant = TextSecondaryDark,
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF3F0B0B),
    outline = Color(0xFF364654)
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BhumiSurveyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val neuPalette = if (darkTheme) {
        NeuPalette(
            canvas = NeuCanvasDark,
            surface = NeuSurfaceDark,
            lightShadow = NeuHighlightDark,
            darkShadow = NeuShadowDark,
            isDark = true
        )
    } else {
        NeuPalette(
            canvas = NeuCanvasLight,
            surface = NeuSurfaceLight,
            lightShadow = NeuHighlightLight,
            darkShadow = NeuShadowLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalNeuPalette provides neuPalette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
