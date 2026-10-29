package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import com.emberr.domain.movie.takeLoaded
import com.emberr.presentation.shared.components.EmberrHorizontalScrollbar
import com.emberr.presentation.shared.components.GestureCoordinates
import com.emberr.presentation.shared.components.LoadMoreButton
import com.emberr.presentation.shared.components.boardDragGesture
import com.emberr.presentation.shared.components.scrollWhenPointerNearEdges
import com.emberr.presentation.shared.components.emberrBorder
import kotlin.math.roundToInt

private val BoardColumnMinWidth = 250.dp
private val BoardColumnGap = 12.dp
private val BoardCardShape = DefaultBlockShape
private val BoardColumnShape = RoundedCornerShape(10.dp)
private val BoardEdgeZone = 48.dp

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
    onToggleSelection: () -> Unit,
    loadedCountOf: (WatchStatus) -> Int,
    onLoadMore: (WatchStatus) -> Unit,
    alwaysShownTitleIds: Collection<String>
) {
    val dragState = remember { WatchlistBoardDragState() }
    val droppedTitleIds = remember { mutableStateListOf<String>() }
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
            if (draggedTitle.id !in droppedTitleIds) droppedTitleIds += draggedTitle.id
            movieEditor.setWatchStatus(draggedTitle, targetStatus)
        }
        dragState.reset()
    }

    LaunchedEffect(dragState.draggedBlockId) {
        if (dragState.draggedBlockId == null) return@LaunchedEffect
        val edgeZonePx = with(density) { BoardEdgeZone.toPx() }
        while (true) {
            scrollState.scrollWhenPointerNearEdges(dragState.pointerInRoot.x - boardOriginInRoot.x, boardWidthPx, edgeZonePx)
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
                        val titlesInColumn = titles.filter { statusShownFor(it) == status }
                        WatchlistBoardColumn(
                            status = status,
                            titles = titlesInColumn,
                            loadedTitles = titlesInColumn.takeLoaded(loadedCountOf(status), alwaysShownTitleIds + droppedTitleIds),
                            onLoadMore = { onLoadMore(status) },
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
            BoardTicketCard(
                block = draggedTitle,
                details = draggedDetails,
                modifier = Modifier
                    .zIndex(1f)
                    .offset { IntOffset(cardOffset.x.roundToInt(), cardOffset.y.roundToInt()) }
                    .width(with(density) { dragState.draggedCardWidthPx.toDp() })
                    .shadow(elevation = 8.dp, shape = boardTicketShape())
            )
        }
    }
}

@Composable
private fun boardColumnColor(): Color =
    lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface, 0.5f)

@Composable
private fun WatchlistBoardColumn(
    status: WatchStatus,
    titles: List<MovieBlock>,
    loadedTitles: List<MovieBlock>,
    onLoadMore: () -> Unit,
    width: Dp,
    isDropTarget: Boolean,
    dragState: WatchlistBoardDragState,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDrop: () -> Unit
) {
    DisposableEffect(status) {
        onDispose { dragState.columnBoundsInRoot.remove(status) }
    }

    Column(
        modifier = Modifier
            .width(width)
            .onGloballyPositioned { dragState.columnBoundsInRoot[status] = it.boundsInRoot() }
            .clip(BoardColumnShape)
            .background(boardColumnColor())
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
        loadedTitles.forEach { title ->
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
        if (loadedTitles.size < titles.size) {
            LoadMoreButton(onClick = onLoadMore)
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
    val gestureCoordinates = remember { GestureCoordinates() }
    var isDetailsOpen by remember(title.id) { mutableStateOf(false) }
    val currentOnDrop by rememberUpdatedState(onDrop)
    val currentInSelectionMode by rememberUpdatedState(inSelectionMode)
    val isThisCardDragged = { dragState.draggedBlockId == title.id }

    DisposableEffect(title.id) {
        onDispose { if (isThisCardDragged()) dragState.reset() }
    }

    if (isDetailsOpen) {
        MovieDetailsDialog(
            block = title,
            details = details,
            editor = movieEditor,
            onDismiss = { isDetailsOpen = false }
        )
    }

    BoardTicketCard(
        block = title,
        details = details,
        modifier = Modifier
            .alpha(if (isThisCardDragged()) 0.4f else 1f)
            .onGloballyPositioned { gestureCoordinates.coordinates = it }
            .boardDragGesture(
                key = title.id,
                isEnabled = true,
                gestureCoordinates = gestureCoordinates,
                onStart = { pointerInRoot, pointerInCard, cardSize ->
                    if (!currentInSelectionMode && dragState.draggedBlockId == null) {
                        dragState.pointerInRoot = pointerInRoot
                        dragState.grabOffset = pointerInCard
                        dragState.draggedCardWidthPx = cardSize.width
                        dragState.draggedBlockId = title.id
                    }
                },
                onMove = { pointerInRoot -> if (isThisCardDragged()) dragState.pointerInRoot = pointerInRoot },
                onDrop = { if (isThisCardDragged()) currentOnDrop() },
                onCancel = { if (isThisCardDragged()) dragState.reset() }
            )
            .clickable { if (inSelectionMode) onToggleSelection() else isDetailsOpen = true }
    )
}
