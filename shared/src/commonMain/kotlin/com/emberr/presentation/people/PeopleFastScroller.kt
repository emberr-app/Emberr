package com.emberr.presentation.people

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.triggerHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

private val ThumbTouchWidth = 32.dp
private val ThumbHeight = 48.dp
private val ThumbIdleWidth = 4.dp
private val ThumbDraggedWidth = 8.dp
private val ThumbEdgeGap = 4.dp
private val BubbleSize = 60.dp
private val BubbleGap = 8.dp
private val BubbleShape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomEndPercent = 0, bottomStartPercent = 50)
private val HideAfterScrollStops = 1500.milliseconds

@Composable
fun PeopleFastScroller(
    listState: LazyListState,
    itemLetters: List<String>,
    modifier: Modifier = Modifier
) {
    val canScroll by remember(listState) { derivedStateOf { listState.canScrollForward || listState.canScrollBackward } }
    if (!canScroll) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val listFraction by remember(listState) { derivedStateOf { scrolledFraction(listState) } }
    var draggedFraction by remember { mutableStateOf<Float?>(null) }
    var isShown by remember { mutableStateOf(false) }
    val isDragging = draggedFraction != null
    val isScrolling = listState.isScrollInProgress

    LaunchedEffect(isScrolling, isDragging) {
        if (isScrolling || isDragging) {
            isShown = true
        } else {
            delay(HideAfterScrollStops)
            isShown = false
        }
    }
    val thumbAlpha by animateFloatAsState(if (isShown) 1f else 0f)
    val thumbWidth by animateDpAsState(if (isDragging) ThumbDraggedWidth else ThumbIdleWidth)
    val thumbColor = if (isDragging) MaterialTheme.colorScheme.primary else mutedTextColor()

    BoxWithConstraints(modifier = modifier.fillMaxHeight().width(ThumbTouchWidth + BubbleGap + BubbleSize)) {
        val trackHeightPx = with(density) { (maxHeight - ThumbHeight).toPx() }.coerceAtLeast(1f)
        val thumbFraction = draggedFraction ?: listFraction
        val thumbTopPx = (thumbFraction * trackHeightPx).roundToInt()

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset { IntOffset(0, thumbTopPx) }
                .size(ThumbTouchWidth, ThumbHeight)
                .dragThumbToScroll(
                    isEnabled = isShown || isDragging,
                    listState = listState,
                    itemLetters = itemLetters,
                    trackHeightPx = trackHeightPx,
                    scope = scope,
                    listFraction = { listFraction },
                    onDraggedFractionChange = { fraction -> draggedFraction = fraction }
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = ThumbEdgeGap)
                    .alpha(thumbAlpha)
                    .width(thumbWidth)
                    .fillMaxHeight()
                    .background(thumbColor, CircleShape)
            )
        }

        val bubbleLetter = if (isDragging) letterAtFraction(listState, itemLetters, thumbFraction) else null
        if (bubbleLetter != null) {
            val bubbleTopPx = thumbTopPx + with(density) { (ThumbHeight / 2 - BubbleSize).roundToPx() }
            val bubbleEndShiftPx = with(density) { (ThumbTouchWidth + BubbleGap).roundToPx() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(-bubbleEndShiftPx, bubbleTopPx) }
                    .size(BubbleSize)
                    .background(MaterialTheme.colorScheme.primary, BubbleShape)
            ) {
                Text(
                    text = bubbleLetter,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

private fun Modifier.dragThumbToScroll(
    isEnabled: Boolean,
    listState: LazyListState,
    itemLetters: List<String>,
    trackHeightPx: Float,
    scope: CoroutineScope,
    listFraction: () -> Float,
    onDraggedFractionChange: (Float?) -> Unit
): Modifier {
    if (!isEnabled) return this
    return pointerInput(listState, trackHeightPx, itemLetters) {
        var draggedFraction = 0f
        var lastLetter: String? = null
        detectVerticalDragGestures(
            onDragStart = {
                draggedFraction = listFraction()
                onDraggedFractionChange(draggedFraction)
                lastLetter = letterAtFraction(listState, itemLetters, draggedFraction)
            },
            onDragEnd = { onDraggedFractionChange(null) },
            onDragCancel = { onDraggedFractionChange(null) },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                val fraction = (draggedFraction + dragAmount / trackHeightPx).coerceIn(0f, 1f)
                draggedFraction = fraction
                onDraggedFractionChange(fraction)
                scope.launch {
                    scrollToFraction(listState, fraction)
                    val letter = letterAtFraction(listState, itemLetters, fraction)
                    if (letter != lastLetter) {
                        lastLetter = letter
                        triggerHapticFeedback()
                    }
                }
            }
        )
    }
}

private fun letterAtFraction(listState: LazyListState, itemLetters: List<String>, fraction: Float): String? {
    val layoutInfo = listState.layoutInfo
    val contentEnd = layoutInfo.viewportEndOffset - layoutInfo.afterContentPadding
    val itemsOnScreen = layoutInfo.visibleItemsInfo.filter { item -> item.offset + item.size > 0 && item.offset < contentEnd }
    if (itemsOnScreen.isEmpty()) return null
    val readIndex = (fraction * itemsOnScreen.lastIndex).roundToInt().coerceIn(0, itemsOnScreen.lastIndex)
    return itemLetters.getOrNull(itemsOnScreen[readIndex].index)
}

private fun scrolledFraction(listState: LazyListState): Float {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (layoutInfo.totalItemsCount == 0 || visibleItems.isEmpty()) return 0f
    if (!listState.canScrollForward) return 1f
    val firstItemSize = visibleItems.firstOrNull { it.index == listState.firstVisibleItemIndex }?.size?.coerceAtLeast(1) ?: 1
    val scrolledItems = listState.firstVisibleItemIndex + listState.firstVisibleItemScrollOffset.toFloat() / firstItemSize
    val scrollableItems = (layoutInfo.totalItemsCount - visibleItems.size).coerceAtLeast(1)
    return (scrolledItems / scrollableItems).coerceIn(0f, 1f)
}

private suspend fun scrollToFraction(listState: LazyListState, fraction: Float) {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (layoutInfo.totalItemsCount == 0 || visibleItems.isEmpty()) return
    if (fraction >= 1f) {
        listState.scrollToItem(layoutInfo.totalItemsCount - 1)
        return
    }
    val scrollableItems = (layoutInfo.totalItemsCount - visibleItems.size).coerceAtLeast(1)
    val targetPosition = fraction * scrollableItems
    val targetIndex = floor(targetPosition).toInt().coerceIn(0, layoutInfo.totalItemsCount - 1)
    val averageItemSize = visibleItems.sumOf { it.size } / visibleItems.size
    listState.scrollToItem(targetIndex, ((targetPosition - targetIndex) * averageItemSize).roundToInt())
}
