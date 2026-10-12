package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.database.activeView
import com.emberr.domain.database.allViews
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.DatabaseViewType
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.editor.DatabaseBlockEditor
import com.emberr.presentation.shared.editor.GlobalEditorState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.kanban
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.table
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.widget
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal fun DatabaseViewType.iconResource(): DrawableResource = when (this) {
    DatabaseViewType.TABLE -> Res.drawable.table
    DatabaseViewType.GALLERY -> Res.drawable.widget
    DatabaseViewType.BOARD -> Res.drawable.kanban
}

@Composable
internal fun DatabaseViewTabs(
    block: DatabaseBlock,
    editor: DatabaseBlockEditor,
    inSelectionMode: Boolean,
    isLocked: Boolean,
    runAfterKeyboardCloses: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val views = block.allViews()
    val activeViewId = block.activeView().id
    val viewIds = views.map { it.id }
    val latestViewIds by rememberUpdatedState(viewIds)
    val dragState = remember { DatabaseViewDragState() }
    val dropLineColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()
    var viewportWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    LaunchedEffect(dragState.isDragging) {
        if (!dragState.isDragging) return@LaunchedEffect
        val edgeWidthPx = with(density) { ColumnAutoScrollEdgeWidth.toPx() }
        val maxStepPx = with(density) { ColumnAutoScrollMaxStep.toPx() }
        while (true) {
            withFrameNanos { }
            val pointerInViewport = dragState.pointerX - scrollState.value
            val step = columnAutoScrollStep(pointerInViewport, viewportWidth.toFloat(), edgeWidthPx, maxStepPx)
            if (step != 0f) dragState.shiftPointerBy(scrollState.scrollBy(step))
        }
    }

    fun dropDraggedView() {
        val draggedViewId = dragState.draggedViewId ?: return
        val viewIdsNow = latestViewIds
        val beforeViewId = viewToDropBefore(viewIdsNow, dragState.pillSpans, dragState.pointerX)
        dragState.reset()
        if (!dropKeepsViewInPlace(viewIdsNow, draggedViewId, beforeViewId)) {
            editor.moveViewBefore(block.id, draggedViewId, beforeViewId)
        }
    }

    Row(
        modifier = modifier
            .onSizeChanged { viewportWidth = it.width }
            .horizontalScroll(scrollState)
            .viewDropLine(dragState, viewIds, dropLineColor),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        views.forEach { view ->
            key(view.id) {
                DatabaseViewTab(
                    block = block,
                    view = view,
                    isActive = view.id == activeViewId,
                    canDelete = views.size > 1,
                    enabled = !inSelectionMode,
                    canEdit = !isLocked,
                    dragState = dragState,
                    onDrop = ::dropDraggedView,
                    editor = editor,
                    runAfterKeyboardCloses = runAfterKeyboardCloses
                )
            }
        }
        if (!isLocked) {
            DatabaseAddViewButton(block = block, enabled = !inSelectionMode, editor = editor, runAfterKeyboardCloses = runAfterKeyboardCloses)
        }
    }
}

@Composable
private fun DatabaseViewTab(
    block: DatabaseBlock,
    view: DatabaseView,
    isActive: Boolean,
    canDelete: Boolean,
    enabled: Boolean,
    canEdit: Boolean,
    dragState: DatabaseViewDragState,
    onDrop: () -> Unit,
    editor: DatabaseBlockEditor,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    val openMenu = { if (canEdit) runAfterKeyboardCloses { showMenu = true } }
    val latestOpenMenu by rememberUpdatedState(openMenu)
    val latestOnDrop by rememberUpdatedState(onDrop)
    val isDragged = dragState.draggedViewId == view.id
    val pillBackground = when {
        isDragged -> draggedColumnBackground()
        isActive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        else -> Color.Transparent
    }

    DisposableEffect(enabled) {
        onDispose { GlobalEditorState.isPointerOverDatabaseViewPill = false }
    }

    Box(
        modifier = Modifier
            .onPlaced { coordinates ->
                val pillLeft = coordinates.positionInParent().x
                val span = HeaderSpan(left = pillLeft, right = pillLeft + coordinates.size.width)
                if (dragState.pillSpans[view.id] != span) dragState.pillSpans[view.id] = span
            }
            .zIndex(if (isDragged) 1f else 0f)
            .viewPillDragGesture(
                dragState = dragState,
                viewId = view.id,
                isEnabled = enabled && canEdit,
                onDrop = { latestOnDrop() },
                onLongPressWithoutDrag = { latestOpenMenu() }
            )
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer { translationX = dragState.offsetOf(view.id) }
                .clip(RoundedCornerShape(8.dp))
                .background(pillBackground)
                .then(
                    if (isDesktopPlatform && enabled) {
                        Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    when (event.type) {
                                        PointerEventType.Enter -> GlobalEditorState.isPointerOverDatabaseViewPill = true
                                        PointerEventType.Exit -> GlobalEditorState.isPointerOverDatabaseViewPill = false
                                        PointerEventType.Press -> if (event.buttons.isSecondaryPressed) {
                                            event.changes.forEach { it.consume() }
                                            latestOpenMenu()
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Modifier
                    }
                )
                .clickable(enabled = enabled) {
                    if (!isActive) editor.showView(block.id, view.id) else openMenu()
                }
                .padding(horizontal = HeaderButtonInnerPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(view.type.iconResource()), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = view.name.ifBlank { view.type.label },
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }

        DatabaseMenu(expanded = showMenu, title = view.name.ifBlank { view.type.label }, onDismiss = { showMenu = false }) { closeAnd ->
            DatabaseMenuLayer(
                title = "Rename view",
                showsCloseButton = false,
                anchor = { openLayer ->
                    DatabaseMenuOption(label = "Rename", icon = { DatabaseOptionIcon(Res.drawable.pen) }, onClick = openLayer)
                }
            ) { closeLayerAnd ->
                DatabaseRenameViewPage(
                    currentName = view.name,
                    onCancel = { closeLayerAnd { } },
                    onRename = { name -> closeLayerAnd { closeAnd { editor.renameView(block.id, view.id, name) } } }
                )
            }
            if (canDelete) {
                DatabaseMenuOption(
                    label = "Delete view",
                    icon = { DatabaseOptionIcon(Res.drawable.trash, tint = MaterialTheme.colorScheme.error) },
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { closeAnd { editor.deleteView(block.id, view.id) } }
                )
            }
        }
    }
}

@Composable
private fun DatabaseRenameViewPage(currentName: String, onCancel: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    val saveName = { if (name.isNotBlank()) onRename(name.trim()) }

    DatabaseMenuContent {
        EmberrTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "View name",
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            onSubmit = saveName
        )
    }
    DatabaseMenuButtons(cancelText = "Cancel", onCancel = onCancel, confirmText = "Save", onConfirm = saveName)
}

@Composable
private fun DatabaseAddViewButton(
    block: DatabaseBlock,
    enabled: Boolean,
    editor: DatabaseBlockEditor,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Icon(
            painter = painterResource(Res.drawable.plus),
            contentDescription = "Add view",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { runAfterKeyboardCloses { showMenu = true } }
                .padding(6.dp)
                .size(16.dp)
        )

        DatabaseMenu(expanded = showMenu, title = "Add view", onDismiss = { showMenu = false }) { closeAnd ->
            DatabaseViewType.entries.forEach { type ->
                DatabaseMenuOption(
                    label = type.label,
                    icon = { DatabaseOptionIcon(type.iconResource()) },
                    onClick = { closeAnd { editor.addView(block.id, type) } }
                )
            }
        }
    }
}
