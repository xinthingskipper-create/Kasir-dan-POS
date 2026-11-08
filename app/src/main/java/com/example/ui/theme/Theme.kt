package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PosBlueDark,
    onPrimary = PosBlueOnDark,
    primaryContainer = PosBlueDarkContainer,
    secondary = PosTealDark,
    background = PosBackgroundDark,
    surface = PosSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurface = PosOnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = PosBluePrimary,
    onPrimary = PosBlueOnPrimary,
    primaryContainer = PosBluePrimaryContainer,
    onPrimaryContainer = PosBlueOnPrimaryContainer,
    secondary = PosTealSecondary,
    onSecondary = PosTealOnSecondary,
    secondaryContainer = PosTealContainer,
    onSecondaryContainer = PosTealOnContainer,
    tertiary = PosAmberTertiary,
    background = PosBackgroundLight,
    surface = PosSurfaceLight,
    surfaceVariant = PosSurfaceVariantLight,
    onSurface = PosOnSurfaceLight,
    outline = PosOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand colors for cohesive POS look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
