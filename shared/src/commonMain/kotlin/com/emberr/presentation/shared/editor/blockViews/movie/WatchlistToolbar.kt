package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistSortKey
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.shownViews
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.SelectedOptionBackground
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.funnel
import emberr.shared.generated.resources.kanban
import emberr.shared.generated.resources.list_sort_descending
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.sliders_horizontal
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.unordered_list
import emberr.shared.generated.resources.widget
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val PillShape = RoundedCornerShape(8.dp)
private val HeaderButtonGap = 6.dp
private val HeaderButtonInnerPadding = 8.dp
private val HeaderIconSize = 18.dp
private val TabIconSize = if (isDesktopPlatform) 18.dp else 16.dp
private val AddTitleButtonColor = Color(0xFF4F5B8A)
private const val DesktopInactiveIconAlpha = 0.6f

@Composable
internal fun WatchlistToolbar(
    block: WatchlistBlock,
    editor: WatchlistBlockEditor,
    isEnabled: Boolean,
    isSearchActive: Boolean,
    onSearchClick: () -> Unit,
    onAddTitle: () -> Unit,
    onDelete: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var isSortMenuOpen by remember { mutableStateOf(false) }
    var isFilterMenuOpen by remember { mutableStateOf(false) }
    var isSettingsMenuOpen by remember { mutableStateOf(false) }
    val sortIsChanged = block.sortKey != WatchlistSortKey.STATUS || block.sortsDescending

    val shownViews = block.shownViews()
    val viewsNotAdded = WatchlistView.entries - shownViews.toSet()
    val viewDragState = remember { WatchlistViewDragState() }
    val latestShownViews by rememberUpdatedState(shownViews)
    val dropLineColor = MaterialTheme.colorScheme.primary

    fun dropDraggedView() {
        val draggedView = viewDragState.draggedView ?: return
        val viewsNow = latestShownViews
        val beforeView = viewToDropBefore(viewsNow, viewDragState.pillSpans, viewDragState.pointerX)
        viewDragState.reset()
        if (!dropKeepsViewInPlace(viewsNow, draggedView, beforeView)) {
            editor.changeSettings(block.id) { watchlist ->
                watchlist.copy(addedViews = viewsWithDroppedView(watchlist.shownViews(), draggedView, beforeView))
            }
        }
    }
    val viewTabs: @Composable () -> Unit = {
        shownViews.forEach { view ->
            WatchlistViewTab(
                view = view,
                isActive = block.view == view,
                canRemove = shownViews.size > 1,
                isEnabled = isEnabled,
                dragState = viewDragState,
                onDrop = ::dropDraggedView,
                onShow = { editor.changeSettings(block.id) { it.copy(view = view) } },
                onRemove = {
                    editor.changeSettings(block.id) { watchlist ->
                        val remainingViews = watchlist.shownViews() - view
                        watchlist.copy(addedViews = remainingViews, view = remainingViews.first())
                    }
                },
                runAfterKeyboardCloses = runAfterKeyboardCloses
            )
        }
        if (viewsNotAdded.isNotEmpty()) {
            AddViewButton(
                viewsNotAdded = viewsNotAdded,
                isEnabled = isEnabled,
                onAdd = { view ->
                    editor.changeSettings(block.id) { watchlist ->
                        watchlist.copy(addedViews = watchlist.shownViews() + view, view = view)
                    }
                },
                runAfterKeyboardCloses = runAfterKeyboardCloses
            )
        }
    }
    val searchButton: @Composable () -> Unit = {
        WatchlistHeaderButton(
            label = "Search",
            icon = Res.drawable.search,
            isActive = isSearchActive,
            isEnabled = isEnabled,
            onClick = onSearchClick
        )
    }
    val actionButtons: @Composable () -> Unit = {
        Box {
            WatchlistHeaderButton(
                label = "Sort",
                icon = Res.drawable.list_sort_descending,
                isActive = sortIsChanged,
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isSortMenuOpen = true } }
            )
            WatchlistSortMenu(expanded = isSortMenuOpen, block = block, editor = editor, onDismiss = { isSortMenuOpen = false })
        }
        Box {
            WatchlistHeaderButton(
                label = "Filter",
                icon = Res.drawable.funnel,
                isActive = block.kindFilter != null || block.statusFilter != null,
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isFilterMenuOpen = true } }
            )
            WatchlistFilterMenu(expanded = isFilterMenuOpen, block = block, editor = editor, onDismiss = { isFilterMenuOpen = false })
        }
        Box {
            WatchlistHeaderButton(
                label = "Settings",
                icon = Res.drawable.sliders_horizontal,
                isActive = false,
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isSettingsMenuOpen = true } }
            )
            MovieMenu(
                expanded = isSettingsMenuOpen,
                title = block.title.ifBlank { "Watchlist" },
                onDismiss = { isSettingsMenuOpen = false }
            ) { closeAnd ->
                MovieMenuOption(
                    label = "Delete watchlist",
                    icon = Res.drawable.trash,
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { closeAnd(onDelete) }
                )
            }
        }
        AddTitleButton(isEnabled = isEnabled, onClick = onAddTitle)
    }

    if (isDesktopPlatform) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HeaderButtonGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f).watchlistViewDropLine(viewDragState, shownViews, dropLineColor),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { viewTabs() }
            searchButton()
            actionButtons()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().watchlistViewDropLine(viewDragState, shownViews, dropLineColor),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { viewTabs() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(HeaderButtonGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                searchButton()
                Spacer(modifier = Modifier.weight(1f))
                actionButtons()
            }
        }
    }
}

