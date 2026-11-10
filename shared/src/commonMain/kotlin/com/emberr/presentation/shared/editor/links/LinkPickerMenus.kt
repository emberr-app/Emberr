package com.emberr.presentation.shared.editor.links

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.LinkableDatabase
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrControlContainerColor
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.rememberNoteTitleNames
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.doc_text_plus
import emberr.shared.generated.resources.database
import emberr.shared.generated.resources.group
import org.jetbrains.compose.resources.painterResource
import com.emberr.presentation.shared.editor.slash.SlashMenuIcon
import com.emberr.presentation.shared.editor.slash.SlashMenuItemData
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.editor_back_to_commands
import emberr.shared.generated.resources.editor_block_link_to_canvas
import emberr.shared.generated.resources.editor_block_link_to_database
import emberr.shared.generated.resources.editor_block_link_to_note
import emberr.shared.generated.resources.editor_create_new_note
import emberr.shared.generated.resources.editor_database_in_note
import emberr.shared.generated.resources.editor_new_note_named
import emberr.shared.generated.resources.editor_search_canvases
import emberr.shared.generated.resources.editor_search_databases
import emberr.shared.generated.resources.editor_search_notes
import emberr.shared.generated.resources.editor_untitled_canvas
import emberr.shared.generated.resources.editor_untitled_database
import org.jetbrains.compose.resources.stringResource

private val LinkPanelHeight = 300.dp

private val DesktopLinkListMaxHeight = 280.dp
private val DesktopLinkBackButtonSize = 56.dp
private val DesktopLinkBackButtonShape = RoundedCornerShape(12.dp)

private val LinkSearchBarPadding = 8.dp
private val LinkSearchBarHeight = 50.dp
private val LinkSearchBarSpace = LinkSearchBarHeight + LinkSearchBarPadding * 2

@Composable
fun NoteLinkMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    allLinkableNotes: List<NoteMetadataEntity>,
    onNoteSelected: (noteId: String) -> Unit,
    onCreateNote: (title: String) -> Unit,
    onCreateBlankNote: () -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (!expanded) return

    EmberrDesktopMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        modifier = Modifier
            .width(290.dp)
            .heightIn(max = 400.dp)
    ) {
        NoteLinkMenuContent(
            allLinkableNotes = allLinkableNotes,
            onNoteSelected = onNoteSelected,
            onCreateNote = onCreateNote,
            onCreateBlankNote = onCreateBlankNote,
            onDismissRequest = onDismissRequest,
            onBack = onBack
        )
    }
}

@Composable
fun NoteLinkMenuContent(
    allLinkableNotes: List<NoteMetadataEntity>,
    onNoteSelected: (noteId: String) -> Unit,
    onCreateNote: (title: String) -> Unit,
    onCreateBlankNote: () -> Unit,
    onDismissRequest: () -> Unit = {},
    autoFocusSearch: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }

    val filteredNotes = remember(query, allLinkableNotes) {
        allLinkableNotes.filter { it.kind == NoteKind.NOTE && it.title.contains(query, ignoreCase = true) }
    }

    val createNewNoteLabel = stringResource(Res.string.editor_create_new_note)
    val noteTitleNames = rememberNoteTitleNames()
    val newNoteForQueryLabel = stringResource(Res.string.editor_new_note_named, query)
    val entries = remember(query, filteredNotes, createNewNoteLabel, noteTitleNames, newNoteForQueryLabel) {
        buildList {
            add(
                SlashMenuItemData(createNewNoteLabel, Res.drawable.doc_text_plus) {
                    onCreateBlankNote()
                }
            )
            filteredNotes.forEach { note ->
                val icon = note.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Drawable(Res.drawable.doc_text)
                add(SlashMenuItemData(noteTitleNames.shown(note.title), icon) { onNoteSelected(note.noteId) })
            }
            if (query.isNotBlank()) {
                add(
                    SlashMenuItemData(newNoteForQueryLabel, Res.drawable.doc_text_plus) {
                        onCreateNote(query.trim())
                    }
                )
            }
        }
    }

    SearchableLinkList(
        query = query,
        onQueryChange = { query = it },
        searchPlaceholder = stringResource(Res.string.editor_search_notes),
        sectionTitle = stringResource(Res.string.editor_block_link_to_note),
        entries = entries,
        onDismissRequest = onDismissRequest,
        autoFocusSearch = autoFocusSearch,
        onBack = onBack
    )
}

@Composable
fun CanvasLinkMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    loadCanvases: suspend () -> List<NoteMetadataEntity>,
    onCanvasSelected: (canvasNoteId: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (!expanded) return

    EmberrDesktopMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        modifier = Modifier
            .width(290.dp)
            .heightIn(max = 400.dp)
    ) {
        CanvasLinkMenuContent(
            loadCanvases = loadCanvases,
            onCanvasSelected = onCanvasSelected,
            onDismissRequest = onDismissRequest,
            onBack = onBack
        )
    }
}

