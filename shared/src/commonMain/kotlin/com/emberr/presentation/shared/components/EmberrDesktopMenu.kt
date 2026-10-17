package com.emberr.presentation.shared.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties

private val DefaultMenuShape = RoundedCornerShape(18.dp)
private val MenuHorizontalPadding = 6.dp

@Composable
fun EmberrDesktopMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
    properties: PopupProperties = PopupProperties(focusable = true),
    solidBackgroundColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurSource = LocalEmberrBlurSource.current.takeIf { solidBackgroundColor == null }
    val surfaceColor = solidBackgroundColor ?: MaterialTheme.colorScheme.surface
    val edgeColor = emberrSoftBorderColor
    val atTapPlacement = LocalMenuAtTapPlacement.current
    val submenuPlacement = LocalDesktopSubmenuPlacement.current
    val latestSubmenuPlacement by rememberUpdatedState(submenuPlacement)
    val submenuCoordinator = remember { DesktopSubmenuCoordinator() }
    val density = LocalDensity.current
    var measuredMenuWidth by remember { mutableStateOf<Dp?>(null) }
    val placementModifier = if (atTapPlacement == null) {
        Modifier
    } else {
        Modifier
            .onSizeChanged { measuredMenuWidth = with(density) { it.width.toDp() } }
            .onGloballyPositioned { atTapPlacement.onMenuPositioned(it.positionInWindow().x) }
    }
    val submenuModifier = Modifier
        .onGloballyPositioned { submenuCoordinator.menuCoordinates = it }
        .onSizeChanged { latestSubmenuPlacement?.onMenuSizeChanged?.invoke(it) }
        .pointerInput(submenuCoordinator) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Move, PointerEventType.Enter -> {
                            latestSubmenuPlacement?.onPointerInside?.invoke()
                            submenuCoordinator.pointerMovedTo(event.changes.first().position)
                        }
                        PointerEventType.Press -> latestSubmenuPlacement?.onPressedInside?.invoke()
                    }
                }
            }
        }
    val menuOffset = when {
        submenuPlacement != null -> submenuPlacement.offset
        atTapPlacement?.opensLeftward == true -> DpOffset(x = -(measuredMenuWidth ?: atTapPlacement.menuWidth), y = 0.dp)
        else -> offset
    }
    val menuContent: @Composable ColumnScope.() -> Unit = {
        DisposableEffect(submenuCoordinator) {
            onDispose { submenuCoordinator.reset() }
        }
        LaunchedEffect(submenuCoordinator) {
            submenuCoordinator.keepOpenRowFollowingHover()
        }
        CompositionLocalProvider(
            LocalMenuCascadeDirection provides (submenuPlacement?.direction ?: atTapPlacement?.direction ?: MenuCascadeDirection.RIGHT),
            LocalMenuAtTapPlacement provides null,
            LocalDesktopSubmenuPlacement provides null,
            LocalDesktopSubmenuCoordinator provides submenuCoordinator
        ) {
            content()
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = menuOffset,
        properties = submenuPlacement?.properties ?: properties,
        shape = DefaultMenuShape,
        containerColor = if (blurSource != null) Color.Transparent else surfaceColor,
        shadowElevation = if (blurSource != null) EmberrShadowElevation.None else MenuDefaults.ShadowElevation,
        border = if (blurSource != null) BorderStroke(EmberrBorderWidth, edgeColor) else null,
        modifier = if (blurSource != null) {
            modifier
                .then(submenuModifier)
                .then(placementModifier)
                .emberrBlur(blurSource, EmberrBlur.Thick)
                .padding(horizontal = MenuHorizontalPadding)
        } else {
            modifier
                .then(submenuModifier)
                .then(placementModifier)
                .background(color = surfaceColor, shape = DefaultMenuShape)
                .padding(horizontal = MenuHorizontalPadding)
        },
        content = menuContent
    )
}