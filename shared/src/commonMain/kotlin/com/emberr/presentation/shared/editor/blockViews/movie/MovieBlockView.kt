package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.hasPoster
import com.emberr.domain.model.titleKey
import com.emberr.domain.movie.TmdbTooManyRequestsException
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.SelectedOptionBackground
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.hidesEditorToolbarWhileFocused
import com.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.film
import emberr.shared.generated.resources.maximize_2
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.star
import emberr.shared.generated.resources.trash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import emberr.shared.generated.resources.x
import com.emberr.presentation.shared.components.customEmberrShadow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Stable
import androidx.compose.foundation.layout.navigationBarsPadding
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.openKeyboardHeightPx
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import kotlin.math.roundToInt

private const val SEARCH_DELAY_MILLIS = 250L
private const val RESULTS_HIDE_DELAY_MILLIS = 150L
private const val KEYBOARD_CLOSE_WAIT_MILLIS = 400L
private const val PART_FADE_IN_MILLIS = 220
private const val PART_FADE_OUT_MILLIS = 120
private const val PART_RESIZE_MILLIS = 260

private val CardPosterWidthDesktop = 88.dp
private val CardPosterWidthPhone = 76.dp
private val CardPadding = 16.dp
private val CardContentGap = 16.dp
internal val CardScrim = Brush.horizontalGradient(
    0f to Color.Black.copy(alpha = 0.35f),
    0.38f to Color.Black.copy(alpha = 0.62f),
    1f to Color.Black.copy(alpha = 0.72f)
)
private val ResultPosterWidth = 30.dp
private val PhoneResultPosterWidth = 44.dp
private val PhoneSearchFieldSpace = 78.dp
internal val PosterCornerRadius = 12.dp
internal val MovieBlock.isWatched: Boolean get() = watchStatus == WatchStatus.WATCHED
private val ChipShape = RoundedCornerShape(8.dp)
private val RowShape = RoundedCornerShape(8.dp)
private val SearchFieldShape = RoundedCornerShape(12.dp)
private val ResultsPanelGap = 6.dp
private val ResultsPanelMaxHeight = 320.dp
private val PhoneResultsMinHeight = 280.dp
private val PhoneResultsMaxHeight = 460.dp
internal val RatingStarColor = Color(0xFFE0A82E)

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
                MovieBlockPart.LOADING -> MovieCardLoading()
                MovieBlockPart.CARD -> block.details?.let { shownDetails ->
                    MovieCard(
                        block = block,
                        details = shownDetails,
                        editor = editor,
                        inSelectionMode = inSelectionMode,
                        onToggleSelection = onToggleSelection,
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SearchFieldShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { isSheetOpen = true }
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
        Text(
            text = "Search a movie or show",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (onClose != null) MovieSearchCloseIcon(onClick = onClose)
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
        val openKeyboardHeight = openKeyboardHeightPx()

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val resultsMaxHeight = with(density) {
                resultsHeightWhileKeyboardMoves(maxHeight, currentKeyboardHeight.toDp(), openKeyboardHeight.toDp())
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SearchFieldShape)
            .background(MaterialTheme.colorScheme.surface)
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
                text = listOf(result.yearText, result.kind.label).filter { it.isNotBlank() }.joinToString(" · "),
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

@Composable
internal fun MovieCardLoading() {
    val pulse = rememberInfiniteTransition()
    val shimmerAlpha by pulse.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse)
    )
    val shimmerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = shimmerAlpha)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(CardPadding),
        horizontalArrangement = Arrangement.spacedBy(CardContentGap)
    ) {
        Box(
            modifier = Modifier
                .width(cardPosterWidth())
                .aspectRatio(2f / 3f)
                .clip(DefaultBlockShape)
                .background(shimmerColor)
        )
        Column(
            modifier = Modifier.weight(1f).padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            listOf(0.45f to 16.dp, 0.6f to 12.dp, 0.35f to 12.dp).forEach { (widthFraction, height) ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(widthFraction)
                        .height(height)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MovieCard(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean = false,
    selection: TitleCardSelection? = null
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isDetailsOpen by remember { mutableStateOf(false) }
    val openDetails = if (inSelectionMode) onToggleSelection else ({ runAfterKeyboardCloses { isDetailsOpen = true } })

    if (isDetailsOpen) {
        MovieDetailsDialog(
            block = block,
            details = details,
            editor = editor,
            onDismiss = { isDetailsOpen = false }
        )
    }

    val hasPoster = details.hasPoster
    val colors = movieCardColors(isOnPoster = hasPoster)
    val posterWidth = cardPosterWidth()
    val chipHeight = movieChipHeight()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (selection?.isSelected == true) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, DefaultBlockShape)
                } else {
                    Modifier
                }
            )
            .clip(DefaultBlockShape)
            .background(if (hasPoster) PosterBackdropBaseColor else MaterialTheme.colorScheme.surface)
            .hoverable(hoverSource)
            .titleSelectionMouseClicks(selection)
            .then(
                if (selection != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (selection.isSelecting) selection.onToggle() },
                        onLongClick = { if (selection.isSelecting) selection.onToggle() else selection.onStartSelecting() }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (hasPoster) {
            BlurredPosterBackdrop(details = details, blurRadius = 30.dp, scrim = CardScrim)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalArrangement = Arrangement.spacedBy(CardContentGap)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .then(if (hasPoster) Modifier.shadow(8.dp, DefaultBlockShape) else Modifier)
                    .clip(DefaultBlockShape)
                    .clickable(onClick = openDetails)
            ) {
                MoviePoster(filePath = details.posterFilePath, url = details.posterUrl, width = posterWidth, cornerRadius = PosterCornerRadius)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = posterWidth * 1.5f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.padding(end = 28.dp).basicMarquee(iterations = Int.MAX_VALUE)) {
                        Text(
                            text = details.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (block.isWatched) colors.secondary else colors.title,
                            textDecoration = if (block.isWatched) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline()
                        )
                        if (details.yearText.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = details.yearText,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondary,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }
                    MovieCardFacts(details = details, colors = colors)
                    if (details.genres.isNotEmpty()) {
                        Text(
                            text = details.genres.joinToString(", "),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MovieChip(
                        label = if (isDesktopPlatform) "Show details" else null,
                        onClick = openDetails,
                        modifier = Modifier.height(chipHeight),
                        background = colors.chipBackground,
                        contentColor = colors.chipContent
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.maximize_2),
                            contentDescription = if (isDesktopPlatform) null else "Show details",
                            tint = colors.chipContent.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    MovieChip(
                        label = block.watchStatus.label,
                        onClick = when {
                            editor == null -> null
                            inSelectionMode -> onToggleSelection
                            else -> ({ editor.showNextWatchStatus(block) })
                        },
                        modifier = Modifier.height(chipHeight),
                        background = colors.chipBackground,
                        contentColor = colors.chipContent
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(block.watchStatus.dotColor(), CircleShape)
                        )
                    }
                }
            }
        }

        if (selection?.isSelecting == true) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = selection.onToggle
                    )
            )
            TitleSelectionCheckbox(
                isSelected = selection.isSelected,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
            )
        } else if (editor != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                if (!isDesktopPlatform || isHovered || isMenuOpen) {
                    IconTooltip(text = "Options") {
                        Icon(
                            painter = painterResource(Res.drawable.ellipsis),
                            contentDescription = "Options",
                            tint = colors.icon,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { runAfterKeyboardCloses { isMenuOpen = true } }
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
                MovieOptionsMenu(
                    expanded = isMenuOpen,
                    title = details.title,
                    onDismiss = { isMenuOpen = false },
                    onChangeTitle = if (isInWatchlist) null else ({ editor.changeTitle(block.id) }),
                    onRefreshDetails = { editor.refreshDetails(block.id, details) },
                    deleteLabel = if (isInWatchlist) "Remove from watchlist" else "Delete block",
                    onDelete = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieCardFacts(details: MovieDetails, colors: MovieCardColors) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = details.kind.label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.chipContent,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.chipBackground)
                .padding(horizontal = 6.dp)
        )
        details.lengthText()?.let { length ->
            Text(text = length, style = MaterialTheme.typography.labelSmall, color = colors.secondary, maxLines = 1)
        }
        details.tmdbRating?.let { rating -> TmdbRating(rating = rating, numberColor = colors.secondary) }
    }
}

@Composable
internal fun TmdbRating(rating: Double, numberColor: Color = MaterialTheme.colorScheme.outline) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(Res.drawable.star),
            contentDescription = null,
            tint = RatingStarColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = tmdbRatingText(rating),
            style = MaterialTheme.typography.labelSmall,
            color = numberColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun MovieChip(
    label: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    background: Color = chipContainerColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (label == null) 8.dp else 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
internal fun movieChipHeight(): Dp =
    with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 12.dp

@Composable
private fun MovieOptionsMenu(
    expanded: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onChangeTitle: (() -> Unit)?,
    onRefreshDetails: () -> Unit,
    deleteLabel: String,
    onDelete: () -> Unit
) {
    MovieMenu(expanded = expanded, title = title, onDismiss = onDismiss) { closeAnd ->
        if (onChangeTitle != null) {
            MovieMenuOption(label = "Change title", icon = Res.drawable.search, onClick = { closeAnd(onChangeTitle) })
        }
        MovieMenuOption(label = "Refresh details", icon = Res.drawable.refresh_cw, onClick = { closeAnd(onRefreshDetails) })
        MovieMenuOption(
            label = deleteLabel,
            icon = Res.drawable.trash,
            labelColor = MaterialTheme.colorScheme.error,
            onClick = { closeAnd(onDelete) }
        )
    }
}

@Composable
internal fun MoviePoster(filePath: String?, url: String?, width: Dp, cornerRadius: Dp) {
    var isImageShown by remember(filePath, url) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(cornerRadius))
            .then(if (isImageShown) Modifier else Modifier.background(chipContainerColor())),
        contentAlignment = Alignment.Center
    ) {
        if (!isImageShown) {
            Icon(
                painter = painterResource(Res.drawable.film),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(width * 0.4f)
            )
        }
        if (filePath != null || url != null) {
            MovieImage(filePath = filePath, url = url, modifier = Modifier.fillMaxSize(), onLoaded = { isImageShown = true })
        }
    }
}

@Composable
internal fun chipContainerColor(): Color =
    if (!isDesktopPlatform || LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFD8D8D8)

private fun cardPosterWidth(): Dp = if (isDesktopPlatform) CardPosterWidthDesktop else CardPosterWidthPhone

@Composable
internal fun WatchStatus.dotColor(): Color {
    val isDark = LocalAppIsDark.current
    return when (this) {
        WatchStatus.NOT_WATCHED -> if (isDark) Color(0xFF529CCA) else Color(0xFF2383E2)
        WatchStatus.WATCHING -> if (isDark) Color(0xFFD9A347) else Color(0xFFCB912F)
        WatchStatus.WATCHED -> if (isDark) Color(0xFF5FA37E) else Color(0xFF448361)
    }
}

internal fun MovieDetails.lengthText(): String? = when (kind) {
    MovieKind.MOVIE -> runtimeMinutes?.let(::minutesText)
    MovieKind.SHOW -> seasonCount?.let { seasons -> if (seasons == 1) "1 season" else "$seasons seasons" }
}

internal fun minutesText(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

internal fun tmdbRatingText(rating: Double): String = ((rating * 10).roundToInt() / 10.0).toString()

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
