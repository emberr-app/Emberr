package com.emberr.presentation.desktop

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.emberr.presentation.desktop.window.keepTitleBarHiddenWhileHovered
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrBorderWidth
import com.emberr.presentation.shared.components.EmberrShadowAmbientColor
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrShadowSpotColor
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorderColor
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.x
import kotlin.math.ceil
import org.jetbrains.compose.resources.painterResource
import java.awt.Cursor
import kotlinx.datetime.LocalDate

private const val MAX_SPLIT_PANES = 4
private val MIN_SPLIT_PANE_WIDTH = 320.dp
private val ADD_PANE_DROP_ZONE_WIDTH = 72.dp
private val SPLIT_DIVIDER_WIDTH = 12.dp
private val SPLIT_PILL_TOP_GAP = 6.dp
private val SPLIT_HEADER_HOVER_HEIGHT = 64.dp
private val SPLIT_HANDLE_LINE_WIDTH = 36.dp
private val SPLIT_HANDLE_LINE_HEIGHT = 4.dp
private val SPLIT_PILL_BUTTON_SIZE = 28.dp
private val SPLIT_PILL_ICON_SIZE = 16.dp
private val SPLIT_PILL_WIDTH = SPLIT_PILL_BUTTON_SIZE * 2 + 8.dp
private val SPLIT_PILL_HEIGHT = 30.dp
private const val NOTCH_SPRING_DAMPING = 0.9f
private const val NOTCH_SPRING_STIFFNESS = 380f
private const val NOTCH_COLOR_MILLIS = 180
private const val NOTCH_BUTTONS_FADE_IN_MILLIS = 160
private const val NOTCH_BUTTONS_FADE_IN_DELAY_MILLIS = 90
private const val NOTCH_BUTTONS_FADE_OUT_MILLIS = 80
private val SPLIT_DROP_ZONE_GAP = 12.dp
private val SPLIT_DROP_ZONE_SHAPE = RoundedCornerShape(16.dp)

private const val SAVED_SPLIT_PREFIX = "SPLIT:"
private const val SAVED_SPLIT_SEPARATOR = "|"
private const val SAVED_FRACTION_SEPARATOR = ","

private val settingsScreenPanes = setOf(
    DetailPane.Settings,
    DetailPane.SelfHostSetup,
    DetailPane.About,
    DetailPane.ImportNotes,
    DetailPane.OpenSourceLicenses
)

sealed interface SplitDropTarget {
    data class ReplacePane(val index: Int) : SplitDropTarget
    data class InsertPane(val index: Int) : SplitDropTarget
}

data class SavedSplitLayout(
    val panes: List<DetailPane>,
    val focusedIndex: Int,
    val widthFractions: List<Float>
) {
    val focusedPane: DetailPane get() = panes[focusedIndex]

    fun keepingOnly(panesToKeep: List<DetailPane>): SavedSplitLayout {
        if (panesToKeep == panes) return this
        return SavedSplitLayout(
            panes = panesToKeep,
            focusedIndex = panesToKeep.indexOf(focusedPane).coerceAtLeast(0),
            widthFractions = equalFractions(panesToKeep.size)
        )
    }
}

class DesktopSplitState {
    var panes by mutableStateOf<List<DetailPane>>(emptyList())
        private set
    var focusedIndex by mutableStateOf(0)
        private set
    private var widthFractions by mutableStateOf(listOf(1f))
    private var contentBoundsInWindow by mutableStateOf(Rect.Zero)
    private var minimumPaneWidthPx by mutableStateOf(0f)
    private var addPaneDropZoneWidthPx by mutableStateOf(0f)
    var draggedPaneIndex by mutableStateOf<Int?>(null)
        private set
    private var paneDragCursorInWindow by mutableStateOf(Offset.Zero)

    val isSplit: Boolean get() = panes.size > 1

    val visibleWidthFractions: List<Float>
        get() {
            val contentWidth = contentBoundsInWindow.width
            if (contentWidth <= 0f) return widthFractions
            val minimumFraction = (minimumPaneWidthPx / contentWidth).coerceAtMost(1f / widthFractions.size)
            return fractionsRespectingMinimum(widthFractions, minimumFraction)
        }

    val paneDragTargetIndex: Int?
        get() {
            val indexBeingDragged = draggedPaneIndex ?: return null
            return paneIndexAt(paneDragCursorInWindow)?.takeIf { hoveredIndex -> hoveredIndex != indexBeingDragged }
        }

