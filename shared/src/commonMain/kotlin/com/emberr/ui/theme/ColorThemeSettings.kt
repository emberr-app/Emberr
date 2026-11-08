package com.emberr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.emberr.data.local.prefs.SettingsManager

data class ColorThemeSettings(
    val colorTheme: ColorThemePreference = ColorThemePreference.EMBERR,
    val themedTextAndIcons: Boolean = true,
    val colorThemeStrength: Int = FULL_STRENGTH_PERCENT,
    val paperTextureStrength: Int = 0,
    val paperTextureAreas: Set<PaperTextureArea> = setOf(PaperTextureArea.WHOLE_APP)
)

@Composable
fun SettingsManager.collectColorThemeSettings(): ColorThemeSettings {
    val colorThemeName by colorThemePreferenceFlow.collectAsState(initial = getColorThemePreference())
    val themedTextAndIcons by themedTextAndIconsFlow.collectAsState(initial = isThemedTextAndIconsEnabled())
    val colorThemeStrength by colorThemeStrengthFlow.collectAsState(initial = getColorThemeStrength())
    val paperTextureEnabled by paperTextureEnabledFlow.collectAsState(initial = isPaperTextureEnabled())
    val paperTextureStrength by paperTextureStrengthFlow.collectAsState(initial = getPaperTextureStrength())
    val savedPaperTextureAreas by paperTextureAreasFlow.collectAsState(initial = getPaperTextureAreas())

    return ColorThemeSettings(
        colorTheme = colorThemePreferenceNamed(colorThemeName),
        themedTextAndIcons = themedTextAndIcons,
        colorThemeStrength = colorThemeStrength,
        paperTextureStrength = if (paperTextureEnabled) paperTextureStrength else 0,
        paperTextureAreas = paperTextureAreasNamed(savedPaperTextureAreas)
    )
}
