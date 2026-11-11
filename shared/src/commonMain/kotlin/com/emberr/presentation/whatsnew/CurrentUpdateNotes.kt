package com.emberr.presentation.whatsnew

import androidx.compose.runtime.Composable
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.isWindowsDesktop
import emberr.shared.generated.resources.Res
import org.jetbrains.compose.resources.StringResource
import emberr.shared.generated.resources.whatsnew_editor_description
import emberr.shared.generated.resources.whatsnew_editor_heading
import emberr.shared.generated.resources.whatsnew_editor_where
import emberr.shared.generated.resources.whatsnew_others_heading
import emberr.shared.generated.resources.whatsnew_others_sliders
import emberr.shared.generated.resources.whatsnew_others_small_fixes
import emberr.shared.generated.resources.whatsnew_others_sync_network
import emberr.shared.generated.resources.whatsnew_others_title_time_desktop
import emberr.shared.generated.resources.whatsnew_others_title_time_mobile
import emberr.shared.generated.resources.whatsnew_others_where
import emberr.shared.generated.resources.whatsnew_others_windows_startup
import emberr.shared.generated.resources.whatsnew_scale_description_desktop
import emberr.shared.generated.resources.whatsnew_scale_description_mobile
import emberr.shared.generated.resources.whatsnew_scale_heading
import emberr.shared.generated.resources.whatsnew_scale_where
import emberr.shared.generated.resources.whatsnew_texture_description
import emberr.shared.generated.resources.whatsnew_texture_heading
import emberr.shared.generated.resources.whatsnew_texture_where
import emberr.shared.generated.resources.whatsnew_themes_description_desktop
import emberr.shared.generated.resources.whatsnew_themes_description_mobile
import emberr.shared.generated.resources.whatsnew_themes_heading
import emberr.shared.generated.resources.whatsnew_themes_where
import emberr.shared.generated.resources.whatsnew_widgets_description
import emberr.shared.generated.resources.whatsnew_widgets_heading
import emberr.shared.generated.resources.whatsnew_widgets_where
import com.emberr.domain.util.system.isLinuxDesktop
import emberr.shared.generated.resources.whatsnew_languages_description_desktop
import emberr.shared.generated.resources.whatsnew_languages_description_mobile
import emberr.shared.generated.resources.whatsnew_languages_heading
import emberr.shared.generated.resources.whatsnew_languages_where
import emberr.shared.generated.resources.whatsnew_others_linux_restart
import emberr.shared.generated.resources.whatsnew_others_windows_update_restart

internal class UpdateNotesSection(
    val heading: StringResource,
    val whereToFind: StringResource? = null,
    val description: StringResource? = null,
    val items: List<StringResource> = emptyList(),
    val isNew: Boolean = false,
    val screenshotPaths: List<String> = emptyList(),
    val preview: (@Composable () -> Unit)? = null
)

internal val CurrentUpdateNotes = listOfNotNull(
    UpdateNotesSection(
        heading = Res.string.whatsnew_languages_heading,
        whereToFind = Res.string.whatsnew_languages_where,
        isNew = true,
        preview = { LanguagesUpdatePreview() },
        description = if (isDesktopPlatform) {
            Res.string.whatsnew_languages_description_desktop
        } else {
            Res.string.whatsnew_languages_description_mobile
        }
    ),
    UpdateNotesSection(
        heading = Res.string.whatsnew_themes_heading,
        whereToFind = Res.string.whatsnew_themes_where,
        isNew = true,
        preview = { ThemesUpdatePreview() },
        description = if (isDesktopPlatform) {
            Res.string.whatsnew_themes_description_desktop
        } else {
            Res.string.whatsnew_themes_description_mobile
        }
    ),
    UpdateNotesSection(
        heading = Res.string.whatsnew_texture_heading,
        whereToFind = Res.string.whatsnew_texture_where,
        isNew = true,
        preview = { PaperTextureUpdatePreview() },
        description = Res.string.whatsnew_texture_description
    ),
    UpdateNotesSection(
        heading = Res.string.whatsnew_scale_heading,
        whereToFind = Res.string.whatsnew_scale_where,
        isNew = true,
        preview = { InterfaceScaleUpdatePreview() },
        description = if (isDesktopPlatform) {
            Res.string.whatsnew_scale_description_desktop
        } else {
            Res.string.whatsnew_scale_description_mobile
        }
    ),
    if (isDesktopPlatform) {
        null
    } else {
        UpdateNotesSection(
            heading = Res.string.whatsnew_widgets_heading,
            whereToFind = Res.string.whatsnew_widgets_where,
            preview = { WidgetsUpdatePreview() },
            description = Res.string.whatsnew_widgets_description
        )
    },
    UpdateNotesSection(
        heading = Res.string.whatsnew_editor_heading,
        whereToFind = Res.string.whatsnew_editor_where,
        preview = { SlashMenuUpdatePreview() },
        description = Res.string.whatsnew_editor_description
    ),
    UpdateNotesSection(
        heading = Res.string.whatsnew_others_heading,
        whereToFind = Res.string.whatsnew_others_where,
        items = listOfNotNull(
            Res.string.whatsnew_others_sliders,
            if (isDesktopPlatform) {
                Res.string.whatsnew_others_title_time_desktop
            } else {
                Res.string.whatsnew_others_title_time_mobile
            },
            if (isDesktopPlatform) null else Res.string.whatsnew_others_sync_network,
            if (isWindowsDesktop) Res.string.whatsnew_others_windows_startup else null,
            if (isWindowsDesktop) Res.string.whatsnew_others_windows_update_restart else null,
            if (isLinuxDesktop) Res.string.whatsnew_others_linux_restart else null,
            Res.string.whatsnew_others_small_fixes
        )
    )
)
