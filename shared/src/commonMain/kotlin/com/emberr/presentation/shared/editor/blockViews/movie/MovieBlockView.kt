package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.titleKey
import com.emberr.domain.movie.TmdbTooManyRequestsException
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.SelectedOptionBackground
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.editor.hidesEditorToolbarWhileFocused
import com.emberr.presentation.shared.fullKeyboardHeightPx
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.x
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.resources.painterResource

private const val SEARCH_DELAY_MILLIS = 250L
private const val RESULTS_HIDE_DELAY_MILLIS = 150L
private const val KEYBOARD_CLOSE_WAIT_MILLIS = 400L
private const val PART_FADE_IN_MILLIS = 220
private const val PART_FADE_OUT_MILLIS = 120
private const val PART_RESIZE_MILLIS = 260

private val ResultPosterWidth = 30.dp
private val PhoneResultPosterWidth = 44.dp
private val PhoneSearchFieldSpace = 78.dp
private val RowShape = RoundedCornerShape(8.dp)
private val SearchFieldShape = RoundedCornerShape(12.dp)
private val ResultsPanelGap = 6.dp
private val ResultsPanelMaxHeight = 320.dp
private val PhoneResultsMinHeight = 280.dp
private val PhoneResultsMaxHeight = 460.dp

private val noLoadingBlocks = MutableStateFlow(emptySet<String>())

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieBlockView(
    block: MovieBlock,
    editor: MovieBlockEditor?,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    focusRequestNonce: String? = null,
    runAfterKeyboardCloses: (() -> Unit) -> Unit = { it() }
) {
    val loadingBlockIds by (editor?.loadingBlockIds ?: noLoadingBlocks).collectAsState()
    val details = block.details

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) onToggleSelection() },
                onLongClick = if (isDesktopPlatform) null else onToggleSelection
            )
    ) {
        val shownPart = when {
            block.id in loadingBlockIds -> MovieBlockPart.LOADING
            details != null -> MovieBlockPart.CARD
            editor != null -> MovieBlockPart.SEARCH
            else -> MovieBlockPart.NOTHING
        }
        AnimatedContent(
            targetState = shownPart,
            transitionSpec = {
                fadeIn(tween(PART_FADE_IN_MILLIS, delayMillis = PART_FADE_OUT_MILLIS)) togetherWith
                    fadeOut(tween(PART_FADE_OUT_MILLIS)) using
                    SizeTransform(clip = false) { _, _ -> tween(PART_RESIZE_MILLIS) }
            }
        ) { part ->
            when (part) {
                MovieBlockPart.LOADING -> MovieTicketLoading()
                MovieBlockPart.CARD -> block.details?.let { shownDetails ->
                    MovieTicketCard(
                        block = block,
                        details = shownDetails,
                        editor = editor,
                        inBlockSelectionMode = inSelectionMode,
                        onToggleBlockSelection = onToggleSelection,
                        onDelete = onDelete,
                        runAfterKeyboardCloses = runAfterKeyboardCloses
                    )
                }
                MovieBlockPart.SEARCH -> editor?.let {
                    MovieSearchBox(
                        search = it::search,
                        onChoose = { result -> it.chooseTitle(block.id, result) },
                        focusRequestNonce = focusRequestNonce
                    )
                }
                MovieBlockPart.NOTHING -> Unit
            }
        }
    }
}

private enum class MovieBlockPart { SEARCH, LOADING, CARD, NOTHING }

@Composable
internal fun MovieSearchBox(
    search: suspend (String) -> List<MovieSearchResult>,
    onChoose: (MovieSearchResult) -> Unit,
    focusRequestNonce: String?,
    excludedTitleKeys: Set<String> = emptySet(),
    onClose: (() -> Unit)? = null
) {
    if (isDesktopPlatform) {
        DesktopMovieSearchBox(search, onChoose, focusRequestNonce, excludedTitleKeys, onClose)
    } else {
        PhoneMovieSearchBox(search, onChoose, focusRequestNonce, excludedTitleKeys, onClose)
    }
}

