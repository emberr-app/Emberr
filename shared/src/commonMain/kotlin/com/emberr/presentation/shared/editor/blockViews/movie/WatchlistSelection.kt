package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import com.emberr.presentation.shared.editor.GlobalEditorState
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.IconTooltip
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
internal class WatchlistSelection {
    var selectedTitleIds by mutableStateOf(emptySet<String>())
        private set
    var isSelecting by mutableStateOf(false)
        private set
    private var rangeAnchorId: String? = null

    fun isSelected(titleId: String): Boolean = titleId in selectedTitleIds

    fun startWith(titleId: String) {
        selectedTitleIds = setOf(titleId)
        isSelecting = true
        rangeAnchorId = titleId
    }

    fun toggle(titleId: String) {
        selectedTitleIds = if (titleId in selectedTitleIds) selectedTitleIds - titleId else selectedTitleIds + titleId
        rangeAnchorId = titleId
    }

    fun ctrlClick(titleId: String) {
        if (isSelecting) toggle(titleId) else startWith(titleId)
    }

    fun shiftClick(titleId: String, titleIdsInShownOrder: List<String>) {
        val anchorIndex = rangeAnchorId?.let(titleIdsInShownOrder::indexOf)?.takeIf { it >= 0 }
        val clickedIndex = titleIdsInShownOrder.indexOf(titleId)
        if (anchorIndex == null || clickedIndex < 0) {
            startWith(titleId)
            return
        }
        val range = titleIdsInShownOrder.subList(minOf(anchorIndex, clickedIndex), maxOf(anchorIndex, clickedIndex) + 1)
        selectedTitleIds = selectedTitleIds + range
        isSelecting = true
    }

    fun keepOnly(titleIds: Collection<String>) {
        val stillShown = selectedTitleIds.intersect(titleIds.toSet())
        if (stillShown.size != selectedTitleIds.size) selectedTitleIds = stillShown
    }

    fun stop() {
        selectedTitleIds = emptySet()
        isSelecting = false
        rangeAnchorId = null
    }
}

internal data class TitleCardSelection(
    val isSelecting: Boolean,
    val isSelected: Boolean,
    val onToggle: () -> Unit,
    val onStartSelecting: () -> Unit,
    val onCtrlClick: () -> Unit,
    val onShiftClick: () -> Unit
)

internal fun WatchlistSelection.forTitle(titleId: String, titleIdsInShownOrder: List<String>): TitleCardSelection =
    TitleCardSelection(
        isSelecting = isSelecting,
        isSelected = isSelected(titleId),
        onToggle = { toggle(titleId) },
        onStartSelecting = { startWith(titleId) },
        onCtrlClick = { ctrlClick(titleId) },
        onShiftClick = { shiftClick(titleId, titleIdsInShownOrder) }
    )

internal fun Modifier.titleSelectionMouseClicks(selection: TitleCardSelection?): Modifier {
    if (!isDesktopPlatform || selection == null) return this
    return composed {
        val latestSelection by rememberUpdatedState(selection)
        DisposableEffect(Unit) {
            onDispose { GlobalEditorState.isPointerOverWatchlistTitle = false }
        }
        pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Enter -> GlobalEditorState.isPointerOverWatchlistTitle = true
                        PointerEventType.Exit -> GlobalEditorState.isPointerOverWatchlistTitle = false
                        PointerEventType.Press -> {
                            if (!event.buttons.isPrimaryPressed) continue
                            val press = event.changes.firstOrNull() ?: continue
                            val pressedKeys = event.keyboardModifiers
                            when {
                                pressedKeys.isShiftPressed -> {
                                    press.consume()
                                    latestSelection.onShiftClick()
                                }
                                pressedKeys.isCtrlPressed || pressedKeys.isMetaPressed -> {
                                    press.consume()
                                    latestSelection.onCtrlClick()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TitleSelectionCheckbox(isSelected: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(4.dp)) {
        EmberrCheckbox(checked = isSelected, onCheckedChange = null)
    }
}

@Composable
internal fun WatchlistSelectionBar(
    selectedCount: Int,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    onClear: () -> Unit
) {
    val hasSelectedTitles = selectedCount > 0
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        SelectionBarButton(label = "Refresh", icon = Res.drawable.refresh_cw, isEnabled = hasSelectedTitles, onClick = onRefresh)
        SelectionBarButton(
            label = "Remove",
            icon = Res.drawable.trash,
            isEnabled = hasSelectedTitles,
            isDestructive = true,
            onClick = onRemove
        )
        IconTooltip(text = "Clear selection") {
            Icon(
                painter = painterResource(Res.drawable.x),
                contentDescription = "Clear selection",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
private fun SelectionBarButton(
    label: String,
    icon: DrawableResource,
    isEnabled: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val fullColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val color = if (isEnabled) fullColor else fullColor.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = if (isDesktopPlatform) null else label, tint = color, modifier = Modifier.size(16.dp))
        if (isDesktopPlatform) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1)
        }
    }
}
