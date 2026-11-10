package com.emberr.presentation.settings

import androidx.compose.runtime.Composable
import com.emberr.domain.util.time.FirstDayOfWeekPreference
import com.emberr.domain.util.time.TimeFormatPreference
import com.emberr.presentation.shared.SubNoteOpenMode
import com.emberr.presentation.shared.TopBarFadeStyle
import com.emberr.ui.theme.ColorThemePreference
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.PaperTextureArea
import com.emberr.ui.theme.ThemePreference
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.settings_color_theme_catppuccin_frappe
import emberr.shared.generated.resources.settings_color_theme_catppuccin_mocha
import emberr.shared.generated.resources.settings_color_theme_emberr
import emberr.shared.generated.resources.settings_color_theme_everforest
import emberr.shared.generated.resources.settings_color_theme_forest
import emberr.shared.generated.resources.settings_color_theme_gruvbox
import emberr.shared.generated.resources.settings_color_theme_high_contrast
import emberr.shared.generated.resources.settings_color_theme_nord
import emberr.shared.generated.resources.settings_color_theme_paper
import emberr.shared.generated.resources.settings_color_theme_rose_pine
import emberr.shared.generated.resources.settings_color_theme_soft
import emberr.shared.generated.resources.settings_color_theme_terracotta
import emberr.shared.generated.resources.settings_color_theme_tokyo_night
import emberr.shared.generated.resources.settings_color_theme_wallpaper
import emberr.shared.generated.resources.settings_first_day_of_week_monday
import emberr.shared.generated.resources.settings_first_day_of_week_saturday
import emberr.shared.generated.resources.settings_first_day_of_week_sunday
import emberr.shared.generated.resources.settings_first_day_of_week_system
import emberr.shared.generated.resources.settings_font_style_system
import emberr.shared.generated.resources.settings_paper_texture_area_canvas
import emberr.shared.generated.resources.settings_paper_texture_area_daily_notes
import emberr.shared.generated.resources.settings_paper_texture_area_notes
import emberr.shared.generated.resources.settings_paper_texture_area_whole_app
import emberr.shared.generated.resources.settings_paper_texture_areas_none
import emberr.shared.generated.resources.settings_subnote_opening_center_dialog
import emberr.shared.generated.resources.settings_subnote_opening_full_right_panel
import emberr.shared.generated.resources.settings_subnote_opening_side_panel
import emberr.shared.generated.resources.settings_theme_mode_dark
import emberr.shared.generated.resources.settings_theme_mode_light
import emberr.shared.generated.resources.settings_theme_mode_system
import emberr.shared.generated.resources.settings_time_format_system
import emberr.shared.generated.resources.settings_time_format_twelve_hour
import emberr.shared.generated.resources.settings_time_format_twenty_four_hour
import emberr.shared.generated.resources.settings_top_bar_fade_blur
import emberr.shared.generated.resources.settings_top_bar_fade_color
import emberr.shared.generated.resources.settings_top_bar_fade_fallback
import emberr.shared.generated.resources.settings_top_bar_fade_none
import emberr.shared.generated.resources.settings_top_bar_fade_progressive_blur
import emberr.shared.generated.resources.settings_top_bar_fade_progressive_color
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TimeFormatPreference.label(): String = stringResource(
    when (this) {
        TimeFormatPreference.SYSTEM -> Res.string.settings_time_format_system
        TimeFormatPreference.TWELVE_HOUR -> Res.string.settings_time_format_twelve_hour
        TimeFormatPreference.TWENTY_FOUR_HOUR -> Res.string.settings_time_format_twenty_four_hour
    }
)

@Composable
fun FirstDayOfWeekPreference.label(): String = stringResource(
    when (this) {
        FirstDayOfWeekPreference.SYSTEM -> Res.string.settings_first_day_of_week_system
        FirstDayOfWeekPreference.SATURDAY -> Res.string.settings_first_day_of_week_saturday
        FirstDayOfWeekPreference.SUNDAY -> Res.string.settings_first_day_of_week_sunday
        FirstDayOfWeekPreference.MONDAY -> Res.string.settings_first_day_of_week_monday
    }
)