    var focusedPane: DetailPane?
        get() = panes.getOrNull(focusedIndex)
        set(pane) {
            if (pane == null) return
            if (panes.isEmpty()) {
                panes = listOf(pane)
                focusedIndex = 0
                return
            }
            val duplicateIndex = panes.indices.firstOrNull { index -> index != focusedIndex && panes[index].isSameScreenAs(pane) }
            val targetIndex = duplicateIndex ?: focusedIndex
            panes = panes.replacing(targetIndex, pane)
            focusedIndex = targetIndex
        }

    fun isShowing(pane: DetailPane): Boolean = pane in panes

    fun canAddPane(pane: DetailPane): Boolean =
        panes.isNotEmpty() && panes.size < MAX_SPLIT_PANES && panes.none { openPane -> openPane.isSameScreenAs(pane) }

    fun addPane(pane: DetailPane, shouldFocusNewPane: Boolean) {
        if (!canAddPane(pane)) return
        panes = panes + pane
        widthFractions = equalFractions(panes.size)
        if (shouldFocusNewPane) focusedIndex = panes.lastIndex
    }

    fun updateContentLayout(boundsInWindow: Rect, minimumPaneWidthPx: Float, addPaneDropZoneWidthPx: Float) {
        contentBoundsInWindow = boundsInWindow
        this.minimumPaneWidthPx = minimumPaneWidthPx
        this.addPaneDropZoneWidthPx = addPaneDropZoneWidthPx
    }

    fun dropTargetAt(cursorInWindow: Offset): SplitDropTarget? {
        val bounds = contentBoundsInWindow
        if (!bounds.contains(cursorInWindow) || panes.isEmpty()) return null
        if (!isSplit) {
            val isOnLeftHalf = cursorInWindow.x < bounds.left + bounds.width / 2f
            return SplitDropTarget.InsertPane(if (isOnLeftHalf) 0 else 1)
        }
        if (panes.size < MAX_SPLIT_PANES && cursorInWindow.x >= bounds.right - addPaneDropZoneWidthPx) {
            return SplitDropTarget.InsertPane(panes.size)
        }
        return paneIndexAt(cursorInWindow)?.let { index -> SplitDropTarget.ReplacePane(index) }
    }

    fun dropPane(pane: DetailPane, target: SplitDropTarget) {
        val existingIndex = panes.indexOfFirst { openPane -> openPane.isSameScreenAs(pane) }
        when (target) {
            is SplitDropTarget.InsertPane -> {
                if (existingIndex >= 0 || panes.size >= MAX_SPLIT_PANES) return
                val insertIndex = target.index.coerceIn(0, panes.size)
                panes = panes.toMutableList().apply { add(insertIndex, pane) }
                widthFractions = equalFractions(panes.size)
                if (insertIndex <= focusedIndex) focusedIndex += 1
            }
            is SplitDropTarget.ReplacePane -> when {
                existingIndex == target.index -> Unit
                existingIndex >= 0 -> swapPanes(existingIndex, target.index)
                else -> panes = panes.replacing(target.index, pane)
            }
        }
    }

    fun startPaneDrag(index: Int, cursorInWindow: Offset) {
        draggedPaneIndex = index
        paneDragCursorInWindow = cursorInWindow
    }

    fun movePaneDrag(cursorInWindow: Offset) {
        paneDragCursorInWindow = cursorInWindow
    }

    fun finishPaneDrag() {
        val indexBeingDragged = draggedPaneIndex
        val targetIndex = paneDragTargetIndex
        if (indexBeingDragged != null && targetIndex != null) swapPanes(indexBeingDragged, targetIndex)
        draggedPaneIndex = null
    }

    fun cancelPaneDrag() {
        draggedPaneIndex = null
    }

    fun focus(index: Int) {
        if (index in panes.indices) focusedIndex = index
    }

    fun resizeDivider(dividerIndex: Int, deltaPx: Float) {
        val contentWidth = contentBoundsInWindow.width
        if (contentWidth <= 0f || dividerIndex + 1 !in panes.indices) return
        val fractions = visibleWidthFractions.toMutableList()
        val minimumFraction = (minimumPaneWidthPx / contentWidth).coerceAtMost(1f / fractions.size)
        val pairTotal = fractions[dividerIndex] + fractions[dividerIndex + 1]
        val newLeftFraction = (fractions[dividerIndex] + deltaPx / contentWidth)
            .coerceIn(minimumFraction, pairTotal - minimumFraction)
        fractions[dividerIndex] = newLeftFraction
        fractions[dividerIndex + 1] = pairTotal - newLeftFraction
        widthFractions = fractions
    }

