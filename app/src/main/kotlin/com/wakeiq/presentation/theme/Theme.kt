package com.wakeiq.presentation.theme

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
    primary = AlarmPrimary,
    onPrimary = AlarmOnPrimary,
    secondary = AlarmSecondary,
    background = AlarmBackground,
    surface = AlarmSurface,
    onBackground = AlarmOnBackground,
    onSurface = AlarmOnSurface,
)

private val LightColorScheme = lightColorScheme(
    primary = AlarmPrimary,
    onPrimary = AlarmOnPrimary,
    secondary = AlarmSecondary,
)

@Composable
fun WakeIQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        typography = AlarmTypography,
        content = content,
    )
}