@Composable
fun CanvasLinkMenuContent(
    loadCanvases: suspend () -> List<NoteMetadataEntity>,
    onCanvasSelected: (canvasNoteId: String) -> Unit,
    onDismissRequest: () -> Unit = {},
    autoFocusSearch: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    val canvases by produceState(initialValue = emptyList<NoteMetadataEntity>()) { value = loadCanvases() }

    val untitledCanvasLabel = stringResource(Res.string.editor_untitled_canvas)
    val entries = remember(query, canvases, untitledCanvasLabel) {
        canvases
            .map { canvas -> canvas.noteId to canvas.title.ifBlank { untitledCanvasLabel } }
            .filter { (_, title) -> title.contains(query, ignoreCase = true) }
            .map { (canvasNoteId, title) -> SlashMenuItemData(title, Res.drawable.group) { onCanvasSelected(canvasNoteId) } }
    }

    SearchableLinkList(
        query = query,
        onQueryChange = { query = it },
        searchPlaceholder = stringResource(Res.string.editor_search_canvases),
        sectionTitle = stringResource(Res.string.editor_block_link_to_canvas),
        entries = entries,
        onDismissRequest = onDismissRequest,
        autoFocusSearch = autoFocusSearch,
        onBack = onBack
    )
}

@Composable
fun DatabaseLinkMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    loadDatabases: suspend () -> List<LinkableDatabase>,
    onDatabaseSelected: (databaseId: String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    if (!expanded) return

    EmberrDesktopMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        modifier = Modifier
            .width(290.dp)
            .heightIn(max = 400.dp)
    ) {
        DatabaseLinkMenuContent(
            loadDatabases = loadDatabases,
            onDatabaseSelected = onDatabaseSelected,
            onDismissRequest = onDismissRequest,
            onBack = onBack
        )
    }
}

@Composable
fun DatabaseLinkMenuContent(
    loadDatabases: suspend () -> List<LinkableDatabase>,
    onDatabaseSelected: (databaseId: String) -> Unit,
    onDismissRequest: () -> Unit = {},
    autoFocusSearch: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    val databases by produceState(initialValue = emptyList<LinkableDatabase>()) { value = loadDatabases() }

    val untitledDatabaseLabel = stringResource(Res.string.editor_untitled_database)
    val noteTitleNames = rememberNoteTitleNames()
    val databaseLabels = databases.map { database ->
        val databaseTitle = database.title.ifBlank { untitledDatabaseLabel }
        database.databaseId to stringResource(Res.string.editor_database_in_note, databaseTitle, noteTitleNames.shown(database.noteTitle))
    }
    val entries = remember(query, databaseLabels) {
        databaseLabels
            .filter { (_, label) -> label.contains(query, ignoreCase = true) }
            .map { (databaseId, label) -> SlashMenuItemData(label, Res.drawable.database) { onDatabaseSelected(databaseId) } }
    }

    SearchableLinkList(
        query = query,
        onQueryChange = { query = it },
        searchPlaceholder = stringResource(Res.string.editor_search_databases),
        sectionTitle = stringResource(Res.string.editor_block_link_to_database),
        entries = entries,
        onDismissRequest = onDismissRequest,
        autoFocusSearch = autoFocusSearch,
        onBack = onBack
    )
}

@Composable
private fun SearchableLinkList(
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    sectionTitle: String,
    entries: List<SlashMenuItemData>,
    onDismissRequest: () -> Unit,
    autoFocusSearch: Boolean,
    onBack: (() -> Unit)?
) {
    if (!isDesktopPlatform) {
        val linkListHazeState = remember { HazeState() }
        Box(modifier = Modifier.fillMaxWidth().height(LinkPanelHeight)) {
            Box(modifier = Modifier.fillMaxSize().hazeSource(linkListHazeState)) {
                SlashMenuList(
                    sections = listOf(SlashMenuSectionData(sectionTitle, entries)),
                    maxHeight = LinkPanelHeight,
                    bottomPadding = LinkSearchBarSpace
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(LinkSearchBarPadding),
                horizontalArrangement = Arrangement.spacedBy(LinkSearchBarPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    TopBarIconButton(
                        icon = painterResource(Res.drawable.chevron_left),
                        contentDescription = stringResource(Res.string.common_back),
                        bgColor = Color.Transparent,
                        tint = MaterialTheme.colorScheme.primary,
                        hazeState = linkListHazeState,
                        size = LinkSearchBarHeight,
                        onClick = onBack
                    )
                }
                EmberrSearchTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = searchPlaceholder.removeSuffix("..."),
                    hazeState = linkListHazeState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    } else {
        var selectedIndex by remember(query) { mutableIntStateOf(0) }
        val searchFocusRequester = remember { FocusRequester() }
        LaunchedEffect(autoFocusSearch) {
            if (autoFocusSearch) searchFocusRequester.requestFocus()
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(LinkSearchBarPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconTooltip(text = stringResource(Res.string.editor_back_to_commands)) {
                    Box(
                        modifier = Modifier
                            .size(DesktopLinkBackButtonSize)
                            .clip(DesktopLinkBackButtonShape)
                            .background(emberrControlContainerColor())
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.chevron_left),
                            contentDescription = stringResource(Res.string.common_back),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            EmberrTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = searchPlaceholder,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (entries.isNotEmpty()) selectedIndex = (selectedIndex + 1) % entries.size
                                true
                            }
                            Key.DirectionUp -> {
                                if (entries.isNotEmpty()) selectedIndex = (selectedIndex - 1 + entries.size) % entries.size
                                true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                entries.getOrNull(selectedIndex)?.action?.invoke()
                                true
                            }
                            Key.Escape -> {
                                onDismissRequest()
                                true
                            }
                            else -> false
                        }
                    }
            )
        }
        SlashMenuList(
            sections = listOf(SlashMenuSectionData(sectionTitle, entries)),
            selectedIndex = selectedIndex,
            maxHeight = DesktopLinkListMaxHeight
        )
    }
}