    fun closePane(index: Int) {
        if (!isSplit || index !in panes.indices) return
        val remainingFractions = widthFractions.filterIndexed { fractionIndex, _ -> fractionIndex != index }
        val remainingTotal = remainingFractions.sum()
        panes = panes.filterIndexed { paneIndex, _ -> paneIndex != index }
        widthFractions = if (panes.size == 1 || remainingTotal <= 0f) equalFractions(panes.size)
        else remainingFractions.map { fraction -> fraction / remainingTotal }
        focusedIndex = when {
            index < focusedIndex -> focusedIndex - 1
            index == focusedIndex -> (index - 1).coerceAtLeast(0)
            else -> focusedIndex
        }
    }

    fun closeUnfocusedPanesWhere(shouldClose: (DetailPane) -> Boolean) {
        val focused = focusedPane ?: return
        val indexesToClose = panes.indices.filter { index -> index != focusedIndex && shouldClose(panes[index]) }
        indexesToClose.asReversed().forEach { index -> closePane(index) }
        focusedIndex = panes.indexOf(focused).coerceAtLeast(0)
    }

    fun exitSplitAndShow(pane: DetailPane) {
        panes = listOf(pane)
        focusedIndex = 0
        widthFractions = listOf(1f)
    }

    fun restoreSplit(saved: SavedSplitLayout) {
        val uniquePanes = saved.panes.filterIndexed { index, pane ->
            saved.panes.subList(0, index).none { earlierPane -> earlierPane.isSameScreenAs(pane) }
        }
        val layout = saved.keepingOnly(uniquePanes)
        panes = layout.panes
        focusedIndex = layout.focusedIndex.coerceIn(0, layout.panes.lastIndex)
        widthFractions = layout.widthFractions
    }

    fun encodeOpenedState(): String? {
        val focused = focusedPane ?: return null
        if (!isSplit) return focused.encode()
        return SAVED_SPLIT_PREFIX + (
            listOf(focusedIndex.toString(), widthFractions.joinToString(SAVED_FRACTION_SEPARATOR)) +
                panes.map { pane -> pane.encode() }
            ).joinToString(SAVED_SPLIT_SEPARATOR)
    }

    private fun paneIndexAt(cursorInWindow: Offset): Int? {
        val bounds = contentBoundsInWindow
        if (!bounds.contains(cursorInWindow)) return null
        var paneRightEdge = bounds.left
        visibleWidthFractions.forEachIndexed { index, fraction ->
            paneRightEdge += bounds.width * fraction
            if (cursorInWindow.x < paneRightEdge) return index
        }
        return panes.lastIndex.takeIf { it >= 0 }
    }

    private fun swapPanes(firstIndex: Int, secondIndex: Int) {
        panes = panes.toMutableList().apply {
            val firstPane = this[firstIndex]
            this[firstIndex] = this[secondIndex]
            this[secondIndex] = firstPane
        }
        widthFractions = widthFractions.toMutableList().apply {
            val firstFraction = this[firstIndex]
            this[firstIndex] = this[secondIndex]
            this[secondIndex] = firstFraction
        }
        focusedIndex = when (focusedIndex) {
            firstIndex -> secondIndex
            secondIndex -> firstIndex
            else -> focusedIndex
        }
    }
}

fun decodeSavedSplitLayout(raw: String, today: LocalDate): SavedSplitLayout? {
    if (!raw.startsWith(SAVED_SPLIT_PREFIX)) return null
    val parts = raw.removePrefix(SAVED_SPLIT_PREFIX).split(SAVED_SPLIT_SEPARATOR)
    val panes = parts.drop(2).map { encodedPane -> decodeDetailPane(encodedPane, today) }
    if (panes.size !in 2..MAX_SPLIT_PANES) return null
    val focusedIndex = parts[0].toIntOrNull()?.takeIf { index -> index in panes.indices } ?: return null
    val widthFractions = parts[1].split(SAVED_FRACTION_SEPARATOR).map { fraction -> fraction.toFloatOrNull() ?: return null }
    if (widthFractions.size != panes.size || widthFractions.any { fraction -> fraction <= 0f }) return null
    val fractionTotal = widthFractions.sum()
    return SavedSplitLayout(
        panes = panes,
        focusedIndex = focusedIndex,
        widthFractions = widthFractions.map { fraction -> fraction / fractionTotal }
    )
}

