// The shared "choose a note" list that every widget setup screen shows.
package com.emberr.presentation.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.SettingsGroupTitle
import com.emberr.presentation.settings.SettingsRowIcon
import com.emberr.presentation.settings.settingsRowSize
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.shownNoteTitle
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.database
import emberr.shared.generated.resources.file_text
import emberr.shared.generated.resources.group
import emberr.shared.generated.resources.notes2
import emberr.shared.generated.resources.search
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.widgets_favorites
import emberr.shared.generated.resources.widgets_other
import emberr.shared.generated.resources.widgets_no_notes_yet
import emberr.shared.generated.resources.widgets_no_notes_match
import emberr.shared.generated.resources.widgets_choose_a_note
import emberr.shared.generated.resources.widgets_search_notes
import org.jetbrains.compose.resources.stringResource

private val noteGroupCornerRadius = 18.dp
private val noteIconSize = 20.dp
private val floatingSearchHeight = 50.dp
private val floatingSearchVerticalPadding = 14.dp

private data class WidgetNoteGroup(
    val title: String,
    val notes: List<SelectableWidgetNote>
)

@Composable
fun WidgetNoteChooser(
    notes: List<SelectableWidgetNote>,
    onNoteChosen: (String) -> Unit,
    onClose: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val headerHeight = with(density) { headerHeightPx.toDp() }
    var searchText by remember { mutableStateOf("") }

    val favoritesGroupTitle = stringResource(Res.string.widgets_favorites)
    val otherNotesGroupTitle = stringResource(Res.string.widgets_other)
    val noteGroups = remember(notes, searchText, favoritesGroupTitle, otherNotesGroupTitle) {
        groupNotesForChooser(notes, searchText, favoritesGroupTitle, otherNotesGroupTitle)
    }
    val keyboardOrNavigationBarHeight = WindowInsets.ime
        .union(WindowInsets.navigationBars)
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                top = headerHeight,
                bottom = keyboardOrNavigationBarHeight + floatingSearchHeight + floatingSearchVerticalPadding * 2 + 8.dp
            )
        ) {
            when {
                notes.isEmpty() -> item(key = "empty_notes") {
                    ChooserEmptyState(
                        icon = painterResource(Res.drawable.notes2),
                        message = stringResource(Res.string.widgets_no_notes_yet)
                    )
                }
                noteGroups.isEmpty() -> item(key = "empty_search") {
                    ChooserEmptyState(
                        icon = painterResource(Res.drawable.search),
                        message = stringResource(Res.string.widgets_no_notes_match, searchText.trim())
                    )
                }
                else -> noteGroups.forEachIndexed { groupIndex, group ->
                    noteGroupSection(groupIndex = groupIndex, group = group, onNoteChosen = onNoteChosen)
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> headerHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = stringResource(Res.string.widgets_choose_a_note),
                hazeState = hazeState,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                onBackClick = onClose
            )
        }

        EmberrSearchTextField(
            value = searchText,
            onValueChange = { newText -> searchText = newText },
            placeholder = stringResource(Res.string.widgets_search_notes),
            hazeState = hazeState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(20f)
                .imePadding()
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = floatingSearchVerticalPadding)
        )
    }
}

@Composable
fun WidgetSetupNotice(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

private fun groupNotesForChooser(
    notes: List<SelectableWidgetNote>,
    searchText: String,
    favoritesGroupTitle: String,
    otherNotesGroupTitle: String
): List<WidgetNoteGroup> {
    val query = searchText.trim()
    val matchingNotes = if (query.isEmpty()) {
        notes
    } else {
        notes.filter { entry -> entry.note.title.contains(query, ignoreCase = true) }
    }

    val (favoriteNotes, otherNotes) = matchingNotes.partition { entry -> entry.note.isFavorite }

    val spaceGroups = otherNotes
        .sortedBy { entry -> entry.spacePosition }
        .groupBy { entry -> entry.spaceName.trim().ifBlank { otherNotesGroupTitle } }
        .map { (title, groupNotes) -> WidgetNoteGroup(title = title, notes = groupNotes) }

    val favoritesGroup = WidgetNoteGroup(title = favoritesGroupTitle, notes = favoriteNotes)
    return (listOf(favoritesGroup) + spaceGroups).filter { group -> group.notes.isNotEmpty() }
}

private fun LazyListScope.noteGroupSection(
    groupIndex: Int,
    group: WidgetNoteGroup,
    onNoteChosen: (String) -> Unit
) {
    item(key = "group_title_$groupIndex") {
        Box(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 22.dp)) {
            SettingsGroupTitle(title = group.title)
        }
    }

    itemsIndexed(
        items = group.notes,
        key = { _, entry -> "group_${groupIndex}_${entry.note.noteId}" }
    ) { index, entry ->
        val isFirstInGroup = index == 0
        val isLastInGroup = index == group.notes.lastIndex
        val topCornerRadius = if (isFirstInGroup) noteGroupCornerRadius else 0.dp
        val bottomCornerRadius = if (isLastInGroup) noteGroupCornerRadius else 0.dp

        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = topCornerRadius,
                        topEnd = topCornerRadius,
                        bottomStart = bottomCornerRadius,
                        bottomEnd = bottomCornerRadius
                    )
                )
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (!isFirstInGroup) {
                SettingsDivider()
            }

            NoteChooserRow(
                note = entry.note,
                onClick = { onNoteChosen(entry.note.noteId) }
            )
        }
    }
}

@Composable
private fun NoteChooserRow(note: NoteMetadataEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .settingsRowSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NoteRowIcon(note = note)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shownNoteTitle(note.title.trim()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val snippet = note.snippet.trim()
            if (snippet.isNotEmpty()) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun NoteRowIcon(note: NoteMetadataEntity) {
    val emoji = note.icon?.trim()
    if (emoji.isNullOrEmpty()) {
        SettingsRowIcon(icon = painterResource(defaultIconFor(note.kind)))
    } else {
        Box(modifier = Modifier.size(noteIconSize), contentAlignment = Alignment.Center) {
            Text(text = emoji, fontSize = 17.sp, maxLines = 1)
        }
    }
}

private fun defaultIconFor(kind: NoteKind) = when (kind) {
    NoteKind.CANVAS -> Res.drawable.group
    NoteKind.DATABASE -> Res.drawable.database
    NoteKind.NOTE, NoteKind.PERSON -> Res.drawable.file_text
}

@Composable
private fun ChooserEmptyState(icon: Painter, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
    }
}
