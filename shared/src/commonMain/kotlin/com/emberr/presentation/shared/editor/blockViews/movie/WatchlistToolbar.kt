package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistSortKey
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.WatchlistLoadLimitChoices
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.NewItemButtonColor
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.ViewPillDragState
import com.emberr.presentation.shared.components.ViewPillSpan
import com.emberr.presentation.shared.components.dropKeepsViewInPlace
import com.emberr.presentation.shared.components.subtleFillColor
import com.emberr.presentation.shared.components.viewDropLine
import com.emberr.presentation.shared.components.viewPillDragGesture
import com.emberr.presentation.shared.components.viewToDropBefore
import com.emberr.presentation.shared.components.viewsWithDroppedView
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_down
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.funnel
import emberr.shared.generated.resources.kanban
import emberr.shared.generated.resources.list_sort_descending
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.sliders_horizontal
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.unordered_list
import emberr.shared.generated.resources.widget
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_save
import emberr.shared.generated.resources.editor_block_watchlist
import emberr.shared.generated.resources.movie_watchlist_add_title
import emberr.shared.generated.resources.movie_watchlist_add_view
import emberr.shared.generated.resources.movie_watchlist_ascending
import emberr.shared.generated.resources.movie_watchlist_delete_view
import emberr.shared.generated.resources.movie_watchlist_delete_watchlist
import emberr.shared.generated.resources.movie_watchlist_descending
import emberr.shared.generated.resources.movie_watchlist_duplicate
import emberr.shared.generated.resources.movie_watchlist_filter
import emberr.shared.generated.resources.movie_watchlist_load_limit
import emberr.shared.generated.resources.movie_watchlist_order
import emberr.shared.generated.resources.movie_watchlist_rename
import emberr.shared.generated.resources.movie_watchlist_rename_view
import emberr.shared.generated.resources.movie_watchlist_search
import emberr.shared.generated.resources.movie_watchlist_settings
import emberr.shared.generated.resources.movie_watchlist_sort
import emberr.shared.generated.resources.movie_watchlist_sort_by
import emberr.shared.generated.resources.movie_watchlist_titles_count
import emberr.shared.generated.resources.movie_watchlist_view_name
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

private val HeaderButtonGap = 6.dp
private val HeaderButtonInnerPadding = 8.dp
private val HeaderIconSize = 18.dp
private val TabIconSize = if (isDesktopPlatform) 18.dp else 16.dp
private const val DesktopInactiveIconAlpha = 0.6f