@Composable
fun rememberDesktopSplitState() = remember { DesktopSplitState() }

private fun equalFractions(count: Int): List<Float> = List(count) { 1f / count }

private fun List<DetailPane>.replacing(index: Int, pane: DetailPane): List<DetailPane> =
    toMutableList().apply { this[index] = pane }

private fun fractionsRespectingMinimum(fractions: List<Float>, minimumFraction: Float): List<Float> {
    val raisedFractions = fractions.map { fraction -> maxOf(fraction, minimumFraction) }
    val excess = raisedFractions.sum() - 1f
    if (excess <= 0f) return raisedFractions
    val shrinkableTotal = raisedFractions.sumOf { fraction -> (fraction - minimumFraction).toDouble() }.toFloat()
    if (shrinkableTotal <= 0f) return equalFractions(fractions.size)
    return raisedFractions.map { fraction -> fraction - (fraction - minimumFraction) / shrinkableTotal * excess }
}

private fun DetailPane.isSameScreenAs(other: DetailPane): Boolean = when {
    this is DetailPane.Daily && other is DetailPane.Daily -> true
    this in settingsScreenPanes && other in settingsScreenPanes -> true
    else -> this == other
}

private fun DetailPane?.screenKey(): Any? = when {
    this is DetailPane.Daily -> "daily"
    this in settingsScreenPanes -> "settings"
    else -> this
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DesktopSplitLayout(
    splitState: DesktopSplitState,
    hazeState: HazeState,
    dropTarget: SplitDropTarget?,
    onDividerDragEnd: () -> Unit,
    paneContent: @Composable (DetailPane?) -> Unit
) {
    val density = LocalDensity.current
    val minimumPaneWidthPx = with(density) { MIN_SPLIT_PANE_WIDTH.toPx() }
    val addPaneDropZoneWidthPx = with(density) { ADD_PANE_DROP_ZONE_WIDTH.toPx() }
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                splitState.updateContentLayout(coordinates.boundsInWindow(), minimumPaneWidthPx, addPaneDropZoneWidthPx)
            }
    ) {
        val paneFractions = splitState.visibleWidthFractions
        val halfDividerWidth = SPLIT_DIVIDER_WIDTH / 2
        val panes = splitState.panes

        Row(Modifier.fillMaxSize()) {
            if (panes.isEmpty()) paneContent(null)
            panes.forEachIndexed { paneIndex, pane ->
                val paneEdgePadding = Modifier.padding(
                    start = if (paneIndex > 0) halfDividerWidth else 0.dp,
                    end = if (paneIndex < panes.lastIndex) halfDividerWidth else 0.dp
                )
                key(pane.screenKey()) {
                    var isPointerInHeader by remember { mutableStateOf(false) }
                    Box(
                        Modifier
                            .weight(paneFractions.getOrElse(paneIndex) { 1f / panes.size })
                            .fillMaxHeight()
                            .then(paneEdgePadding)
                            .onPointerEvent(PointerEventType.Press, PointerEventPass.Initial) { splitState.focus(paneIndex) }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val pointerY = event.changes.first().position.y
                                        isPointerInHeader = event.type != PointerEventType.Exit &&
                                                pointerY <= SPLIT_HEADER_HOVER_HEIGHT.toPx()
                                    }
                                }
                            }
                    ) {
                        Box(Modifier.fillMaxSize().hazeSource(state = hazeState)) {
                            paneContent(pane)
                        }
                        if (splitState.isSplit) {
                            SplitPaneHandle(
                                isExpanded = isPointerInHeader || splitState.draggedPaneIndex == paneIndex,
                                isFocused = paneIndex == splitState.focusedIndex,
                                paneIndex = paneIndex,
                                splitState = splitState,
                                hazeState = hazeState,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = SPLIT_PILL_TOP_GAP)
                            )
                        }
                    }
                }
            }
        }

        if (splitState.isSplit) {
            var dividerCenterFraction = 0f
            for (dividerIndex in 0 until panes.lastIndex) {
                dividerCenterFraction += paneFractions.getOrElse(dividerIndex) { 0f }
                SplitDivider(
                    centerOffset = maxWidth * dividerCenterFraction,
                    onDrag = { deltaPx -> splitState.resizeDivider(dividerIndex, deltaPx) },
                    onDragEnd = onDividerDragEnd
                )
            }
        }

        val paneDragTargetIndex by remember { derivedStateOf { splitState.paneDragTargetIndex } }
        val paneDragTarget = paneDragTargetIndex?.let { index -> SplitDropTarget.ReplacePane(index) }
        if (dropTarget != null || splitState.draggedPaneIndex != null) {
            SplitDropOverlay(
                hoveredTarget = dropTarget ?: paneDragTarget,
                paneFractions = if (splitState.isSplit) paneFractions else listOf(0.5f, 0.5f),
                isSplit = splitState.isSplit,
                showsAddPaneZone = splitState.isSplit && panes.size < MAX_SPLIT_PANES && splitState.draggedPaneIndex == null
            )
        }
    }
}

