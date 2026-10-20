package com.emberr.presentation.calendar

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

const val MovedEventOriginalAlpha = 0.4f
private val EdgeHoldCardOvershoot = 16.dp
private val EdgeHoldPointerZone = 24.dp
private const val EdgeHoldNanosBeforeSwitch = 600_000_000L

enum class EdgeHoldDirection { PREVIOUS, NEXT }

data class EventMovePreview(val event: CalendarEvent, val targetDate: LocalDate, val timeRange: EventTimeRange)

class ActiveEventMove(
    val event: CalendarEvent,
    val pointerId: PointerId,
    val startPointerMinuteOfDay: Float,
    val hourHeightPx: Float,
    val grabOffsetInCard: Offset,
    val cardSize: IntSize
)

class EventMoveTracker {
    var preview by mutableStateOf<EventMovePreview?>(null)
    var activeMove by mutableStateOf<ActiveEventMove?>(null)
        private set
    var pointerInWindow by mutableStateOf(Offset.Zero)
        private set
    var gridCoordinates: LayoutCoordinates? = null
    var autoScroll: EventResizeAutoScroll? = null
    var visibleDates: List<LocalDate> = emptyList()
    private val columnCoordinatesByDate = mutableMapOf<LocalDate, LayoutCoordinates>()

    fun registerColumn(date: LocalDate, coordinates: LayoutCoordinates) {
        columnCoordinatesByDate[date] = coordinates
    }

    fun unregisterColumn(date: LocalDate, coordinates: LayoutCoordinates?) {
        if (columnCoordinatesByDate[date] === coordinates) columnCoordinatesByDate.remove(date)
    }

    fun start(
        event: CalendarEvent,
        pointerId: PointerId,
        pointerInWindow: Offset,
        cardTopLeftInWindow: Offset,
        cardSize: IntSize,
        sourceColumn: LayoutCoordinates,
        hourHeightPx: Float
    ) {
        val pointerYInColumn = sourceColumn.windowToLocal(pointerInWindow).y
        this.pointerInWindow = pointerInWindow
        activeMove = ActiveEventMove(
            event = event,
            pointerId = pointerId,
            startPointerMinuteOfDay = pointerYInColumn / hourHeightPx * 60f,
            hourHeightPx = hourHeightPx,
            grabOffsetInCard = pointerInWindow - cardTopLeftInWindow,
            cardSize = cardSize
        )
    }

    fun moveTo(pointerInWindow: Offset) {
        this.pointerInWindow = pointerInWindow
        refreshPreview()
    }

    fun refreshPreview() {
        val move = activeMove ?: return
        val (targetDate, targetColumn) = columnUnderPointer() ?: return
        val pointerMinuteOfDay = targetColumn.windowToLocal(pointerInWindow).y / move.hourHeightPx * 60f
        preview = EventMovePreview(
            event = move.event,
            targetDate = targetDate,
            timeRange = moveEventTimeRange(move.event.timeRange(), pointerMinuteOfDay - move.startPointerMinuteOfDay)
        )
    }

    fun finish(): EventMovePreview? {
        val finished = preview?.takeIf { activeMove != null }
        activeMove = null
        val isUnchanged = finished == null ||
            (finished.targetDate.toString() == finished.event.dateString && finished.timeRange == finished.event.timeRange())
        if (isUnchanged) {
            preview = null
            return null
        }
        return finished
    }

    fun edgeHoldDirection(cardOvershootPx: Float, pointerZonePx: Float): EdgeHoldDirection? {
        val move = activeMove ?: return null
        val columns = visibleAttachedColumns().values
        if (columns.isEmpty()) return null
        val daysLeft = columns.minOf { it.positionInWindow().x }
        val daysRight = columns.maxOf { it.positionInWindow().x + it.size.width }
        val cardLeft = pointerInWindow.x - move.grabOffsetInCard.x
        val cardRight = cardLeft + move.cardSize.width
        return when {
            cardLeft <= daysLeft - cardOvershootPx || pointerInWindow.x <= daysLeft + pointerZonePx -> EdgeHoldDirection.PREVIOUS
            cardRight >= daysRight + cardOvershootPx || pointerInWindow.x >= daysRight - pointerZonePx -> EdgeHoldDirection.NEXT
            else -> null
        }
    }

