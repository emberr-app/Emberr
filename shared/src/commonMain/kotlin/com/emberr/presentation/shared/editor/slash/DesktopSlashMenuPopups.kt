package com.emberr.presentation.shared.editor.slash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import kotlin.math.roundToInt
import com.emberr.presentation.shared.editor.links.CanvasLinkMenu
import com.emberr.presentation.shared.editor.links.DatabaseLinkMenu
import com.emberr.presentation.shared.editor.links.NoteLinkMenu
import com.emberr.presentation.shared.editor.EditorActions

@Composable
internal fun rememberSlashMenuSections(
    isSlashMenuActiveHere: Boolean,
    slashQuery: String,
    customProperties: List<CustomPropertyEntity>,
    actions: EditorActions,
    onDismissSlashMenu: () -> Unit,
    onChooseHighlightColor: () -> Unit
): List<SlashMenuSectionData> {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val slashMenuLabels = rememberSlashMenuLabels()
    return remember(slashQuery, isSlashMenuActiveHere, customProperties, usesTwentyFourHourClock, slashMenuLabels) {
        if (isSlashMenuActiveHere) {
            filteredSlashMenuSections(
                labels = slashMenuLabels,
                query = slashQuery,
                onChangeBlockType = { actions.onChangeBlockType(it) },
                onToggleFormat = { format ->
                    if (format == "highlight") {
                        onDismissSlashMenu()
                        onChooseHighlightColor()
                    } else {
                        actions.onToggleFormat(format)
                    }
                },
                onAdjustIndentation = { actions.onAdjustIndentation(it) },
                onSetAlignment = { actions.onSetBlockAlignment(it) },
                onInsertMediaBlock = { actions.onInsertMediaBlock(it) },
                onInsertText = { actions.onReplaceSlashQuery("$it ") },
                usesTwentyFourHourClock = usesTwentyFourHourClock,
                customProperties = customProperties
            )
        } else {
            emptyList()
        }
    }
}

@Composable
internal fun DesktopSlashMenuPopups(
    anchorRect: Rect,
    isSlashMenuActiveHere: Boolean,
    slashMenuSections: List<SlashMenuSectionData>,
    slashMenuSelectedIndex: Int,
    onDismissSlashMenu: () -> Unit,
    isNoteLinkMenuActiveHere: Boolean,
    onDismissNoteLinkMenu: () -> Unit,
    isCanvasLinkMenuActiveHere: Boolean,
    onDismissCanvasLinkMenu: () -> Unit,
    isDatabaseLinkMenuActiveHere: Boolean,
    onDismissDatabaseLinkMenu: () -> Unit,
    onLinkMenuBack: (() -> Unit)?,
    allLinkableNotes: List<NoteMetadataEntity>,
    actions: EditorActions
) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(anchorRect.left.roundToInt(), anchorRect.top.roundToInt()) }
            .size(width = 1.dp, height = with(density) { anchorRect.height.toDp() })
    ) {
        if (isSlashMenuActiveHere) {
            EmberrDesktopMenu(
                expanded = true,
                onDismissRequest = onDismissSlashMenu,
                properties = PopupProperties(focusable = false),
                modifier = Modifier
                    .width(290.dp)
                    .heightIn(max = 400.dp)
            ) {
                SlashMenuList(
                    sections = slashMenuSections,
                    selectedIndex = slashMenuSelectedIndex
                )
            }
        }

        if (isNoteLinkMenuActiveHere) {
            NoteLinkMenu(
                expanded = true,
                onDismissRequest = onDismissNoteLinkMenu,
                onBack = onLinkMenuBack?.let { goBack -> { onDismissNoteLinkMenu(); goBack() } },
                allLinkableNotes = allLinkableNotes,
                onNoteSelected = { noteId ->
                    actions.onInsertLinkedNoteBlock(noteId)
                    onDismissNoteLinkMenu()
                },
                onCreateNote = { title ->
                    val newNoteId = actions.onCreateLinkedNote(title)
                    actions.onInsertLinkedNoteBlock(newNoteId)
                    onDismissNoteLinkMenu()
                },
                onCreateBlankNote = {
                    val newNoteId = actions.onCreateLinkedNote("Untitled")
                    actions.onInsertLinkedNoteBlock(newNoteId)
                    onDismissNoteLinkMenu()
                    actions.onNoteLinkClick(newNoteId)
                }
            )
        }

        if (isCanvasLinkMenuActiveHere) {
            CanvasLinkMenu(
                expanded = true,
                onDismissRequest = onDismissCanvasLinkMenu,
                onBack = onLinkMenuBack?.let { goBack -> { onDismissCanvasLinkMenu(); goBack() } },
                loadCanvases = { actions.getLinkableCanvases() },
                onCanvasSelected = { canvasNoteId ->
                    actions.onInsertCanvasBlock(canvasNoteId)
                    onDismissCanvasLinkMenu()
                }
            )
        }

        if (isDatabaseLinkMenuActiveHere) {
            DatabaseLinkMenu(
                expanded = true,
                onDismissRequest = onDismissDatabaseLinkMenu,
                onBack = onLinkMenuBack?.let { goBack -> { onDismissDatabaseLinkMenu(); goBack() } },
                loadDatabases = { actions.getLinkableDatabases() },
                onDatabaseSelected = { databaseId ->
                    actions.onInsertLinkedDatabaseBlock(databaseId)
                    onDismissDatabaseLinkMenu()
                }
            )
        }
    }
}
