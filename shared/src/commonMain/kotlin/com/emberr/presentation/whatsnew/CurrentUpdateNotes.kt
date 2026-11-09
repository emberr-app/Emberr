package com.emberr.presentation.whatsnew

import androidx.compose.runtime.Composable
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.isWindowsDesktop

internal class UpdateNotesSection(
    val heading: String,
    val whereToFind: String? = null,
    val description: String? = null,
    val items: List<String> = emptyList(),
    val isNew: Boolean = false,
    val screenshotPaths: List<String> = emptyList(),
    val preview: (@Composable () -> Unit)? = null
)

internal val CurrentUpdateNotes = listOfNotNull(
    UpdateNotesSection(
        heading = "Themes",
        whereToFind = "Settings → Appearance → Theme",
        isNew = true,
        preview = { ThemesUpdatePreview() },
        description = if (isDesktopPlatform) {
            "Pick from 13 color themes, like Nord, Catppuccin, Tokyo Night, Gruvbox, Rosé Pine and High Contrast. " +
                "Each one has a strength setting, and you can choose whether text and icons follow it too."
        } else {
            "Pick from 14 color themes, like Nord, Catppuccin, Tokyo Night, Gruvbox, Rosé Pine and High Contrast, " +
                "or let **Wallpaper** take its colors from your wallpaper. " +
                "Each one has a strength setting, and you can choose whether text and icons follow it too. " +
                "Your home screen widgets follow the theme as well."
        }
    ),
    UpdateNotesSection(
        heading = "Paper Texture",
        whereToFind = "Settings → Appearance → Theme → Texture",
        isNew = true,
        preview = { PaperTextureUpdatePreview() },
        description = "Give the app a soft grain, like real paper. " +
            "Set how strong it is, and choose where it shows: the whole app, notes, canvas or Daily Notes."
    ),
    UpdateNotesSection(
        heading = "Interface Scale",
        whereToFind = "Settings → Appearance → Interface Scale",
        isNew = true,
        preview = { InterfaceScaleUpdatePreview() },
        description = if (isDesktopPlatform) {
            "Make everything in the app bigger or smaller, from 80% to 200%, or use **Ctrl +**, **Ctrl −** and **Ctrl 0**."
        } else {
            "Make everything in the app bigger or smaller, from 80% to 200%."
        }
    ),
    if (isDesktopPlatform) {
        null
    } else {
        UpdateNotesSection(
            heading = "Widgets",
            whereToFind = "Long-press your home screen → Widgets → Emberr",
            preview = { WidgetsUpdatePreview() },
            description = "Every widget has a cleaner new look, and there are three new ones: **Note card**, **Jot it down** " +
                "and **Quote of the day**. Widgets now show the note you picked right away, and tell you when that note has been deleted."
        )
    },
    UpdateNotesSection(
        heading = "Editor",
        whereToFind = "Type / anywhere in a note",
        preview = { SlashMenuUpdatePreview() },
        description = "The slash menu now opens right after any character, not only after a space. " +
            "If you type past a match, delete back and the menu comes back."
    ),
    UpdateNotesSection(
        heading = "Others",
        whereToFind = "Smaller changes and fixes",
        items = listOfNotNull(
            "Sliders across the app have a new design.",
            if (isDesktopPlatform) {
                "Type **/** in a note's title to add the current time or date."
            } else {
                "Add the current time or date to a note's title from the keyboard toolbar."
            },
            if (isDesktopPlatform) null else "The network choice for sync is now in **Settings → Sync & Backup**.",
            if (isWindowsDesktop) "Faster startup on Windows." else null,
            "Plenty of small fixes."
        )
    )
)