@Stable
private class MovieSearchState {
    var results by mutableStateOf<List<MovieSearchResult>?>(null)
    var failureMessage by mutableStateOf<String?>(null)
}

@Composable
private fun rememberMovieSearch(
    query: String,
    isActive: Boolean,
    search: suspend (String) -> List<MovieSearchResult>
): MovieSearchState {
    val state = remember { MovieSearchState() }
    LaunchedEffect(query, isActive) {
        if (!isActive) return@LaunchedEffect
        if (query.isNotBlank()) delay(SEARCH_DELAY_MILLIS)
        try {
            state.results = search(query)
            state.failureMessage = null
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: TmdbTooManyRequestsException) {
            state.failureMessage = "Too many searches right now. Try again in a minute."
        } catch (_: Exception) {
            state.failureMessage = "Needs internet to search"
        }
    }
    return state
}

@Composable
private fun DesktopMovieSearchBox(
    search: suspend (String) -> List<MovieSearchResult>,
    onChoose: (MovieSearchResult) -> Unit,
    focusRequestNonce: String?,
    excludedTitleKeys: Set<String>,
    onClose: (() -> Unit)?
) {
    var query by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var showsResults by remember { mutableStateOf(false) }
    var highlightedIndex by remember { mutableIntStateOf(0) }
    var fieldWidthPx by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val resultsGapPx = with(density) { ResultsPanelGap.roundToPx() }
    val searchState = rememberMovieSearch(query = query, isActive = isFocused, search = search)
    val visibleResults = if (searchState.failureMessage != null) emptyList() else searchState.results.orEmpty().filter { it.titleKey() !in excludedTitleKeys }
    val hasSomethingToShow = searchState.failureMessage != null || visibleResults.isNotEmpty() || (searchState.results != null && query.isNotBlank())

    LaunchedEffect(focusRequestNonce) {
        if (focusRequestNonce != null) runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            showsResults = true
        } else {
            delay(RESULTS_HIDE_DELAY_MILLIS)
            showsResults = false
        }
    }

    LaunchedEffect(searchState.results) { highlightedIndex = 0 }

    fun choose(result: MovieSearchResult) {
        showsResults = false
        focusManager.clearFocus()
        onChoose(result)
    }

    fun close() {
        showsResults = false
        focusManager.clearFocus()
        onClose?.invoke()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        MovieSearchField(
            query = query,
            onQueryChange = { query = it },
            onClose = if (onClose != null) ::close else null,
            modifier = Modifier
                .onSizeChanged { fieldWidthPx = it.width },
            fieldModifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (visibleResults.isNotEmpty()) highlightedIndex = (highlightedIndex + 1) % visibleResults.size
                            true
                        }
                        Key.DirectionUp -> {
                            if (visibleResults.isNotEmpty()) {
                                highlightedIndex = (highlightedIndex - 1 + visibleResults.size) % visibleResults.size
                            }
                            true
                        }
                        Key.Enter, Key.NumPadEnter -> {
                            visibleResults.getOrNull(highlightedIndex)?.let(::choose)
                            true
                        }
                        Key.Escape -> {
                            close()
                            true
                        }
                        else -> false
                    }
                },
            onSearchAction = { visibleResults.getOrNull(highlightedIndex)?.let(::choose) }
        )

        if (showsResults && hasSomethingToShow) {
            val positionProvider = remember(resultsGapPx) { ResultsBelowField(gapPx = resultsGapPx) }
            Popup(popupPositionProvider = positionProvider, properties = PopupProperties(focusable = false)) {
                Column(
                    modifier = Modifier
                        .width(with(density) { fieldWidthPx.toDp() })
                        .customEmberrShadow(SearchFieldShape)
                        .clip(SearchFieldShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .heightIn(max = ResultsPanelMaxHeight)
                        .verticalScroll(rememberScrollState())
                        .padding(6.dp)
                ) {
                    MovieSearchResultsList(
                        query = query,
                        results = visibleResults,
                        failureMessage = searchState.failureMessage,
                        highlightedIndex = highlightedIndex,
                        onHighlight = { highlightedIndex = it },
                        onChoose = ::choose
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneMovieSearchBox(
    search: suspend (String) -> List<MovieSearchResult>,
    onChoose: (MovieSearchResult) -> Unit,
    focusRequestNonce: String?,
    excludedTitleKeys: Set<String>,
    onClose: (() -> Unit)?
) {
    var isSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(focusRequestNonce) {
        if (focusRequestNonce != null) isSheetOpen = true
    }

    MovieSearchRow(onClose = onClose, onClick = { isSheetOpen = true }) {
        Text(
            text = "Search a movie or show",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
    }

    MovieSearchSheet(
        expanded = isSheetOpen,
        search = search,
        excludedTitleKeys = excludedTitleKeys,
        onChoose = onChoose,
        onDismiss = { isSheetOpen = false }
    )
}

@Composable
internal fun MovieSearchSheet(
    expanded: Boolean,
    search: suspend (String) -> List<MovieSearchResult>,
    excludedTitleKeys: Set<String>,
    onChoose: (MovieSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    EmberrBottomSheet(
        expanded = expanded,
        onDismiss = onDismiss,
        title = "Add a movie or show",
        contentHorizontalPadding = 0.dp,
        scrollsContent = false
    ) { closeAnd ->
        var query by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current
        val keyboardController = LocalSoftwareKeyboardController.current
        val coroutineScope = rememberCoroutineScope()
        val currentKeyboardHeight by rememberUpdatedState(WindowInsets.ime.getBottom(LocalDensity.current))
        val searchState = rememberMovieSearch(query = query, isActive = true, search = search)

        var hasChosen by remember { mutableStateOf(false) }

        fun chooseAfterKeyboardCloses(result: MovieSearchResult) {
            if (hasChosen) return
            hasChosen = true
            coroutineScope.launch {
                focusManager.clearFocus()
                keyboardController?.hide()
                withTimeoutOrNull(KEYBOARD_CLOSE_WAIT_MILLIS) {
                    snapshotFlow { currentKeyboardHeight }.first { it == 0 }
                }
                closeAnd { onChoose(result) }
            }
        }
        val visibleResults = if (searchState.failureMessage != null) emptyList() else searchState.results.orEmpty().filter { it.titleKey() !in excludedTitleKeys }

        val resultsHazeState = remember { HazeState() }
        val density = LocalDensity.current
        val fullKeyboardHeight = fullKeyboardHeightPx()

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val resultsMaxHeight = with(density) {
                resultsHeightWhileKeyboardMoves(maxHeight, currentKeyboardHeight.toDp(), fullKeyboardHeight.toDp())
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PhoneResultsMinHeight.coerceAtMost(resultsMaxHeight), max = resultsMaxHeight)
                    .hazeSource(resultsHazeState)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = PhoneSearchFieldSpace)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (searchState.results != null || searchState.failureMessage != null) {
                    MovieSearchResultsList(
                        query = query,
                        results = visibleResults,
                        failureMessage = searchState.failureMessage,
                        highlightedIndex = -1,
                        onHighlight = {},
                        onChoose = ::chooseAfterKeyboardCloses,
                        rowVerticalPadding = 12.dp,
                        posterWidth = PhoneResultPosterWidth
                    )
                }
            }
            EmberrSearchTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search a movie or show",
                hazeState = resultsHazeState,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onSubmit = { focusManager.clearFocus() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .hidesEditorToolbarWhileFocused()
            )
        }
    }
}

@Composable
internal fun MovieSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: (() -> Unit)?,
    onSearchAction: () -> Unit,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    placeholder: String = "Search a movie or show"
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    MovieSearchRow(onClose = onClose, modifier = modifier) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = textStyle,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
            modifier = Modifier.weight(1f).then(fieldModifier),
            decorationBox = { innerTextField ->
                Box {
                    if (query.isEmpty()) {
                        Text(text = placeholder, style = textStyle.copy(color = MaterialTheme.colorScheme.outline))
                    }
                    innerTextField()
                }
            }
        )
    }
}

@Composable
private fun MovieSearchRow(
    onClose: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SearchFieldShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        content()
        if (onClose != null) MovieSearchCloseIcon(onClick = onClose)
    }
}

