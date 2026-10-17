package com.emberr.presentation.shared.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.emberr.domain.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private val SubmenuGap = 2.dp
private val SubmenuWindowVerticalMargin = 48.dp
private val SubmenuBuiltInVerticalPadding = 8.dp
private val DefaultSubmenuWidthEstimate = 280.dp
private const val SubmenuSwitchDelayMillis = 100L
private const val SubmenuCloseDelayMillis = 250L

internal class DesktopSubmenuCoordinator {
    private val rowBoundsInWindow = mutableMapOf<Any, Rect>()
    var menuCoordinates: LayoutCoordinates? = null
    var hoveredRowId by mutableStateOf<Any?>(null)
    var openRowId by mutableStateOf<Any?>(null)

    fun setRowBounds(rowId: Any, bounds: Rect) {
        rowBoundsInWindow[rowId] = bounds
    }

    fun removeRow(rowId: Any) {
        rowBoundsInWindow.remove(rowId)
        if (openRowId == rowId) openRowId = null
    }

    fun close(rowId: Any) {
        if (openRowId == rowId) openRowId = null
    }

    fun reset() {
        hoveredRowId = null
        openRowId = null
        rowBoundsInWindow.clear()
    }

    fun pointerMovedTo(positionInMenu: Offset) {
        val coordinates = menuCoordinates?.takeIf { it.isAttached } ?: return
        val positionInWindow = coordinates.localToWindow(positionInMenu)
        hoveredRowId = rowBoundsInWindow.entries.firstOrNull {
            positionInWindow.y >= it.value.top && positionInWindow.y < it.value.bottom
        }?.key
    }

    fun pointerEnteredSubmenu(rowId: Any) {
        hoveredRowId = rowId
    }

    suspend fun keepOpenRowFollowingHover() {
        snapshotFlow { hoveredRowId }.collectLatest { hoveredId ->
            val rowOpenBeforeDelay = openRowId
            when {
                hoveredId == rowOpenBeforeDelay -> Unit
                hoveredId == null -> {
                    delay(SubmenuCloseDelayMillis)
                    if (openRowId == rowOpenBeforeDelay) openRowId = null
                }
                rowOpenBeforeDelay == null -> {
                    openRowId = hoveredId
                }
                else -> {
                    delay(SubmenuSwitchDelayMillis)
                    openRowId = hoveredId
                }
            }
        }
    }
}

internal class DesktopSubmenuPlacement(
    val offset: DpOffset,
    val direction: MenuCascadeDirection,
    val properties: PopupProperties,
    val onMenuSizeChanged: (IntSize) -> Unit,
    val onPressedInside: () -> Unit,
    val onPointerInside: () -> Unit
)

internal val LocalDesktopSubmenuCoordinator = compositionLocalOf<DesktopSubmenuCoordinator?> { null }

internal val LocalDesktopSubmenuPlacement = compositionLocalOf<DesktopSubmenuPlacement?> { null }

internal val LocalShowsSubmenuChevron = compositionLocalOf { false }

private class AnchorCoordinatesHolder {
    var coordinates: LayoutCoordinates? = null
}

@Composable
fun EmberrDesktopSubmenu(
    modifier: Modifier = Modifier,
    opensOnHover: Boolean = true,
    menuWidthEstimate: Dp = DefaultSubmenuWidthEstimate,
    anchor: @Composable (openSubmenu: () -> Unit, isOpen: Boolean) -> Unit,
    menu: @Composable (isOpen: Boolean, onDismissRequest: () -> Unit) -> Unit
) {
    val coordinator = LocalDesktopSubmenuCoordinator.current ?: remember { DesktopSubmenuCoordinator() }
    val rowId = remember { Any() }
    var isEngaged by remember { mutableStateOf(false) }
    val isOpen = coordinator.openRowId == rowId
    val showsChevron = opensOnHover && isDesktopPlatform

    DisposableEffect(coordinator, rowId) {
        onDispose { coordinator.removeRow(rowId) }
    }
    LaunchedEffect(isOpen) {
        if (!isOpen) isEngaged = false
    }

    EmberrDesktopSubmenuPlacement(
        isFocusable = isEngaged,
        onPressedInside = { isEngaged = true },
        onPointerInside = { coordinator.pointerEnteredSubmenu(rowId) },
        onAnchorPositioned = { anchorCoordinates ->
            if (opensOnHover && anchorCoordinates.isAttached) {
                coordinator.setRowBounds(rowId, anchorCoordinates.boundsInWindow())
            }
        },
        menuWidthEstimate = menuWidthEstimate,
        modifier = modifier,
        anchor = {
            CompositionLocalProvider(LocalShowsSubmenuChevron provides showsChevron) {
                anchor(
                    {
                        isEngaged = true
                        coordinator.openRowId = rowId
                    },
                    isOpen
                )
            }
        },
        menu = { menu(isOpen) { coordinator.close(rowId) } }
    )
}

