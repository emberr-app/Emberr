package com.emberr.ui.theme

import androidx.compose.runtime.Composable

@Composable
expect fun rememberWallpaperColorSchemes(): ThemeColorSchemes?

@Composable
fun ColorThemePreference.sourceColorSchemes(): ThemeColorSchemes {
    val wallpaperColorSchemes = if (this == ColorThemePreference.WALLPAPER) rememberWallpaperColorSchemes() else null
    return wallpaperColorSchemes ?: builtInColorSchemes
}

@Composable
fun availableColorThemes(): List<ColorThemePreference> {
    val supportsWallpaperColors = rememberWallpaperColorSchemes() != null
    return ColorThemePreference.entries.filter { colorTheme ->
        supportsWallpaperColors || colorTheme != ColorThemePreference.WALLPAPER
    }
}
