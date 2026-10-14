package com.emberr.presentation.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.NoteSearchResult
import com.emberr.presentation.home.RenameBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar_day
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.trash
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private const val EMPTY_MESSAGE_DELAY_MILLIS = 350L
private val ResultShape = RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)

/**
 * The shared result list behind cross-note search. Results arrive in two waves (title/snippet
 * hits first, block-content hits a moment later), so the "nothing found" message waits out a
 * short settle delay instead of flashing between those two emissions.
 */
@Composable
fun SearchResultsList(
    query: String,
    results: List<NoteSearchResult>,
    onNoteClick: (String) -> Unit,
    onDailyNoteClick: (String) -> Unit,
    onRenameNote: (noteId: String, newTitle: String) -> Unit,
    onSetNoteFavorite: (noteId: String, isFavorite: Boolean) -> Unit,
    onTrashNote: (noteId: String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    var hasSettledOnNoResults by remember { mutableStateOf(false) }
    var resultWithOptions by remember { mutableStateOf<NoteSearchResult?>(null) }
    var showOptionsSheet by remember { mutableStateOf(false) }
    var showRenameSheet by remember { mutableStateOf(false) }

    LaunchedEffect(query, results) {
        hasSettledOnNoResults = false
        if (results.isEmpty()) {
            delay(EMPTY_MESSAGE_DELAY_MILLIS)
            hasSettledOnNoResults = true
        }
    }

    Box(modifier = modifier) {
        when {
            query.isBlank() -> SearchMessage("Start typing to search titles, snippets, and note content.")
            results.isEmpty() -> if (hasSettledOnNoResults) SearchMessage("No matching notes found.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results, key = { it.note.noteId }) { result ->
                    SearchResultRow(
                        result = result,
                        query = query,
                        onClick = {
                            if (result.note.isDaily) {
                                result.note.dateString?.let(onDailyNoteClick)
                            } else {
                                onNoteClick(result.note.noteId)
                            }
                        },
                        onLongClick = if (result.canBeRenamed || result.canBeFavoritedOrTrashed) {
                            {
                                resultWithOptions = result
                                showOptionsSheet = true
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    resultWithOptions?.let { result ->
        SearchResultOptionsSheet(
            expanded = showOptionsSheet,
            result = result,
            onDismiss = { showOptionsSheet = false },
            onOpenRename = { showRenameSheet = true },
            onToggleFavorite = { onSetNoteFavorite(result.note.noteId, !result.note.isFavorite) },
            onTrash = { onTrashNote(result.note.noteId) }
        )
        RenameBottomSheet(
            expanded = showRenameSheet,
            currentName = result.note.title,
            onDismiss = { showRenameSheet = false },
            onRename = { newTitle ->
                onRenameNote(result.note.noteId, newTitle)
                showRenameSheet = false
            },
            title = "Rename Note",
            placeholder = "Note title..."
        )
    }
}

@Composable
private fun SearchResultOptionsSheet(
    expanded: Boolean,
    result: NoteSearchResult,
    onDismiss: () -> Unit,
    onOpenRename: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTrash: () -> Unit
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = result.note.title.ifBlank { "Untitled" }
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            if (result.canBeRenamed) {
                EmberrBottomSheetOption(
                    label = "Rename",
                    onClick = { closeAnd(onOpenRename) },
                    icon = { SearchResultOptionIcon(painterResource(Res.drawable.pen), MaterialTheme.colorScheme.onSurface) },
                    outerHorizontalPadding = 0.dp
                )
            }
            if (result.canBeFavoritedOrTrashed) {
                EmberrBottomSheetOption(
                    label = result.favoriteOptionLabel,
                    onClick = { closeAnd(onToggleFavorite) },
                    icon = {
                        SearchResultOptionIcon(
                            rememberVectorPainter(if (result.note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder),
                            MaterialTheme.colorScheme.onSurface
                        )
                    },
                    outerHorizontalPadding = 0.dp
                )
                EmberrBottomSheetOption(
                    label = "Move to Trash",
                    onClick = { closeAnd(onTrash) },
                    icon = { SearchResultOptionIcon(painterResource(Res.drawable.trash), MaterialTheme.colorScheme.error) },
                    outerHorizontalPadding = 0.dp,
                    labelColor = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SearchResultOptionIcon(icon: Painter, tint: Color) {
    Icon(painter = icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

@Composable
private fun SearchMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    result: NoteSearchResult,
    query: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val note: NoteMetadataEntity = result.note
    val highlightStyle = defaultHighlightStyle(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))

    // A daily note is stored with the title "Daily: <date>", which the calendar chip below already
    // says - so the title line shows the bare date and the chip carries the "Daily" part.
    val dailyDateString = note.dateString?.takeIf { note.isDaily }
    val titleText = dailyDateString ?: note.title.ifBlank { "Untitled" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ResultShape)
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = highlightMatches(titleText, query, highlightStyle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (result.matchedText.isNotBlank() && result.matchedText != note.title) {
            Text(
                text = highlightMatches(result.matchedText, query, highlightStyle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        result.parentTitle?.let { parentTitle ->
            Text(
                text = "in $parentTitle",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (dailyDateString != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.calendar_day),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Daily",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