@Composable
internal fun WatchlistToolbar(
    block: WatchlistBlock,
    views: List<WatchlistViewSettings>,
    openView: WatchlistViewSettings,
    titles: List<MovieBlock>,
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
    val sortIsChanged = openView.sortKey != WatchlistSortKey.STATUS || openView.sortsDescending

    val viewIds = views.map { it.id }
    val viewDragState = remember { ViewPillDragState() }
    val latestViewIds by rememberUpdatedState(viewIds)
    val dropLineColor = MaterialTheme.colorScheme.primary

    fun dropDraggedView() {
        val draggedViewId = viewDragState.draggedViewId ?: return
        val viewIdsNow = latestViewIds
        val beforeViewId = viewToDropBefore(viewIdsNow, viewDragState.pillSpans, viewDragState.pointerX)
        viewDragState.reset()
        if (!dropKeepsViewInPlace(viewIdsNow, draggedViewId, beforeViewId)) {
            editor.reorderViews(block.id, viewsWithDroppedView(viewIdsNow, draggedViewId, beforeViewId))
        }
    }
    val viewTabs: @Composable () -> Unit = {
        views.forEach { view ->
            key(view.id) {
                WatchlistViewTab(
                    view = view,
                    isActive = view.id == openView.id,
                    canDelete = views.size > 1,
                    isEnabled = isEnabled,
                    dragState = viewDragState,
                    onDrop = ::dropDraggedView,
                    onShow = { editor.showView(block.id, view.id) },
                    onRename = { name -> editor.changeView(block.id, view.id) { it.copy(name = name) } },
                    onDuplicate = { editor.duplicateView(block, view) },
                    onDelete = { editor.removeView(block.id, view.id) },
                    runAfterKeyboardCloses = runAfterKeyboardCloses
                )
            }
        }
        AddViewButton(
            isEnabled = isEnabled,
            onAdd = { layout -> editor.addView(block, layout) },
            runAfterKeyboardCloses = runAfterKeyboardCloses
        )
    }
    val searchButton: @Composable () -> Unit = {
        WatchlistHeaderButton(
            label = stringResource(Res.string.movie_watchlist_search),
            icon = Res.drawable.search,
            isActive = isSearchActive,
            isEnabled = isEnabled,
            onClick = onSearchClick
        )
    }
    val actionButtons: @Composable () -> Unit = {
        Box {
            WatchlistHeaderButton(
                label = stringResource(Res.string.movie_watchlist_sort),
                icon = Res.drawable.list_sort_descending,
                isActive = sortIsChanged,
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isSortMenuOpen = true } }
            )
            WatchlistSortMenu(expanded = isSortMenuOpen, block = block, openView = openView, editor = editor, onDismiss = { isSortMenuOpen = false })
        }
        Box {
            WatchlistHeaderButton(
                label = stringResource(Res.string.movie_watchlist_filter),
                icon = Res.drawable.funnel,
                isActive = openView.filters.isNotEmpty(),
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isFilterMenuOpen = true } }
            )
            WatchlistFilterMenu(expanded = isFilterMenuOpen, block = block, openView = openView, titles = titles, editor = editor, onDismiss = { isFilterMenuOpen = false })
        }
        Box {
            WatchlistHeaderButton(
                label = stringResource(Res.string.movie_watchlist_settings),
                icon = Res.drawable.sliders_horizontal,
                isActive = false,
                isEnabled = isEnabled,
                onClick = { runAfterKeyboardCloses { isSettingsMenuOpen = true } }
            )
            MovieMenu(
                expanded = isSettingsMenuOpen,
                title = block.title.ifBlank { stringResource(Res.string.editor_block_watchlist) },
                onDismiss = { isSettingsMenuOpen = false }
            ) { closeAnd ->
                MovieMenuLayer(
                    title = stringResource(Res.string.movie_watchlist_load_limit),
                    anchor = { openLayer ->
                        MovieMenuOption(
                            label = stringResource(Res.string.movie_watchlist_load_limit),
                            icon = Res.drawable.arrow_down,
                            trailingText = pluralStringResource(Res.plurals.movie_watchlist_titles_count, openView.loadLimit, openView.loadLimit),
                            onClick = openLayer
                        )
                    }
                ) { closeLayerAnd ->
                    WatchlistLoadLimitChoices.forEach { loadLimit ->
                        MovieMenuOption(
                            label = pluralStringResource(Res.plurals.movie_watchlist_titles_count, loadLimit, loadLimit),
                            isSelected = openView.loadLimit == loadLimit,
                            onClick = { closeLayerAnd { editor.changeView(block.id, openView.id) { it.copy(loadLimit = loadLimit) } } }
                        )
                    }
                }
                MovieMenuOption(
                    label = stringResource(Res.string.movie_watchlist_delete_watchlist),
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
                modifier = Modifier.weight(1f).viewDropLine(viewDragState, viewIds, dropLineColor),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) { viewTabs() }
            searchButton()
            actionButtons()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().viewDropLine(viewDragState, viewIds, dropLineColor),
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
private fun WatchlistSortMenu(
    expanded: Boolean,
    block: WatchlistBlock,
    openView: WatchlistViewSettings,
    editor: WatchlistBlockEditor,
    onDismiss: () -> Unit
) {
    MovieMenu(expanded = expanded, title = stringResource(Res.string.movie_watchlist_sort), onDismiss = onDismiss) { closeAnd ->
        MovieMenuSectionLabel(stringResource(Res.string.movie_watchlist_sort_by))
        WatchlistSortKey.entries.forEach { sortKey ->
            MovieMenuOption(
                label = sortKey.displayName(),
                isSelected = openView.sortKey == sortKey,
                onClick = {
                    closeAnd {
                        editor.changeView(block.id, openView.id) { it.copy(sortKey = sortKey, sortsDescending = sortKey.sortsDescendingFirst) }
                    }
                }
            )
        }
        MovieMenuDivider()
        MovieMenuSectionLabel(stringResource(Res.string.movie_watchlist_order))
        listOf(false, true).forEach { descending ->
            MovieMenuOption(
                label = if (descending) stringResource(Res.string.movie_watchlist_descending) else stringResource(Res.string.movie_watchlist_ascending),
                isSelected = openView.sortsDescending == descending,
                onClick = { closeAnd { editor.changeView(block.id, openView.id) { it.copy(sortsDescending = descending) } } }
            )
        }
    }
}

@Composable
private fun WatchlistViewTab(
    view: WatchlistViewSettings,
    isActive: Boolean,
    canDelete: Boolean,
    isEnabled: Boolean,
    dragState: ViewPillDragState,
    onDrop: () -> Unit,
    onShow: () -> Unit,
    onRename: (String) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val latestOnDrop by rememberUpdatedState(onDrop)
    val color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .onPlaced { coordinates ->
                val pillLeft = coordinates.positionInParent().x
                val span = ViewPillSpan(left = pillLeft, right = pillLeft + coordinates.size.width)
                if (dragState.pillSpans[view.id] != span) dragState.pillSpans[view.id] = span
            }
            .zIndex(if (dragState.draggedViewId == view.id) 1f else 0f)
            .viewPillDragGesture(
                dragState = dragState,
                viewId = view.id,
                isEnabled = isEnabled,
                onDrop = { latestOnDrop() },
                onLongPressWithoutDrag = { runAfterKeyboardCloses { isMenuOpen = true } }
            )
    ) {
        Row(
            modifier = Modifier
                .graphicsLayer { translationX = dragState.offsetOf(view.id) }
                .clip(SmallRoundedCornerShape)
                .background(if (isActive) subtleFillColor() else Color.Transparent)
                .clickable(enabled = isEnabled) {
                    if (isActive) runAfterKeyboardCloses { isMenuOpen = true } else onShow()
                }
                .padding(horizontal = HeaderButtonInnerPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(view.layout.iconResource()),
                contentDescription = null,
                tint = dimmedIconTintOnDesktop(color),
                modifier = Modifier.size(TabIconSize)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = view.name,
                style = if (isDesktopPlatform) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.labelSmall,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
        MovieMenu(expanded = isMenuOpen, title = view.name, onDismiss = { isMenuOpen = false }) { closeAnd ->
            MovieMenuLayer(
                title = stringResource(Res.string.movie_watchlist_rename_view),
                showsCloseButton = false,
                anchor = { openLayer -> MovieMenuOption(label = stringResource(Res.string.movie_watchlist_rename), icon = Res.drawable.pen, onClick = openLayer) }
            ) { closeLayerAnd ->
                WatchlistRenameViewPage(
                    currentName = view.name,
                    onCancel = { closeLayerAnd { } },
                    onRename = { name -> closeLayerAnd { closeAnd { onRename(name) } } }
                )
            }
            MovieMenuOption(label = stringResource(Res.string.movie_watchlist_duplicate), icon = Res.drawable.copy, onClick = { closeAnd(onDuplicate) })
            if (canDelete) {
                MovieMenuOption(
                    label = stringResource(Res.string.movie_watchlist_delete_view),
                    icon = Res.drawable.trash,
                    labelColor = MaterialTheme.colorScheme.error,
                    onClick = { closeAnd(onDelete) }
                )
            }
        }
    }
}

@Composable
private fun WatchlistRenameViewPage(currentName: String, onCancel: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(currentName) }
    val saveName = { if (name.isNotBlank()) onRename(name.trim()) }

    MovieMenuContent {
        EmberrTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = stringResource(Res.string.movie_watchlist_view_name),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            onSubmit = saveName
        )
    }
    MovieMenuButtons(
        cancelText = stringResource(Res.string.common_cancel),
        onCancel = onCancel,
        confirmText = stringResource(Res.string.common_save),
        onConfirm = saveName
    )
}

@Composable
private fun AddViewButton(
    isEnabled: Boolean,
    onAdd: (WatchlistView) -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val addViewLabel = stringResource(Res.string.movie_watchlist_add_view)
    Box {
        IconTooltip(text = addViewLabel) {
            Icon(
                painter = painterResource(Res.drawable.plus),
                contentDescription = addViewLabel,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .clip(SmallRoundedCornerShape)
                    .clickable(enabled = isEnabled) { runAfterKeyboardCloses { isMenuOpen = true } }
                    .padding(6.dp)
                    .size(16.dp)
            )
        }
        MovieMenu(expanded = isMenuOpen, title = addViewLabel, onDismiss = { isMenuOpen = false }) { closeAnd ->
            WatchlistView.entries.forEach { layout ->
                MovieMenuOption(
                    label = layout.displayName(),
                    icon = layout.iconResource(),
                    onClick = { closeAnd { onAdd(layout) } }
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
                .clip(SmallRoundedCornerShape)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = HeaderButtonInnerPadding, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isDesktopPlatform) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (isActive) color else dimmedIconTintOnDesktop(color),
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
    val addTitleLabel = stringResource(Res.string.movie_watchlist_add_title)
    IconTooltip(text = addTitleLabel) {
        Icon(
            painter = painterResource(Res.drawable.plus),
            contentDescription = addTitleLabel,
            tint = Color.White,
            modifier = Modifier
                .padding(start = HeaderButtonInnerPadding)
                .clip(SmallRoundedCornerShape)
                .background(NewItemButtonColor)
                .clickable(enabled = isEnabled, onClick = onClick)
                .padding(horizontal = 7.dp, vertical = 5.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun dimmedIconTintOnDesktop(mobileTint: Color): Color =
    if (isDesktopPlatform) MaterialTheme.colorScheme.onSurface.copy(alpha = DesktopInactiveIconAlpha) else mobileTint

private fun WatchlistView.iconResource(): DrawableResource = when (this) {
    WatchlistView.LIST -> Res.drawable.unordered_list
    WatchlistView.BOARD -> Res.drawable.kanban
    WatchlistView.GALLERY -> Res.drawable.widget
}