@Composable
private fun SplitDivider(centerOffset: Dp, onDrag: (Float) -> Unit, onDragEnd: () -> Unit) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    Box(
        modifier = Modifier
            .offset(x = centerOffset - SPLIT_DIVIDER_WIDTH / 2)
            .width(SPLIT_DIVIDER_WIDTH)
            .fillMaxHeight()
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = onDragEnd) { change, dragAmount ->
                    change.consume()
                    currentOnDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 12.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        )
    }
}

@Composable
private fun SplitDropOverlay(
    hoveredTarget: SplitDropTarget?,
    paneFractions: List<Float>,
    isSplit: Boolean,
    showsAddPaneZone: Boolean
) {
    Box(Modifier.fillMaxSize().padding(SPLIT_DROP_ZONE_GAP / 2)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SPLIT_DROP_ZONE_GAP)
        ) {
            paneFractions.forEachIndexed { zoneIndex, fraction ->
                val zoneTarget = if (isSplit) SplitDropTarget.ReplacePane(zoneIndex) else SplitDropTarget.InsertPane(zoneIndex)
                SplitDropZone(
                    isHovered = zoneTarget == hoveredTarget,
                    modifier = Modifier.weight(fraction).fillMaxHeight()
                )
            }
        }
        if (showsAddPaneZone) {
            SplitDropZone(
                isHovered = hoveredTarget == SplitDropTarget.InsertPane(paneFractions.size),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(ADD_PANE_DROP_ZONE_WIDTH - SPLIT_DROP_ZONE_GAP / 2)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SplitDropZone(isHovered: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(SPLIT_DROP_ZONE_SHAPE)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isHovered) 0.12f else 0.03f))
            .border(
                width = if (isHovered) 1.5.dp else 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isHovered) 0.6f else 0.15f),
                shape = SPLIT_DROP_ZONE_SHAPE
            )
    )
}

