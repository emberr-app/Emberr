package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.hasPoster
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrHorizontalScrollbar
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.ui.theme.LocalAppIsDark
import kotlin.math.roundToInt

private val BoardColumnMinWidth = 250.dp
private val BoardColumnGap = 12.dp
private val BoardCardShape = DefaultBlockShape
private val BoardColumnShape = RoundedCornerShape(10.dp)
private val BoardEdgeZone = 48.dp
private const val BOARD_EDGE_SCROLL_STEP_PIXELS = 12f

private data class PendingMove(val status: WatchStatus, val changedAtBeforeMove: Long)

@Stable
private class WatchlistBoardDragState {
    var draggedBlockId by mutableStateOf<String?>(null)
    var pointerInRoot by mutableStateOf(Offset.Zero)
    var grabOffset by mutableStateOf(Offset.Zero)
    var draggedCardWidthPx by mutableIntStateOf(0)
    val columnBoundsInRoot = mutableStateMapOf<WatchStatus, Rect>()

    fun statusUnderPointer(): WatchStatus? =
        columnBoundsInRoot.entries.firstOrNull { (_, bounds) -> pointerInRoot.x in bounds.left..bounds.right }?.key

    fun reset() {
        draggedBlockId = null
    }
}

@Composable
internal fun WatchlistBoard(
    titles: List<MovieBlock>,
    statuses: List<WatchStatus>,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit
) {
    val dragState = remember { WatchlistBoardDragState() }
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    var boardOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var boardWidthPx by remember { mutableIntStateOf(0) }
    val pendingMoves = remember { mutableStateMapOf<String, PendingMove>() }

    LaunchedEffect(titles) {
        val titlesById = titles.associateBy { it.id }
        pendingMoves.keys.toList().forEach { blockId ->
            val title = titlesById[blockId]
            if (title == null || title.watchStateChangedAt != pendingMoves[blockId]?.changedAtBeforeMove) pendingMoves.remove(blockId)
        }
    }

    fun statusShownFor(title: MovieBlock): WatchStatus = pendingMoves[title.id]?.status ?: title.watchStatus

    fun dropDraggedCard() {
        val draggedTitle = titles.firstOrNull { it.id == dragState.draggedBlockId }
        val targetStatus = dragState.statusUnderPointer()
        if (draggedTitle != null && targetStatus != null && targetStatus != statusShownFor(draggedTitle)) {
            pendingMoves[draggedTitle.id] = PendingMove(targetStatus, draggedTitle.watchStateChangedAt)
            movieEditor.setWatchStatus(draggedTitle, targetStatus)
        }
        dragState.reset()
    }

    LaunchedEffect(dragState.draggedBlockId) {
        if (dragState.draggedBlockId == null) return@LaunchedEffect
        val edgeZonePx = with(density) { BoardEdgeZone.toPx() }
        while (true) {
            scrollBoardNearEdges(scrollState, dragState.pointerInRoot.x - boardOriginInRoot.x, boardWidthPx, edgeZonePx)
            withFrameNanos { }
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                boardOriginInRoot = coordinates.positionInRoot()
                boardWidthPx = coordinates.size.width
            }
    ) {
        val columnWidth = maxOf(BoardColumnMinWidth, (maxWidth - BoardColumnGap * (statuses.size - 1)) / statuses.size)
        Column {
            Row(
                modifier = Modifier.horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(BoardColumnGap),
                verticalAlignment = Alignment.Top
            ) {
                statuses.forEach { status ->
                    key(status) {
                        WatchlistBoardColumn(
                            status = status,
                            titles = titles.filter { statusShownFor(it) == status },
                            width = columnWidth,
                            isDropTarget = dragState.draggedBlockId != null &&
                                dragState.statusUnderPointer() == status &&
                                titles.firstOrNull { it.id == dragState.draggedBlockId }?.let(::statusShownFor) != status,
                            dragState = dragState,
                            movieEditor = movieEditor,
                            inSelectionMode = inSelectionMode,
                            onToggleSelection = onToggleSelection,
                            onDrop = ::dropDraggedCard
                        )
                    }
                }
            }
            EmberrHorizontalScrollbar(
                scrollState = scrollState,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }

        val draggedTitle = titles.firstOrNull { it.id == dragState.draggedBlockId }
        val draggedDetails = draggedTitle?.details
        if (draggedTitle != null && draggedDetails != null) {
            val cardOffset = dragState.pointerInRoot - boardOriginInRoot - dragState.grabOffset
            WatchlistBoardCard(
                title = draggedTitle,
                details = draggedDetails,
                modifier = Modifier
                    .zIndex(1f)
                    .offset { IntOffset(cardOffset.x.roundToInt(), cardOffset.y.roundToInt()) }
                    .width(with(density) { dragState.draggedCardWidthPx.toDp() })
                    .shadow(elevation = 8.dp, shape = BoardCardShape)
            )
        }
    }
}

private suspend fun scrollBoardNearEdges(scrollState: ScrollState, pointerX: Float, boardWidthPx: Int, edgeZonePx: Float) {
    when {
        pointerX < edgeZonePx -> scrollState.scrollBy(-BOARD_EDGE_SCROLL_STEP_PIXELS)
        pointerX > boardWidthPx - edgeZonePx -> scrollState.scrollBy(BOARD_EDGE_SCROLL_STEP_PIXELS)
    }
}

@Composable
private fun WatchlistBoardColumn(
    status: WatchStatus,
    titles: List<MovieBlock>,
    width: Dp,
    isDropTarget: Boolean,
    dragState: WatchlistBoardDragState,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDrop: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .onGloballyPositioned { dragState.columnBoundsInRoot[status] = it.boundsInRoot() }
            .clip(BoardColumnShape)
            .background(MaterialTheme.colorScheme.surface)
            .then(
                if (isDropTarget) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), BoardColumnShape)
                } else {
                    Modifier
                }
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.size(8.dp).background(status.dotColor(), CircleShape))
            Text(
                text = status.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = titles.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        if (titles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp)
                    .clip(BoardCardShape)
                    .emberrBorder(BoardCardShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Drop titles here",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
        titles.forEach { title ->
            val details = title.details ?: return@forEach
            key(title.id) {
                DraggableBoardCard(
                    title = title,
                    details = details,
                    dragState = dragState,
                    movieEditor = movieEditor,
                    inSelectionMode = inSelectionMode,
                    onToggleSelection = onToggleSelection,
                    onDrop = onDrop
                )
            }
        }
    }
}

