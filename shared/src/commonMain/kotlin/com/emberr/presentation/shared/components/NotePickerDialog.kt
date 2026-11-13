package com.emberr.presentation.shared.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.system.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.components_create_new_note
import emberr.shared.generated.resources.components_link_to_note
import emberr.shared.generated.resources.components_link_to_note_heading
import emberr.shared.generated.resources.components_new_named_note
import emberr.shared.generated.resources.components_note_picker_hint
import emberr.shared.generated.resources.components_search_notes
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.doc_text_plus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val DesktopRowHorizontalPadding = 16.dp
private val SheetRowHorizontalPadding = 20.dp

@Composable
fun NotePickerDialog(
    expanded: Boolean,
    onDismiss: () -> Unit,
    allLinkableNotes: List<NoteMetadataEntity>,
    onNoteSelected: (noteId: String) -> Unit,
    onCreateNote: (title: String) -> Unit,
    onCreateBlankNote: () -> Unit
) {
    if (!expanded) return

    var query by remember { mutableStateOf("") }
    val filteredNotes = remember(query, allLinkableNotes) {
        allLinkableNotes.filter { it.title.contains(query, ignoreCase = true) }
    }

    // Every row action runs through this instead of firing its callback directly. On mobile it is
    // EmberrBottomSheet's closeAnd, which plays sheetState.hide() first and only then runs the action
    // and dismisses - without it the sheet is yanked out of composition the instant a row is tapped
    // (mentionQuery goes null -> the `if (!expanded) return` above trips) with no exit animation.
    // Running the action after the hide also stops the inserted link from appearing in the editor
    // underneath a sheet that is still on screen.
    val rowHorizontalPadding = if (isDesktopPlatform) DesktopRowHorizontalPadding else SheetRowHorizontalPadding

    val createBlankNoteRow: @Composable (closeAnd: (() -> Unit) -> Unit) -> Unit = { closeAnd ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { closeAnd { onCreateBlankNote() } }
                .padding(horizontal = rowHorizontalPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(Res.drawable.doc_text_plus), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(Res.string.components_create_new_note), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = rowHorizontalPadding, vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f).orHighContrastLine())
    }

    val listContent: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit = { closeAnd ->
        filteredNotes.forEach { note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { closeAnd { onNoteSelected(note.noteId) } }
                    .padding(horizontal = rowHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.icon != null) {
                    Text(note.icon, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Icon(painterResource(Res.drawable.doc_text), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = shownNoteTitle(note.title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (query.isNotBlank()) {
            if (filteredNotes.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = rowHorizontalPadding, vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f).orHighContrastLine())
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { closeAnd { onCreateNote(query.trim()) } }
                    .padding(horizontal = rowHorizontalPadding, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(Res.drawable.doc_text_plus), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(Res.string.components_new_named_note, query), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        } else if (filteredNotes.isEmpty()) {
            Text(
                text = stringResource(Res.string.components_note_picker_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = rowHorizontalPadding, vertical = 8.dp).padding(bottom = 8.dp)
            )
        }
    }

    if (isDesktopPlatform) {
        val positionProvider = remember {
            object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: androidx.compose.ui.unit.IntRect,
                    windowSize: IntSize,
                    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                    popupContentSize: IntSize
                ): androidx.compose.ui.unit.IntOffset {
                    val x = (windowSize.width - popupContentSize.width) / 2
                    val y = (windowSize.height - popupContentSize.height) / 2
                    return androidx.compose.ui.unit.IntOffset(x, y)
                }
            }
        }

        // A Popup has no exit animation to wait on, so the desktop closer is a straight pass-through.
        val immediateClose: ((() -> Unit) -> Unit) = { action -> action(); onDismiss() }

        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = EmberrShadowElevation.Standard,
                modifier = Modifier
                    .width(320.dp)
                    .heightIn(max = 360.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = stringResource(Res.string.components_link_to_note_heading),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    EmberrTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = stringResource(Res.string.components_search_notes),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = DesktopRowHorizontalPadding, vertical = 8.dp)
                    )
                    createBlankNoteRow(immediateClose)
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        listContent(immediateClose)
                    }
                }
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = onDismiss,
            title = stringResource(Res.string.components_link_to_note),
            contentHorizontalPadding = 0.dp,
            scrollsContent = false
        ) { closeAnd ->
            EmberrSearchSheetBody(
                searchText = query,
                onSearchTextChange = { query = it },
                placeholder = stringResource(Res.string.components_search_notes)
            ) {
                createBlankNoteRow(closeAnd)
                listContent(closeAnd)
            }
        }
    }
}