@Composable
private fun SplitPaneHandle(
    isExpanded: Boolean,
    isFocused: Boolean,
    paneIndex: Int,
    splitState: DesktopSplitState,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val lineColor =
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val pillBorderColor = emberrBorderColor
    val iconTint =
        if (isFocused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val transition = updateTransition(targetState = isExpanded, label = "split_pane_handle")
    val handleWidth by transition.animateDp(
        transitionSpec = { spring(dampingRatio = NOTCH_SPRING_DAMPING, stiffness = NOTCH_SPRING_STIFFNESS) },
        label = "split_handle_width"
    ) { expanded -> if (expanded) SPLIT_PILL_WIDTH else SPLIT_HANDLE_LINE_WIDTH }
    val handleHeight by transition.animateDp(
        transitionSpec = { spring(dampingRatio = NOTCH_SPRING_DAMPING, stiffness = NOTCH_SPRING_STIFFNESS) },
        label = "split_handle_height"
    ) { expanded -> if (expanded) SPLIT_PILL_HEIGHT else SPLIT_HANDLE_LINE_HEIGHT }
    val lineAlpha by transition.animateFloat(
        transitionSpec = { tween(NOTCH_COLOR_MILLIS) },
        label = "split_handle_line_alpha"
    ) { expanded -> if (expanded) 0f else 1f }
    val buttonsAlpha by transition.animateFloat(
        transitionSpec = {
            if (targetState) tween(NOTCH_BUTTONS_FADE_IN_MILLIS, delayMillis = NOTCH_BUTTONS_FADE_IN_DELAY_MILLIS)
            else tween(NOTCH_BUTTONS_FADE_OUT_MILLIS)
        },
        label = "split_handle_buttons_alpha"
    ) { expanded -> if (expanded) 1f else 0f }

    Box(
        modifier = modifier
            .keepTitleBarHiddenWhileHovered()
            .size(width = SPLIT_PILL_WIDTH, height = SPLIT_PILL_HEIGHT),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    val shapeSize = Size(handleWidth.toPx(), handleHeight.toPx())
                    val shapeTopLeft = Offset((size.width - shapeSize.width) / 2f, 0f)
                    val borderWidth = ceil(EmberrBorderWidth.toPx())
                    val halfBorderWidth = borderWidth / 2f
                    drawRoundRect(
                        color = pillBorderColor.copy(alpha = pillBorderColor.alpha * buttonsAlpha),
                        topLeft = shapeTopLeft + Offset(halfBorderWidth, halfBorderWidth),
                        size = Size(shapeSize.width - borderWidth, shapeSize.height - borderWidth),
                        cornerRadius = CornerRadius((shapeSize.height - borderWidth) / 2f),
                        style = Stroke(width = borderWidth)
                    )
                }
                .graphicsLayer {
                    shape = NotchShape(widthPx = handleWidth.toPx(), heightPx = handleHeight.toPx())
                    clip = true
                    shadowElevation = EmberrShadowElevation.Standard.toPx() * buttonsAlpha
                    spotShadowColor = EmberrShadowSpotColor
                    ambientShadowColor = EmberrShadowAmbientColor
                }
                .emberrBlur(hazeState, EmberrBlur.Regular)
                .drawBehind {
                    val shapeSize = Size(handleWidth.toPx(), handleHeight.toPx())
                    val shapeTopLeft = Offset((size.width - shapeSize.width) / 2f, 0f)
                    val shapeCornerRadius = CornerRadius(shapeSize.height / 2f)
                    drawRoundRect(
                        color = lineColor.copy(alpha = lineColor.alpha * lineAlpha),
                        topLeft = shapeTopLeft,
                        size = shapeSize,
                        cornerRadius = shapeCornerRadius
                    )
                }
        )
        Row(
            modifier = Modifier.graphicsLayer { alpha = buttonsAlpha },
            verticalAlignment = Alignment.CenterVertically
        ) {
            SplitPillDragHandle(tint = iconTint, isEnabled = isExpanded, paneIndex = paneIndex, splitState = splitState)
            SplitPillButton(painterResource(Res.drawable.x), "Close this screen", iconTint, isExpanded) { splitState.closePane(paneIndex) }
        }
    }
}

private class NotchShape(private val widthPx: Float, private val heightPx: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val left = (size.width - widthPx) / 2f
        return Outline.Rounded(
            RoundRect(left = left, top = 0f, right = left + widthPx, bottom = heightPx, cornerRadius = CornerRadius(heightPx / 2f))
        )
    }
}

@Composable
private fun SplitPillDragHandle(
    tint: Color,
    isEnabled: Boolean,
    paneIndex: Int,
    splitState: DesktopSplitState
) {
    var handleOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = Modifier
            .size(SPLIT_PILL_BUTTON_SIZE)
            .onGloballyPositioned { coordinates -> handleOriginInWindow = coordinates.positionInWindow() }
            .pointerHoverIcon(PointerIcon(Cursor(Cursor.MOVE_CURSOR)))
            .pointerInput(isEnabled, paneIndex) {
                if (!isEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { startPosition -> splitState.startPaneDrag(paneIndex, handleOriginInWindow + startPosition) },
                    onDrag = { change, _ ->
                        change.consume()
                        splitState.movePaneDrag(handleOriginInWindow + change.position)
                    },
                    onDragEnd = splitState::finishPaneDrag,
                    onDragCancel = splitState::cancelPaneDrag
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = "Drag to swap screens",
            tint = tint,
            modifier = Modifier.size(SPLIT_PILL_ICON_SIZE)
        )
    }
}

@Composable
private fun SplitPillButton(
    icon: Painter,
    contentDescription: String,
    tint: Color,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(SPLIT_PILL_BUTTON_SIZE)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = NoRippleIndicationNodeFactory,
                enabled = isEnabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(SPLIT_PILL_ICON_SIZE))
    }
}
