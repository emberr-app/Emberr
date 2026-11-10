package com.emberr.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrTextContextMenu
import kotlinx.datetime.DayOfWeek

private const val TEXT_SELECTION_BACKGROUND_ALPHA = 0.4f

// Base color palette

val CharcoalNoir   = Color(0xFF0d0d0d)
val IroncladGrey   = Color(0xFF1E1E1E)
val UrbanFog       = Color(0xFF848484)
val CloudVeil      = Color(0xFFEFEFEF)
val HighlightLime      = Color(0xFFFFF176)
val HighlightLimeDark  = Color(0xFF6B5F10)
const val HighlightCellBackgroundHex = "#FDFFB6"
val TableGridLineLight = Color(0xFFDADADA)
val TableGridLineDark  = Color(0xFF3A3A3A)

val LocalAppIsDark = staticCompositionLocalOf { false }

val LocalColorThemePreference = staticCompositionLocalOf { ColorThemePreference.EMBERR }

val LocalUsesTwentyFourHourClock = staticCompositionLocalOf { false }

val LocalFirstDayOfWeek = staticCompositionLocalOf { DayOfWeek.SUNDAY }

val highlightBackgroundColor: Color
    @Composable get() = if (LocalAppIsDark.current) HighlightLimeDark else HighlightLime

val tableGridLineColor: Color
    @Composable get() = when {
        LocalColorThemePreference.current == ColorThemePreference.HIGH_CONTRAST -> MaterialTheme.colorScheme.outlineVariant
        LocalAppIsDark.current -> TableGridLineDark
        else -> TableGridLineLight
    }

enum class FontSizePreference { EXTRA_SMALL, SMALL, DEFAULT, LARGE, EXTRA_LARGE }
enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun EmberrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    fontSizePreference: FontSizePreference = FontSizePreference.DEFAULT,
    fontStylePreference: FontStylePreference = FontStylePreference.POPPINS,
    colorThemeSettings: ColorThemeSettings = ColorThemeSettings(),
    usesTwentyFourHourClock: Boolean = false,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
    interfaceScalePercent: Int = SyncConstants.DEFAULT_INTERFACE_SCALE_PERCENT,
    content: @Composable () -> Unit
) {
    val colorTheme = colorThemeSettings.colorTheme
    val colorScheme = colorTheme.colorSchemeFor(
        darkTheme = darkTheme,
        themedTextAndIcons = colorThemeSettings.themedTextAndIcons,
        strengthPercent = colorThemeSettings.colorThemeStrength,
        sourceColorSchemes = colorTheme.sourceColorSchemes()
    )

    val currentFontSizes = when {
        isDesktopPlatform -> when (fontSizePreference) {
            FontSizePreference.EXTRA_SMALL -> DesktopFontSizesExtraSmall
            FontSizePreference.SMALL -> DesktopFontSizesSmall
            FontSizePreference.DEFAULT -> DesktopFontSizesDefault
            FontSizePreference.LARGE -> DesktopFontSizesLarge
            FontSizePreference.EXTRA_LARGE -> DesktopFontSizesExtraLarge
        }
        else -> when (fontSizePreference) {
            FontSizePreference.EXTRA_SMALL -> MobileFontSizesExtraSmall
            FontSizePreference.SMALL -> MobileFontSizesSmall
            FontSizePreference.DEFAULT -> MobileFontSizesDefault
            FontSizePreference.LARGE -> MobileFontSizesLarge
            FontSizePreference.EXTRA_LARGE -> MobileFontSizesExtraLarge
        }
    }

    SetSystemBars(
        statusBarColor = Color.Transparent,
        darkIcons = !darkTheme
    )

    ProvideInterfaceScale(interfaceScalePercent) {
        CompositionLocalProvider(
            LocalAppIsDark provides darkTheme,
            LocalColorThemePreference provides colorTheme,
            LocalEmberrFontSizes provides currentFontSizes,
            LocalEmberrFontStyle provides fontStylePreference,
            LocalUsesTwentyFourHourClock provides usesTwentyFourHourClock,
            LocalFirstDayOfWeek provides firstDayOfWeek,
            LocalPaperTextureStrength provides colorThemeSettings.paperTextureStrength,
            LocalPaperTextureAreas provides colorThemeSettings.paperTextureAreas
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = AppTypography
            ) {
                val accentTextSelectionColors = TextSelectionColors(
                    handleColor = colorScheme.secondary,
                    backgroundColor = colorScheme.primary.copy(alpha = TEXT_SELECTION_BACKGROUND_ALPHA)
                )
                CompositionLocalProvider(LocalTextSelectionColors provides accentTextSelectionColors) {
                    EmberrTextContextMenu {
                        if (isDesktopPlatform) content() else PaperTextureBox(propagateMinConstraints = true) { content() }
                    }
                }
            }
        }
    }
}