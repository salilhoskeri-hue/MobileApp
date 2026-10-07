package com.supplytrack.app.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E4D64),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7E4EF),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFF8A5100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCBE),
    onSecondaryContainer = Color(0xFF2C1600),
    tertiary = Color(0xFF3B6939),
    tertiaryContainer = Color(0xFFBCF0B4),
    onTertiaryContainer = Color(0xFF002204),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF83CFE6),
    onPrimary = Color(0xFF003545),
    primaryContainer = Color(0xFF004D63),
    onPrimaryContainer = Color(0xFFB7E4EF),
    secondary = Color(0xFFFFB870),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF693C00),
    onSecondaryContainer = Color(0xFFFFDCBE),
    tertiary = Color(0xFFA1D39A),
    tertiaryContainer = Color(0xFF235024),
    onTertiaryContainer = Color(0xFFBCF0B4),
)

@Composable
fun SupplyTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