@Composable
private fun DraggableBoardCard(
    title: MovieBlock,
    details: MovieDetails,
    dragState: WatchlistBoardDragState,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDrop: () -> Unit
) {
    var cardCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var isDetailsOpen by remember(title.id) { mutableStateOf(false) }
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentInSelectionMode by rememberUpdatedState(inSelectionMode)

    if (isDetailsOpen) {
        MovieDetailsDialog(
            block = title,
            details = details,
            editor = movieEditor,
            onDismiss = { isDetailsOpen = false }
        )
    }

    WatchlistBoardCard(
        title = title,
        details = details,
        modifier = Modifier
            .alpha(if (dragState.draggedBlockId == title.id) 0.4f else 1f)
            .onGloballyPositioned { cardCoordinates = it }
            .pointerInput(title.id) {
                val isThisCardDragged = { dragState.draggedBlockId == title.id }
                val startDrag = { pointerInCard: Offset ->
                    val coordinates = cardCoordinates
                    if (!currentInSelectionMode && dragState.draggedBlockId == null && coordinates != null) {
                        dragState.pointerInRoot = coordinates.localToRoot(pointerInCard)
                        dragState.grabOffset = pointerInCard
                        dragState.draggedCardWidthPx = coordinates.size.width
                        dragState.draggedBlockId = title.id
                    }
                }
                val followPointer = { change: PointerInputChange, _: Offset ->
                    if (isThisCardDragged()) {
                        change.consume()
                        cardCoordinates?.let { dragState.pointerInRoot = it.localToRoot(change.position) }
                    }
                }
                val finishDrag = { if (isThisCardDragged()) currentOnDrop() }
                val cancelDrag = { if (isThisCardDragged()) dragState.reset() }
                try {
                    if (isDesktopPlatform) {
                        detectDragGestures(onDragStart = startDrag, onDragEnd = finishDrag, onDragCancel = cancelDrag, onDrag = followPointer)
                    } else {
                        detectDragGesturesAfterLongPress(onDragStart = startDrag, onDragEnd = finishDrag, onDragCancel = cancelDrag, onDrag = followPointer)
                    }
                } finally {
                    cancelDrag()
                }
            }
            .clickable { if (inSelectionMode) onToggleSelection() else isDetailsOpen = true }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WatchlistBoardCard(title: MovieBlock, details: MovieDetails, modifier: Modifier = Modifier) {
    val hasPoster = details.hasPoster
    val colors = movieCardColors(isOnPoster = hasPoster)
    val plainBackground = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.background
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(BoardCardShape)
            .background(if (hasPoster) PosterBackdropBaseColor else plainBackground)
    ) {
        if (hasPoster) {
            BlurredPosterBackdrop(details = details, blurRadius = 30.dp, scrim = CardScrim)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MoviePoster(filePath = details.posterFilePath, url = details.posterUrl, width = 44.dp, cornerRadius = 4.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.title,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(details.yearText, details.kind.label)
                        .filter { it.isNotBlank() }
                        .forEach { fact ->
                            Text(text = fact, style = MaterialTheme.typography.labelSmall, color = colors.secondary, maxLines = 1)
                        }
                    details.tmdbRating?.let { rating -> TmdbRating(rating = rating, numberColor = colors.secondary) }
                }
            }
        }
    }
}