@Composable
internal fun EmberrDesktopSubmenuPlacement(
    isFocusable: Boolean,
    onPressedInside: () -> Unit,
    onPointerInside: () -> Unit,
    menuWidthEstimate: Dp,
    modifier: Modifier = Modifier,
    onAnchorPositioned: (LayoutCoordinates) -> Unit = {},
    anchor: @Composable () -> Unit,
    menu: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val parentCoordinator = LocalDesktopSubmenuCoordinator.current
    val preferredDirection = LocalMenuCascadeDirection.current
    val anchorHolder = remember { AnchorCoordinatesHolder() }
    var menuSize by remember { mutableStateOf(IntSize.Zero) }

    val gapPx = with(density) { SubmenuGap.roundToPx() }
    val windowMarginPx = with(density) { SubmenuWindowVerticalMargin.roundToPx() }
    val builtInPaddingPx = with(density) { SubmenuBuiltInVerticalPadding.roundToPx() }
    val anchorBounds = anchorHolder.coordinates?.takeIf { it.isAttached }?.boundsInWindow() ?: Rect.Zero
    val parentMenuBounds = parentCoordinator?.menuCoordinates?.takeIf { it.isAttached }?.boundsInWindow() ?: anchorBounds
    val menuWidthPx = if (menuSize.width > 0) menuSize.width else with(density) { menuWidthEstimate.roundToPx() }
    val menuHeightPx = if (menuSize.height > 0) menuSize.height + builtInPaddingPx * 2 else 0

    val fitsOnRight = parentMenuBounds.right + gapPx + menuWidthPx <= windowSize.width
    val fitsOnLeft = parentMenuBounds.left - gapPx - menuWidthPx >= 0f
    val opensLeftward = when {
        preferredDirection == MenuCascadeDirection.LEFT && fitsOnLeft -> true
        fitsOnRight -> false
        else -> fitsOnLeft
    }
    val menuLeft = if (opensLeftward) parentMenuBounds.left - gapPx - menuWidthPx else parentMenuBounds.right + gapPx
    val highestTop = windowMarginPx.toFloat()
    val lowestTop = (windowSize.height - windowMarginPx - menuHeightPx).toFloat()
    val menuTop = anchorBounds.top.coerceAtMost(lowestTop).coerceAtLeast(highestTop)

    val placement = DesktopSubmenuPlacement(
        offset = with(density) {
            DpOffset(
                x = (menuLeft - anchorBounds.left).toDp(),
                y = (menuTop - anchorBounds.bottom).toDp()
            )
        },
        direction = if (opensLeftward) MenuCascadeDirection.LEFT else MenuCascadeDirection.RIGHT,
        properties = PopupProperties(focusable = isFocusable, dismissOnClickOutside = isFocusable),
        onMenuSizeChanged = { menuSize = it },
        onPressedInside = onPressedInside,
        onPointerInside = onPointerInside
    )

    Box(
        modifier = modifier.onGloballyPositioned {
            anchorHolder.coordinates = it
            onAnchorPositioned(it)
        }
    ) {
        anchor()
        CompositionLocalProvider(LocalDesktopSubmenuPlacement provides placement) {
            menu()
        }
    }
}