@Composable
private fun MovieSearchCloseIcon(onClick: () -> Unit) {
    IconTooltip(text = "Close search") {
        Icon(
            painter = painterResource(Res.drawable.x),
            contentDescription = "Close search",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(4.dp)
                .size(14.dp)
        )
    }
}

private class ResultsBelowField(private val gapPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(anchorBounds.left, anchorBounds.bottom + gapPx)
}

private fun resultsHeightWhileKeyboardMoves(spaceNow: Dp, keyboardNow: Dp, keyboardOpen: Dp): Dp {
    val spaceWithKeyboardOpen = spaceNow - (keyboardOpen - keyboardNow)
    val shrinkWithKeyboardOpen = (PhoneResultsMaxHeight - spaceWithKeyboardOpen).coerceAtLeast(0.dp)
    val keyboardProgress = if (keyboardOpen > 0.dp) (keyboardNow / keyboardOpen).coerceIn(0f, 1f) else 0f
    return (PhoneResultsMaxHeight - shrinkWithKeyboardOpen * keyboardProgress).coerceAtMost(spaceNow)
}

@Composable
private fun MovieSearchResultsList(
    query: String,
    results: List<MovieSearchResult>,
    failureMessage: String?,
    highlightedIndex: Int,
    onHighlight: (Int) -> Unit,
    onChoose: (MovieSearchResult) -> Unit,
    rowVerticalPadding: Dp = 6.dp,
    posterWidth: Dp = ResultPosterWidth
) {
    val trimmedQuery = query.trim()
    when {
        failureMessage != null -> MovieSearchResultsMessage(failureMessage)
        results.isEmpty() -> MovieSearchResultsMessage(
            if (trimmedQuery.isEmpty()) {
                "Everything here is already added"
            } else {
                "No titles match \"$trimmedQuery\". Check the spelling or try the original title."
            }
        )
        else -> {
            Text(
                text = if (trimmedQuery.isEmpty()) "Suggestions" else "Results",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
            )
            results.forEachIndexed { index, result ->
                MovieSearchResultRow(
                    result = result,
                    query = trimmedQuery,
                    isHighlighted = index == highlightedIndex,
                    onHover = { onHighlight(index) },
                    onClick = { onChoose(result) },
                    verticalPadding = rowVerticalPadding,
                    posterWidth = posterWidth
                )
            }
        }
    }
}

