package com.emberr.presentation.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.prefs.SyncConstants
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.shared.editor.DefaultKeyboardToolbarItems
import com.emberr.presentation.shared.editor.KeyboardToolbarItem
import com.emberr.presentation.shared.editor.KeyboardToolbarItemIcon
import com.emberr.presentation.shared.editor.keyboardToolbarItemsFrom
import com.emberr.presentation.shared.editor.keyboardToolbarSavedValueOf
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.circle_minus
import emberr.shared.generated.resources.circle_plus
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private data class ToolbarEntry(val id: Int, val item: KeyboardToolbarItem)

private val ToolbarEntryRowHeight = 52.dp
private val DragHandleWidth = 48.dp
private val ToolbarEntryShape = RoundedCornerShape(12.dp)
private val AutoScrollEdgeHeight = 72.dp
private val AutoScrollStep = 9.dp
private val RowSettleAnimationSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

@Composable
fun KeyboardToolbarSettingsScreen(
    onNavigateBack: () -> Unit,
    settingsManager: SettingsManager = koinInject()
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    val listState = rememberLazyListState()
    var listBoundsInWindow by remember { mutableStateOf(Rect.Zero) }

    val toolbarEntries = remember {
        keyboardToolbarItemsFrom(settingsManager.getKeyboardToolbarItems())
            .mapIndexed { index, item -> ToolbarEntry(id = index, item = item) }
            .toMutableStateList()
    }
    var nextEntryId by remember { mutableIntStateOf(toolbarEntries.size) }
    val availableItems = KeyboardToolbarItem.entries.filter { item ->
        item == KeyboardToolbarItem.DIVIDER || toolbarEntries.none { it.item == item }
    }

    val saveToolbarEntries: () -> Unit = {
        settingsManager.saveKeyboardToolbarItems(keyboardToolbarSavedValueOf(toolbarEntries.map { it.item }))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { listBoundsInWindow = it.boundsInWindow() }
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(top = topBarHeightDp + 8.dp, bottom = 48.dp)
        ) {
            item(key = "in_toolbar") {
                SettingsGroup(title = "In Toolbar") {
                    if (toolbarEntries.isEmpty()) {
                        Text(
                            text = "No buttons yet. Add some from the list below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        ReorderableToolbarEntries(
                            entries = toolbarEntries,
                            listState = listState,
                            visibleTopInWindow = listBoundsInWindow.top + topBarHeightPx,
                            visibleBottomInWindow = listBoundsInWindow.bottom,
                            onRemove = { entry ->
                                toolbarEntries.remove(entry)
                                saveToolbarEntries()
                            },
                            onOrderChanged = saveToolbarEntries
                        )
                    }
                    SettingsFootnote(text = "Drag the handle to reorder. The close keyboard button always stays on the right.")
                }
            }

            item(key = "available") {
                SettingsGroup(title = "Available") {
                    availableItems.forEachIndexed { index, item ->
                        if (index > 0) SettingsDivider()
                        AvailableToolbarItemRow(
                            item = item,
                            onAdd = {
                                toolbarEntries.add(ToolbarEntry(id = nextEntryId, item = item))
                                nextEntryId++
                                saveToolbarEntries()
                            }
                        )
                    }
                }
            }

            item(key = "reset") {
                EmberrButtonSecondary(
                    text = "Reset to Default",
                    onClick = {
                        toolbarEntries.clear()
                        DefaultKeyboardToolbarItems.forEach { item ->
                            toolbarEntries.add(ToolbarEntry(id = nextEntryId, item = item))
                            nextEntryId++
                        }
                        settingsManager.saveKeyboardToolbarItems(SyncConstants.DEFAULT_KEYBOARD_TOOLBAR_ITEMS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 22.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topBarHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = "Keyboard Toolbar",
                hazeState = hazeState,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                backTooltip = "Back",
                onBackClick = onNavigateBack
            )
        }
    }
}

@Composable
private fun ReorderableToolbarEntries(
    entries: SnapshotStateList<ToolbarEntry>,
    listState: LazyListState,
    visibleTopInWindow: Float,
    visibleBottomInWindow: Float,
    onRemove: (ToolbarEntry) -> Unit,
    onOrderChanged: () -> Unit
) {
    val settleOffsetsY = remember { mutableStateMapOf<Int, Float>() }
    val settleJobs = remember { mutableMapOf<Int, Job>() }
    val dragScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var listTopInWindow by remember { mutableFloatStateOf(0f) }
    var draggedEntryId by remember { mutableStateOf<Int?>(null) }
    var releasingEntryId by remember { mutableStateOf<Int?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }
    var pointerYInWindow by remember { mutableFloatStateOf(0f) }
    var pointerYInList by remember { mutableFloatStateOf(0f) }
    var liftedOffsetY by remember { mutableFloatStateOf(0f) }
    var releaseJob by remember { mutableStateOf<Job?>(null) }

    val rowHeightPx = with(density) { ToolbarEntryRowHeight.toPx() }
    val autoScrollEdgeHeightPx = with(density) { AutoScrollEdgeHeight.toPx() }
    val autoScrollStepPx = with(density) { AutoScrollStep.toPx() }

    val slideRowIntoNewSlot: (Int, Float) -> Unit = { entryId, distanceFromNewSlot ->
        settleJobs[entryId]?.cancel()
        val startOffsetY = (settleOffsetsY[entryId] ?: 0f) + distanceFromNewSlot
        settleOffsetsY[entryId] = startOffsetY
        settleJobs[entryId] = dragScope.launch {
            animate(initialValue = startOffsetY, targetValue = 0f, animationSpec = RowSettleAnimationSpec) { value, _ ->
                settleOffsetsY[entryId] = value
            }
        }
    }

    val finishDragging: (Int) -> Unit = { entryId ->
        val startIndex = dragStartIndex
        val finalIndex = entries.indexOfFirst { it.id == entryId }
        draggedEntryId = null
        releasingEntryId = entryId
        dragStartIndex = -1
        releaseJob?.cancel()
        releaseJob = dragScope.launch {
            animate(initialValue = liftedOffsetY, targetValue = 0f, animationSpec = RowSettleAnimationSpec) { value, _ ->
                liftedOffsetY = value
            }
            releasingEntryId = null
        }
        if (startIndex != -1 && finalIndex != -1 && startIndex != finalIndex) onOrderChanged()
    }

    LaunchedEffect(draggedEntryId) {
        val entryId = draggedEntryId ?: return@LaunchedEffect
        while (true) {
            withFrameNanos {
                val autoScrollStep = when {
                    pointerYInWindow > visibleBottomInWindow - autoScrollEdgeHeightPx -> autoScrollStepPx
                    pointerYInWindow < visibleTopInWindow + autoScrollEdgeHeightPx -> -autoScrollStepPx
                    else -> 0f
                }
                if (autoScrollStep != 0f) pointerYInList += listState.dispatchRawDelta(autoScrollStep)

                val fromIndex = entries.indexOfFirst { it.id == entryId }
                if (fromIndex == -1) return@withFrameNanos

                val listHeight = entries.size * rowHeightPx
                val toIndex = (pointerYInList.coerceIn(0f, listHeight - 1f) / rowHeightPx).toInt()
                if (toIndex != fromIndex) {
                    val isMovingDown = toIndex > fromIndex
                    val rowsMakingSpace =
                        if (isMovingDown) entries.subList(fromIndex + 1, toIndex + 1).map { it.id }
                        else entries.subList(toIndex, fromIndex).map { it.id }
                    val distanceOfMovingRowFromNewSlot = if (isMovingDown) rowHeightPx else -rowHeightPx

                    entries.add(toIndex, entries.removeAt(fromIndex))
                    rowsMakingSpace.forEach { id -> slideRowIntoNewSlot(id, distanceOfMovingRowFromNewSlot) }
                }

                val liftedRowCenterY = pointerYInList.coerceIn(rowHeightPx / 2, listHeight - rowHeightPx / 2)
                liftedOffsetY = liftedRowCenterY - (toIndex * rowHeightPx + rowHeightPx / 2)
            }
        }
    }

    Column(modifier = Modifier.onGloballyPositioned { listTopInWindow = it.positionInWindow().y }) {
        entries.forEachIndexed { index, entry ->
            key(entry.id) {
                val isLifted = draggedEntryId == entry.id || releasingEntryId == entry.id

                DisposableEffect(Unit) {
                    onDispose {
                        settleOffsetsY.remove(entry.id)
                        settleJobs.remove(entry.id)?.cancel()
                    }
                }

                Box(modifier = Modifier.zIndex(if (isLifted) 1f else 0f)) {
                    ToolbarEntryRow(
                        item = entry.item,
                        showsDividerAbove = index > 0 && !isLifted,
                        isLifted = isLifted,
                        translationProvider = {
                            if (isLifted) liftedOffsetY else settleOffsetsY[entry.id] ?: 0f
                        },
                        dragHandleModifier = Modifier.pointerInput(entry.id) {
                            detectDragGestures(
                                onDragStart = {
                                    releaseJob?.cancel()
                                    releasingEntryId = null
                                    liftedOffsetY = 0f
                                    val startIndex = entries.indexOfFirst { it.id == entry.id }
                                    dragStartIndex = startIndex
                                    pointerYInList = startIndex * rowHeightPx + rowHeightPx / 2
                                    pointerYInWindow = listTopInWindow + pointerYInList
                                    draggedEntryId = entry.id
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    pointerYInWindow += dragAmount.y
                                    pointerYInList += dragAmount.y
                                },
                                onDragEnd = { finishDragging(entry.id) },
                                onDragCancel = { finishDragging(entry.id) }
                            )
                        },
                        onRemove = { onRemove(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolbarEntryRow(
    item: KeyboardToolbarItem,
    showsDividerAbove: Boolean,
    isLifted: Boolean,
    translationProvider: () -> Float,
    dragHandleModifier: Modifier,
    onRemove: () -> Unit
) {
    val liftProgress by animateFloatAsState(
        targetValue = if (isLifted) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "ToolbarEntryLift"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ToolbarEntryRowHeight)
            .graphicsLayer {
                translationY = translationProvider()
                shadowElevation = 8.dp.toPx() * liftProgress
                shape = ToolbarEntryShape
                clip = true
            }
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (showsDividerAbove) SettingsDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ToolbarEntryRowHeight)
                .padding(end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = dragHandleModifier
                    .width(DragHandleWidth)
                    .height(ToolbarEntryRowHeight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DragIndicator,
                    contentDescription = "Reorder ${item.title}",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            ToolbarItemPreview(item)

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            IconTooltip(text = "Remove") {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onRemove),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(Res.drawable.circle_minus),
                        contentDescription = "Remove ${item.title}",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AvailableToolbarItemRow(item: KeyboardToolbarItem, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd)
            .height(ToolbarEntryRowHeight)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarItemPreview(item)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painterResource(Res.drawable.circle_plus),
            contentDescription = "Add ${item.title}",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ToolbarItemPreview(item: KeyboardToolbarItem) {
    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        KeyboardToolbarItemIcon(item, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
    }
}