    fun floatingCardOffsetInGrid(): IntOffset {
        val move = activeMove ?: return IntOffset.Zero
        val grid = gridCoordinates?.takeIf { it.isAttached } ?: return IntOffset.Zero
        val cardTopLeft = grid.windowToLocal(pointerInWindow - move.grabOffsetInCard)
        return IntOffset(cardTopLeft.x.roundToInt(), cardTopLeft.y.roundToInt())
    }

    private fun columnUnderPointer(): Pair<LocalDate, LayoutCoordinates>? =
        visibleAttachedColumns().entries
            .minByOrNull { (_, column) -> horizontalDistance(column, pointerInWindow.x) }
            ?.toPair()

    private fun visibleAttachedColumns(): Map<LocalDate, LayoutCoordinates> =
        columnCoordinatesByDate.filter { (date, column) -> date in visibleDates && column.isAttached }

    private fun horizontalDistance(column: LayoutCoordinates, pointerX: Float): Float {
        val left = column.positionInWindow().x
        val right = left + column.size.width
        return when {
            pointerX < left -> left - pointerX
            pointerX > right -> pointerX - right
            else -> 0f
        }
    }
}

suspend fun PointerInputScope.followEventMovePointer(
    tracker: EventMoveTracker,
    onMoveFinished: (EventMovePreview) -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val pointerEvent = awaitPointerEvent()
            val move = tracker.activeMove ?: continue
            val change = pointerEvent.changes.firstOrNull { it.id == move.pointerId } ?: continue
            change.consume()
            if (!change.pressed) {
                tracker.finish()?.let(onMoveFinished)
                continue
            }
            val grid = tracker.gridCoordinates?.takeIf { it.isAttached } ?: continue
            tracker.moveTo(grid.localToWindow(change.position))
        }
    }
}

@Composable
fun EventMoveFrameEffect(
    tracker: EventMoveTracker,
    onHoldAtPreviousEdge: () -> Unit,
    onHoldAtNextEdge: () -> Unit
) {
    val density = LocalDensity.current
    val cardOvershootPx = with(density) { EdgeHoldCardOvershoot.toPx() }
    val pointerZonePx = with(density) { EdgeHoldPointerZone.toPx() }
    val currentOnHoldAtPreviousEdge by rememberUpdatedState(onHoldAtPreviousEdge)
    val currentOnHoldAtNextEdge by rememberUpdatedState(onHoldAtNextEdge)

    LaunchedEffect(tracker.activeMove, cardOvershootPx, pointerZonePx) {
        if (tracker.activeMove == null) return@LaunchedEffect
        var heldDirection: EdgeHoldDirection? = null
        var heldSinceNanos = 0L
        var hasLeftStartingEdge = tracker.edgeHoldDirection(cardOvershootPx, pointerZonePx) == null
        while (true) {
            val frameNanos = withFrameNanos { it }
            tracker.autoScroll?.scrollTowards(tracker.pointerInWindow)

            val edgeUnderPointer = tracker.edgeHoldDirection(cardOvershootPx, pointerZonePx)
            if (edgeUnderPointer == null) hasLeftStartingEdge = true
            val direction = edgeUnderPointer.takeIf { hasLeftStartingEdge }
            if (direction != heldDirection) {
                heldDirection = direction
                heldSinceNanos = frameNanos
            } else if (direction != null && frameNanos - heldSinceNanos >= EdgeHoldNanosBeforeSwitch) {
                when (direction) {
                    EdgeHoldDirection.PREVIOUS -> currentOnHoldAtPreviousEdge()
                    EdgeHoldDirection.NEXT -> currentOnHoldAtNextEdge()
                }
                heldSinceNanos = frameNanos
            }

            tracker.refreshPreview()
        }
    }
}

@Composable
fun FloatingMovedEventCard(tracker: EventMoveTracker, categories: List<CalendarCategory>) {
    val move = tracker.activeMove ?: return
    val density = LocalDensity.current
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val category = categories.firstOrNull { it.id == move.event.categoryId }
    val timeRange = tracker.preview?.timeRange ?: move.event.timeRange()

    EventMovePreviewChip(
        text = move.event.text,
        timeText = formatEventTimeRange(timeRange, usesTwentyFourHourClock),
        color = category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.surface,
        hasCategory = category != null,
        isChecked = move.event.isChecked,
        height = with(density) { move.cardSize.height.toDp() },
        modifier = Modifier
            .offset { tracker.floatingCardOffsetInGrid() }
            .width(with(density) { move.cardSize.width.toDp() })
    )
}