@Composable
private fun WatchlistSortMenu(expanded: Boolean, block: WatchlistBlock, editor: WatchlistBlockEditor, onDismiss: () -> Unit) {
    MovieMenu(expanded = expanded, title = "Sort", onDismiss = onDismiss) { closeAnd ->
        MovieMenuSectionLabel("Sort by")
        WatchlistSortKey.entries.forEach { sortKey ->
            MovieMenuOption(
                label = sortKey.label,
                isSelected = block.sortKey == sortKey,
                onClick = {
                    closeAnd {
                        editor.changeSettings(block.id) { it.copy(sortKey = sortKey, sortsDescending = sortKey.sortsDescendingFirst) }
                    }
                }
            )
        }
        MovieMenuDivider()
        MovieMenuSectionLabel("Order")
        listOf(false, true).forEach { descending ->
            MovieMenuOption(
                label = if (descending) "Descending" else "Ascending",
                isSelected = block.sortsDescending == descending,
                onClick = { closeAnd { editor.changeSettings(block.id) { it.copy(sortsDescending = descending) } } }
            )
        }
    }
}

@Composable
private fun WatchlistFilterMenu(expanded: Boolean, block: WatchlistBlock, editor: WatchlistBlockEditor, onDismiss: () -> Unit) {
    MovieMenu(expanded = expanded, title = "Filter", onDismiss = onDismiss) { closeAnd ->
        MovieMenuSectionLabel("Type")
        listOf(null, MovieKind.MOVIE, MovieKind.SHOW).forEach { kind ->
            MovieMenuOption(
                label = kind?.let { "${it.label}s" } ?: "All titles",
                isSelected = block.kindFilter == kind,
                onClick = { closeAnd { editor.changeSettings(block.id) { it.copy(kindFilter = kind) } } }
            )
        }
        MovieMenuDivider()
        MovieMenuSectionLabel("Status")
        (listOf(null) + WatchStatus.entries).forEach { status ->
            MovieMenuOption(
                label = status?.label ?: "Any status",
                isSelected = block.statusFilter == status,
                onClick = { closeAnd { editor.changeSettings(block.id) { it.copy(statusFilter = status) } } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WatchlistFilterChips(block: WatchlistBlock, editor: WatchlistBlockEditor, isEnabled: Boolean) {
    if (block.kindFilter == null && block.statusFilter == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        block.kindFilter?.let { kind ->
            WatchlistFilterChip(label = "${kind.label}s only", isEnabled = isEnabled) {
                editor.changeSettings(block.id) { it.copy(kindFilter = null) }
            }
        }
        block.statusFilter?.let { status ->
            WatchlistFilterChip(label = status.label, isEnabled = isEnabled) {
                editor.changeSettings(block.id) { it.copy(statusFilter = null) }
            }
        }
    }
}

@Composable
private fun WatchlistFilterChip(label: String, isEnabled: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(SelectedOptionBackground)
            .clickable(enabled = isEnabled, onClick = onRemove)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Icon(
            painter = painterResource(Res.drawable.x),
            contentDescription = "Remove filter",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun WatchlistViewTab(
    view: WatchlistView,
    isActive: Boolean,
    canRemove: Boolean,
    isEnabled: Boolean,
    dragState: WatchlistViewDragState,
    onDrop: () -> Unit,
    onShow: () -> Unit,
    onRemove: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val latestOnDrop by rememberUpdatedState(onDrop)
    val latestCanRemove by rememberUpdatedState(canRemove)
    val color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .onPlaced { coordinates ->
                val pillLeft = coordinates.positionInParent().x
                val span = ViewPillSpan(left = pillLeft, right = pillLeft + coordinates.size.width)
                if (dragState.pillSpans[view] != span) dragState.pillSpans[view] = span
            }
            .zIndex(if (dragState.draggedView == view) 1f else 0f)
            .watchlistViewPillDrag(
                dragState = dragState,
                view = view,
                isEnabled = isEnabled,
                onDrop = { latestOnDrop() },
                onLongPressWithoutDrag = { if (latestCanRemove) runAfterKeyboardCloses { isMenuOpen = true } }
            )
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer { translationX = dragState.offsetOf(view) }
                .clip(PillShape)
                .background(if (isActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f) else Color.Transparent)
                .clickable(enabled = isEnabled) {
                    when {
                        !isActive -> onShow()
                        canRemove -> runAfterKeyboardCloses { isMenuOpen = true }
                    }
                }
                .padding(horizontal = HeaderButtonInnerPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(view.iconResource()),
                contentDescription = null,
                tint = desktopInactiveTintOr(color),
                modifier = Modifier.size(TabIconSize)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = view.label,
                style = if (isDesktopPlatform) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        MovieMenu(expanded = isMenuOpen, title = view.label, onDismiss = { isMenuOpen = false }) { closeAnd ->
            MovieMenuOption(
                label = "Remove view",
                icon = Res.drawable.trash,
                labelColor = MaterialTheme.colorScheme.error,
                onClick = { closeAnd(onRemove) }
            )
        }
    }
}

@Composable
private fun AddViewButton(
    viewsNotAdded: List<WatchlistView>,
    isEnabled: Boolean,
    onAdd: (WatchlistView) -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    Box {
        IconTooltip(text = "Add view") {
            Icon(
                painter = painterResource(Res.drawable.plus),
                contentDescription = "Add view",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(enabled = isEnabled) { runAfterKeyboardCloses { isMenuOpen = true } }
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
        MovieMenu(expanded = isMenuOpen, title = "Add view", onDismiss = { isMenuOpen = false }) { closeAnd ->
            viewsNotAdded.forEach { view ->
                MovieMenuOption(
                    label = view.label,
                    icon = view.iconResource(),
                    onClick = { closeAnd { onAdd(view) } }
                )
            }
        }
    }
}

@Composable
private fun WatchlistHeaderButton(
    label: String,
    icon: DrawableResource,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    IconTooltip(text = if (isDesktopPlatform) null else label) {
        Row(
            modifier = Modifier
                .clip(PillShape)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = HeaderButtonInnerPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDesktopPlatform) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (isActive) color else desktopInactiveTintOr(color),
                    modifier = Modifier.size(HeaderIconSize)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1)
            } else {
                Icon(painter = painterResource(icon), contentDescription = label, tint = color, modifier = Modifier.size(HeaderIconSize))
            }
        }
    }
}

@Composable
private fun AddTitleButton(isEnabled: Boolean, onClick: () -> Unit) {
    IconTooltip(text = "Add title") {
        Icon(
            painter = painterResource(Res.drawable.plus),
            contentDescription = "Add title",
            tint = Color.White,
            modifier = Modifier
                .padding(start = HeaderButtonInnerPadding)
                .clip(PillShape)
                .background(AddTitleButtonColor)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 5.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun desktopInactiveTintOr(mobileTint: Color): Color =
    if (isDesktopPlatform) MaterialTheme.colorScheme.onSurface.copy(alpha = DesktopInactiveIconAlpha) else mobileTint

private fun WatchlistView.iconResource(): DrawableResource = when (this) {
    WatchlistView.LIST -> Res.drawable.unordered_list
    WatchlistView.BOARD -> Res.drawable.kanban
    WatchlistView.GALLERY -> Res.drawable.widget
}