@Composable
private fun MovieSearchResultRow(
    result: MovieSearchResult,
    query: String,
    isHighlighted: Boolean,
    onHover: () -> Unit,
    onClick: () -> Unit,
    verticalPadding: Dp,
    posterWidth: Dp
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    val currentOnClick by rememberUpdatedState(onClick)
    LaunchedEffect(isHovered) { if (isHovered) onHover() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(if (isHighlighted) SelectedOptionBackground else Color.Transparent)
            .hoverable(hoverSource)
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnClick() }) }
            .padding(horizontal = 8.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MoviePoster(filePath = null, url = result.posterUrl, width = posterWidth, cornerRadius = 4.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titleWithMatchInBold(result.title, query),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOf(result.kind.label, result.yearText).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1
            )
        }
        result.tmdbRating?.let { rating ->
            Spacer(modifier = Modifier.width(8.dp))
            TmdbRating(rating = rating)
        }
    }
}

@Composable
private fun MovieSearchResultsMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

private fun titleWithMatchInBold(title: String, query: String): AnnotatedString = buildAnnotatedString {
    val matchStart = if (query.isEmpty()) -1 else title.indexOf(query, ignoreCase = true)
    if (matchStart < 0) {
        append(title)
        return@buildAnnotatedString
    }
    val matchEnd = matchStart + query.length
    append(title.substring(0, matchStart))
    withStyle(SpanStyle(fontWeight = FontWeight.Medium)) { append(title.substring(matchStart, matchEnd)) }
    append(title.substring(matchEnd))
}
