package com.emberr.presentation.shared.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

private val MobileMenuWidth = 220.dp
private val MobileMenuOptionMinHeight = 48.dp
private val MobileMenuGapFromTrigger = 6.dp
private val MobileMenuShadowPadding = 24.dp
private val MobileMenuWindowMargin = 12.dp
private val MobileMenuHorizontalPadding = 6.dp
private val MobileMenuShape = RoundedCornerShape(18.dp)
private const val MobileMenuAppearMillis = 120

@Stable
class MobileMenuAnchor {
    var triggerBounds by mutableStateOf(Rect.Zero)
    internal var areaCoordinates: LayoutCoordinates? = null

    internal fun recordTrigger(triggerCoordinates: LayoutCoordinates?) {
        val area = areaCoordinates?.takeIf { it.isAttached } ?: return
        val trigger = triggerCoordinates?.takeIf { it.isAttached } ?: return
        triggerBounds = area.localBoundingBoxOf(trigger, clipBounds = false)
    }
}

@Composable
fun rememberMobileMenuAnchor(): MobileMenuAnchor = remember { MobileMenuAnchor() }

fun Modifier.mobileMenuArea(anchor: MobileMenuAnchor): Modifier =
    onGloballyPositioned { anchor.areaCoordinates = it }

fun Modifier.mobileMenuTrigger(anchor: MobileMenuAnchor): Modifier = composed {
    var triggerCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    onGloballyPositioned { triggerCoordinates = it }
        .pointerInput(anchor) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                anchor.recordTrigger(triggerCoordinates)
            }
        }
}

@Composable
fun EmberrMobileMenu(
    anchor: MobileMenuAnchor,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val density = LocalDensity.current
    val bounds = anchor.triggerBounds
    val triggerSize = with(density) { DpSize(bounds.width.toDp(), bounds.height.toDp()) }
    val positionProvider = remember(density) {
        with(density) {
            MobileMenuPositionProvider(
                gapPx = MobileMenuGapFromTrigger.roundToPx(),
                shadowPaddingPx = MobileMenuShadowPadding.roundToPx(),
                windowMarginPx = MobileMenuWindowMargin.roundToPx()
            )
        }
    }

    Box(
        modifier = Modifier
            .offset { bounds.topLeft.round() }
            .size(triggerSize)
    ) {
        if (expanded) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = onDismissRequest,
                properties = PopupProperties(focusable = true)
            ) {
                MobileMenuSurface(modifier = modifier, content = content)
            }
        }
    }
}

@Composable
private fun MobileMenuSurface(
    modifier: Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val blurSource = LocalEmberrBlurSource.current
    val appearAnimation = remember { MutableTransitionState(false).apply { targetState = true } }

    AnimatedVisibility(
        visibleState = appearAnimation,
        enter = fadeIn(tween(MobileMenuAppearMillis)) + scaleIn(tween(MobileMenuAppearMillis), initialScale = 0.92f),
        exit = ExitTransition.None
    ) {
        Column(
            modifier = modifier
                .padding(MobileMenuShadowPadding)
                .width(MobileMenuWidth)
                .emberrBorder(MobileMenuShape)
                .customEmberrShadow(
                    shape = MobileMenuShape,
                    spotColor = EmberrPillShadowSpotColor,
                    ambientColor = EmberrPillShadowAmbientColor
                )
                .clip(MobileMenuShape)
                .then(
                    if (blurSource != null) Modifier.emberrBlur(blurSource, EmberrBlur.Regular)
                    else Modifier.background(MaterialTheme.colorScheme.background)
                )
                .padding(horizontal = MobileMenuHorizontalPadding, vertical = 4.dp),
            content = content
        )
    }
}

private class MobileMenuPositionProvider(
    private val gapPx: Int,
    private val shadowPaddingPx: Int,
    private val windowMarginPx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val menuWidth = popupContentSize.width - shadowPaddingPx * 2
        val menuHeight = popupContentSize.height - shadowPaddingPx * 2

        val menuLeft = if (anchorBounds.left + menuWidth <= windowSize.width - windowMarginPx) {
            anchorBounds.left
        } else {
            (anchorBounds.right - menuWidth).coerceAtLeast(windowMarginPx)
        }

        val belowTop = anchorBounds.bottom + gapPx
        val aboveTop = anchorBounds.top - gapPx - menuHeight
        val menuTop = when {
            belowTop + menuHeight <= windowSize.height - windowMarginPx -> belowTop
            aboveTop >= windowMarginPx -> aboveTop
            else -> belowTop.coerceAtMost(windowSize.height - windowMarginPx - menuHeight).coerceAtLeast(windowMarginPx)
        }

        return IntOffset(menuLeft - shadowPaddingPx, menuTop - shadowPaddingPx)
    }
}

@Composable
fun EmberrMobileMenuOption(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null
) {
    EmberrDesktopMenuOption(
        label = label,
        onClick = onClick,
        modifier = modifier.heightIn(min = MobileMenuOptionMinHeight),
        icon = icon
    )
}
