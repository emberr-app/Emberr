package com.emberr.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberWallpaperColorSchemes(): ThemeColorSchemes? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) { wallpaperColorSchemesFor(context) }
}

fun wallpaperColorSchemesFor(context: Context): ThemeColorSchemes? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    return ThemeColorSchemes(
        light = emberrColorRolesFrom(dynamicLightColorScheme(context)),
        dark = emberrColorRolesFrom(dynamicDarkColorScheme(context))
    )
}

private fun emberrColorRolesFrom(wallpaperColors: ColorScheme): ColorScheme = wallpaperColors.copy(
    primary = wallpaperColors.onSurface,
    onPrimary = wallpaperColors.surface,
    secondary = wallpaperColors.primary,
    onSecondary = wallpaperColors.onPrimary,
    background = wallpaperColors.surface,
    onBackground = wallpaperColors.onSurface,
    surface = wallpaperColors.surfaceContainer,
    onSurface = wallpaperColors.onSurface,
    surfaceVariant = wallpaperColors.surfaceContainerHigh,
    surfaceContainerHigh = wallpaperColors.surfaceContainerHighest
)