@Composable
fun SubNoteOpenMode.label(): String = stringResource(
    when (this) {
        SubNoteOpenMode.SIDE_PANEL -> Res.string.settings_subnote_opening_side_panel
        SubNoteOpenMode.CENTER_DIALOG -> Res.string.settings_subnote_opening_center_dialog
        SubNoteOpenMode.FULL_RIGHT_PANEL -> Res.string.settings_subnote_opening_full_right_panel
    }
)

@Composable
fun TopBarFadeStyle.label(isProgressive: Boolean): String {
    val showsProgressiveName = isProgressive && !isAlwaysFlatOnThisPlatform
    return stringResource(
        when (this) {
            TopBarFadeStyle.BLUR ->
                if (showsProgressiveName) Res.string.settings_top_bar_fade_progressive_blur else Res.string.settings_top_bar_fade_blur
            TopBarFadeStyle.COLOR ->
                if (showsProgressiveName) Res.string.settings_top_bar_fade_progressive_color else Res.string.settings_top_bar_fade_color
            TopBarFadeStyle.NONE -> Res.string.settings_top_bar_fade_none
            TopBarFadeStyle.FALLBACK -> Res.string.settings_top_bar_fade_fallback
        }
    )
}

@Composable
fun FontStylePreference.label(): String =
    if (this == FontStylePreference.SYSTEM) stringResource(Res.string.settings_font_style_system) else displayName

@Composable
fun ThemePreference.label(): String = stringResource(
    when (this) {
        ThemePreference.SYSTEM -> Res.string.settings_theme_mode_system
        ThemePreference.LIGHT -> Res.string.settings_theme_mode_light
        ThemePreference.DARK -> Res.string.settings_theme_mode_dark
    }
)

@Composable
fun ColorThemePreference.label(): String = stringResource(
    when (this) {
        ColorThemePreference.EMBERR -> Res.string.settings_color_theme_emberr
        ColorThemePreference.SOFT -> Res.string.settings_color_theme_soft
        ColorThemePreference.WALLPAPER -> Res.string.settings_color_theme_wallpaper
        ColorThemePreference.HIGH_CONTRAST -> Res.string.settings_color_theme_high_contrast
        ColorThemePreference.TERRACOTTA -> Res.string.settings_color_theme_terracotta
        ColorThemePreference.PAPER -> Res.string.settings_color_theme_paper
        ColorThemePreference.NORD -> Res.string.settings_color_theme_nord
        ColorThemePreference.FOREST -> Res.string.settings_color_theme_forest
        ColorThemePreference.ROSE -> Res.string.settings_color_theme_rose_pine
        ColorThemePreference.CATPPUCCIN_FRAPPE -> Res.string.settings_color_theme_catppuccin_frappe
        ColorThemePreference.CATPPUCCIN -> Res.string.settings_color_theme_catppuccin_mocha
        ColorThemePreference.TOKYO_NIGHT -> Res.string.settings_color_theme_tokyo_night
        ColorThemePreference.GRUVBOX -> Res.string.settings_color_theme_gruvbox
        ColorThemePreference.EVERFOREST -> Res.string.settings_color_theme_everforest
    }
)

private fun PaperTextureArea.labelResource(): StringResource = when (this) {
    PaperTextureArea.WHOLE_APP -> Res.string.settings_paper_texture_area_whole_app
    PaperTextureArea.DAILY_NOTES -> Res.string.settings_paper_texture_area_daily_notes
    PaperTextureArea.NOTES -> Res.string.settings_paper_texture_area_notes
    PaperTextureArea.CANVAS -> Res.string.settings_paper_texture_area_canvas
}

@Composable
fun PaperTextureArea.label(): String = stringResource(labelResource())

@Composable
fun paperTextureAreasLabel(areas: Set<PaperTextureArea>): String {
    if (PaperTextureArea.WHOLE_APP in areas) return PaperTextureArea.WHOLE_APP.label()
    val checkedAreas = PaperTextureArea.entries.filter { it in areas }
    if (checkedAreas.isEmpty()) return stringResource(Res.string.settings_paper_texture_areas_none)
    return checkedAreas.map { area -> area.label() }.joinToString(", ")
}
