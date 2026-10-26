package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.titleKey
import com.emberr.domain.movie.boardStatuses
import com.emberr.domain.movie.openViewOn
import com.emberr.domain.movie.shownIn
import com.emberr.domain.movie.viewsInOrderOf
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.blockViews.rememberDeviceTableLayout
import com.emberr.presentation.shared.editor.hidesEditorToolbarWhileFocused
import kotlinx.coroutines.delay
import java.util.UUID

private const val NEW_TITLE_HIGHLIGHT_MILLIS = 1_200L
private const val TITLE_RESIZE_MILLIS = 260
private const val TITLE_FADE_MILLIS = 220
private const val TITLE_FADE_DELAY_MILLIS = 80

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WatchlistBlockView(
    block: WatchlistBlock,
    editor: WatchlistBlockEditor?,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit = { it() }
) {
    if (editor == null) return

    val titles by remember(block.watchlistNoteId) { editor.titlesOf(block.watchlistNoteId) }.collectAsState(initial = null)
    val addingToWatchlistIds by editor.addingToWatchlistIds.collectAsState()
    val lastAddedBlockId by editor.lastAddedBlockId.collectAsState()
    val movieEditor = remember(block.watchlistNoteId) { editor.movieEditorFor(block.watchlistNoteId) }
    var isAddingTitle by remember { mutableStateOf(false) }
    var isSearchSheetOpen by remember { mutableStateOf(false) }
    var searchFocusNonce by remember { mutableStateOf<String?>(null) }
    val isLoadingNewTitle = block.watchlistNoteId in addingToWatchlistIds
    val titleSelection = remember(block.watchlistNoteId) { WatchlistSelection() }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    val deviceLayout = rememberDeviceTableLayout(block.id)
    val viewsHere = block.viewsInOrderOf(deviceLayout)
    val openView = block.openViewOn(deviceLayout)

    fun closeSearch() {
        isSearchOpen = false
        searchText = ""
    }

    KmpBackHandler(enabled = titleSelection.isSelecting) { titleSelection.stop() }

    LaunchedEffect(openView.layout, inSelectionMode) {
        if (openView.layout == WatchlistView.BOARD || inSelectionMode) titleSelection.stop()
    }

    LaunchedEffect(titles, openView, searchText) {
        titleSelection.keepOnly(titles.orEmpty().shownIn(openView, searchText).map { it.id })
    }

    LaunchedEffect(isSearchOpen) {
        if (!isSearchOpen) return@LaunchedEffect
        withFrameNanos { }
        runCatching { searchFocusRequester.requestFocus() }
    }

    fun startAddingTitle() {
        if (inSelectionMode) {
            onToggleSelection()
            return
        }
        editor.loadSuggestionsEarly()
        if (isDesktopPlatform) {
            isAddingTitle = true
            searchFocusNonce = UUID.randomUUID().toString()
        } else {
            isSearchSheetOpen = true
        }
    }

    LaunchedEffect(lastAddedBlockId) {
        if (lastAddedBlockId == null) return@LaunchedEffect
        delay(NEW_TITLE_HIGHLIGHT_MILLIS)
        editor.forgetLastAddedTitle()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() }
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        WatchlistHeader(
            block = block,
            inSelectionMode = inSelectionMode,
            onToggleSelection = onToggleSelection,
            onRename = { title -> editor.rename(block.id, title) }
        )
        WatchlistToolbar(
            block = block,
            views = viewsHere,
            openView = openView,
            editor = editor,
            isEnabled = !inSelectionMode,
            isSearchActive = searchText.isNotBlank(),
            onSearchClick = { if (isSearchOpen) closeSearch() else isSearchOpen = true },
            onAddTitle = { runAfterKeyboardCloses { startAddingTitle() } },
            onDelete = onDelete,
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
        if (isSearchOpen) {
            MovieSearchField(
                query = searchText,
                onQueryChange = { searchText = it },
                onClose = ::closeSearch,
                onSearchAction = {},
                placeholder = "Search this watchlist",
                fieldModifier = Modifier
                    .focusRequester(searchFocusRequester)
                    .hidesEditorToolbarWhileFocused()
                    .onPreviewKeyEvent { event ->
                        val isEscapePress = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                        if (isEscapePress) closeSearch()
                        isEscapePress
                    }
            )
        }
        WatchlistFilterChips(block = block, openView = openView, editor = editor, isEnabled = !inSelectionMode)
        if (titleSelection.isSelecting) {
            WatchlistSelectionBar(
                selectedCount = titleSelection.selectedTitleIds.size,
                onRefresh = {
                    titles.orEmpty()
                        .filter { titleSelection.isSelected(it.id) }
                        .forEach { title -> title.details?.let { movieEditor.refreshDetails(title.id, it) } }
                    titleSelection.stop()
                },
                onRemove = {
                    editor.removeTitles(block.watchlistNoteId, titleSelection.selectedTitleIds)
                    titleSelection.stop()
                },
                onClear = titleSelection::stop
            )
        }

        if (isAddingTitle) {
            MovieSearchBox(
                search = editor::search,
                onChoose = { result ->
                    editor.addTitle(block.watchlistNoteId, result)
                    isAddingTitle = false
                },
                focusRequestNonce = searchFocusNonce,
                excludedTitleKeys = titles.orEmpty().mapNotNullTo(HashSet()) { it.titleKey() },
                onClose = { isAddingTitle = false }
            )
        }

        MovieSearchSheet(
            expanded = isSearchSheetOpen,
            search = editor::search,
            excludedTitleKeys = titles.orEmpty().mapNotNullTo(HashSet()) { it.titleKey() },
            onChoose = { result -> editor.addTitle(block.watchlistNoteId, result) },
            onDismiss = { isSearchSheetOpen = false }
        )

        AnimatedVisibility(
            visible = isLoadingNewTitle,
            enter = expandVertically(tween(TITLE_RESIZE_MILLIS)) + fadeIn(tween(TITLE_FADE_MILLIS)),
            exit = shrinkVertically(tween(TITLE_RESIZE_MILLIS)) + fadeOut(tween(TITLE_FADE_MILLIS))
        ) {
            MovieCardLoading()
        }

        val loadedTitles = titles
        val shownTitles = loadedTitles.orEmpty().shownIn(openView, searchText)
        val shownTitleIds = shownTitles.map { it.id }
        val titleKeysInWatchlist = loadedTitles.orEmpty().mapNotNullTo(HashSet()) { it.titleKey() }
        val watchlistAdder = remember(titleKeysInWatchlist, isLoadingNewTitle, block.watchlistNoteId) {
            WatchlistAdder(
                titleKeysInWatchlist = titleKeysInWatchlist,
                isAdding = isLoadingNewTitle,
                add = { result -> editor.addTitle(block.watchlistNoteId, result) }
            )
        }
        CompositionLocalProvider(LocalWatchlistAdder provides watchlistAdder) {
            when {
                loadedTitles == null -> Unit
                loadedTitles.isEmpty() && !isAddingTitle && !isLoadingNewTitle -> WatchlistEmptyState(onAddTitle = { startAddingTitle() })
                shownTitles.isEmpty() && loadedTitles.isNotEmpty() && searchText.isNotBlank() -> WatchlistNoMatches(
                    message = "No titles match \"${searchText.trim()}\"",
                    clearLabel = "Clear search",
                    onClear = ::closeSearch
                )
                shownTitles.isEmpty() && loadedTitles.isNotEmpty() -> WatchlistNoMatches(
                    message = "No titles match these filters",
                    clearLabel = "Clear filters",
                    onClear = { editor.changeView(block.id, openView.id) { it.copy(filters = emptyList()) } }
                )
                openView.layout == WatchlistView.BOARD -> WatchlistBoard(
                    titles = shownTitles,
                    statuses = openView.boardStatuses(),
                    movieEditor = movieEditor,
                    inSelectionMode = inSelectionMode,
                    onToggleSelection = onToggleSelection
                )
                openView.layout == WatchlistView.GALLERY -> WatchlistGallery(
                    titles = shownTitles,
                    movieEditor = movieEditor,
                    inSelectionMode = inSelectionMode,
                    onToggleSelection = onToggleSelection,
                    titleSelection = titleSelection
                )
                else -> shownTitles.forEach { title ->
                    key(title.id) {
                        val isNewlyAdded = title.id == lastAddedBlockId
                        val appearance = remember { MutableTransitionState(!isNewlyAdded).apply { targetState = true } }
                        AnimatedVisibility(
                            visibleState = appearance,
                            enter = expandVertically(tween(TITLE_RESIZE_MILLIS)) + fadeIn(tween(TITLE_FADE_MILLIS, delayMillis = TITLE_FADE_DELAY_MILLIS))
                        ) {
                            WatchlistTitle(
                                title = title,
                                movieEditor = movieEditor,
                                isNewlyAdded = isNewlyAdded,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = onToggleSelection,
                                onRemove = { editor.removeTitle(block.watchlistNoteId, title.id) },
                                selection = if (inSelectionMode) null else titleSelection.forTitle(title.id, shownTitleIds),
                                runAfterKeyboardCloses = runAfterKeyboardCloses
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WatchlistHeader(
    block: WatchlistBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRename: (String) -> Unit
) {
    var titleText by remember(block.id) { mutableStateOf(block.title) }

    LaunchedEffect(block.title) {
        if (block.title != titleText) titleText = block.title
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = if (isDesktopPlatform) null else onToggleSelection
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = titleText,
                onValueChange = { newTitle ->
                    titleText = newTitle
                    onRename(newTitle)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f, fill = false).hidesEditorToolbarWhileFocused(),
                decorationBox = { innerTextField ->
                    if (titleText.isEmpty()) {
                        Text(
                            text = "Watchlist",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun WatchlistTitle(
    title: MovieBlock,
    movieEditor: MovieBlockEditor,
    isNewlyAdded: Boolean,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRemove: () -> Unit,
    selection: TitleCardSelection?,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    val loadingBlockIds by movieEditor.loadingBlockIds.collectAsState()
    val details = title.details ?: return
    if (title.id in loadingBlockIds) {
        MovieCardLoading()
        return
    }
    MovieCard(
        block = title,
        details = details,
        editor = movieEditor,
        inSelectionMode = inSelectionMode,
        onToggleSelection = onToggleSelection,
        onDelete = onRemove,
        runAfterKeyboardCloses = runAfterKeyboardCloses,
        isInWatchlist = true,
        selection = selection,
        modifier = if (isNewlyAdded) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), DefaultBlockShape)
        } else {
            Modifier
        }
    )
}

@Composable
private fun WatchlistEmptyState(onAddTitle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .emberrBorder(DefaultBlockShape)
            .clickable(onClick = onAddTitle)
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "No titles yet",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Add a movie or show to start your watchlist",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WatchlistNoMatches(message: String, clearLabel: String, onClear: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .emberrBorder(DefaultBlockShape)
            .padding(vertical = 28.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        MovieChip(label = clearLabel, onClick = onClear) {}
    }
}

