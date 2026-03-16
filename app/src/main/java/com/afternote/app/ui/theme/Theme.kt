package com.afternote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

private fun buildColorScheme(
    primary: Color,
    onPrimary: Color,
    container: Color,
    darkSurface: Color,
    dark: Boolean
): ColorScheme = if (dark) darkColorScheme(
    primary          = primary,
    onPrimary        = onPrimary,
    primaryContainer = container,
    surface          = darkSurface,
    background       = darkSurface
) else lightColorScheme(
    primary          = primary,
    onPrimary        = onPrimary,
    primaryContainer = container
)

val themeOptions = listOf("Indigo", "Forest", "Sunset")

@Composable
fun AfterNoteTheme(
    themeIndex: Int = 0,
    darkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeIndex) {
        1    -> buildColorScheme(ForestPrimary, ForestOnPrimary, ForestContainer, ForestDarkSurface, darkMode)
        2    -> buildColorScheme(SunsetPrimary, SunsetOnPrimary, SunsetContainer, SunsetDarkSurface, darkMode)
        else -> buildColorScheme(IndigoPrimary, IndigoOnPrimary, IndigoContainer, IndigoDarkSurface, darkMode)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AppTypography,
        content     = content
    )
}
