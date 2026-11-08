package com.emberr.presentation.shared.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emberr.domain.util.system.isDesktopPlatform
import androidx.compose.ui.graphics.painter.Painter
import com.emberr.presentation.shared.stableStatusBarsPadding
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.PaperTextureBox
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.math.abs

private val BottomSheetCornerRadius = 26.dp
private val BottomSheetShape = RoundedCornerShape(topEnd = BottomSheetCornerRadius, topStart = BottomSheetCornerRadius)
private val FloatingBottomSheetShape = RoundedCornerShape(BottomSheetCornerRadius)
private val FloatingBottomSheetEdgeGap = 12.dp
private val FloatingDialogShape = RoundedCornerShape(22.dp)
private val FloatingDialogMaxWidth = 480.dp
private val SheetHorizontalPadding = 20.dp
private val SheetHeaderTopPadding = 28.dp
private val SheetHeaderBottomPadding = if (isDesktopPlatform) 16.dp else 20.dp
private val SheetDividerGap = 12.dp
private val SheetHeaderFadeOverhang = 24.dp
private val FloatingDialogContentMaxHeight = 560.dp
private val SheetIconSpacing = 8.dp

val LocalDialogWindowTitleBar = compositionLocalOf<(@Composable () -> Unit)?> { null }

class EmberrSheetHeaderSpace(val height: Dp, val onContentScrolledChange: (Boolean) -> Unit)

val LocalEmberrSheetHeaderSpace = compositionLocalOf<EmberrSheetHeaderSpace?> { null }

@Composable
fun ReportSheetContentScrolled(state: ScrollableState) {
    val headerSpace = LocalEmberrSheetHeaderSpace.current ?: return
    LaunchedEffect(state, headerSpace) {
        snapshotFlow { state.canScrollBackward }.collect { isScrolled -> headerSpace.onContentScrolledChange(isScrolled) }
    }
}

/**
 * Scrolls a focused child just far enough to be fully visible, and not at all when it already is.
 * A sheet has to provide this for itself because a host screen can install its own
 * [BringIntoViewSpec] to reserve room for floating bars; inherited into a short sheet, those
 * reservations never fit, so every keystroke asks for a scroll the sheet cannot make and the
 * leftover distance is handed to the sheet's drag anchor instead, walking it off the bottom.
 */
@OptIn(ExperimentalFoundationApi::class)
internal object SheetBringIntoViewSpec : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val leadingEdge = offset
        val trailingEdge = offset + size
        val bottomDelta = trailingEdge - containerSize
        return when {
            leadingEdge >= 0f && trailingEdge <= containerSize -> 0f
            leadingEdge < 0f && trailingEdge > containerSize -> 0f
            abs(leadingEdge) < abs(bottomDelta) -> leadingEdge
            else -> bottomDelta
        }
    }
}

private val KeepLeftoverUpwardFlingAwayFromSheet = object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0f) available else Velocity.Zero
}

private class KeepScrolledContentAwayFromSheet : NestedScrollConnection {
    private var contentScrolledDuringGesture = false

    fun startNewGesture() {
        contentScrolledDuringGesture = false
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && consumed.y != 0f) contentScrolledDuringGesture = true
        return if (contentScrolledDuringGesture) available else Offset.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        val keepsFling = contentScrolledDuringGesture
        contentScrolledDuringGesture = false
        return if (keepsFling) available else Velocity.Zero
    }
}

@Composable
private fun sheetCardColor(): Color = if (LocalAppIsDark.current) {
    MaterialTheme.colorScheme.surface
} else {
    MaterialTheme.colorScheme.background
}

@Composable
private fun Modifier.sheetCardBackground(): Modifier = this.background(sheetCardColor())

class EmberrBottomSheetAction(
    val icon: Painter,
    val contentDescription: String,
    val tint: Color? = null,
    val tooltip: String? = null,
    val closesSheet: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EmberrBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    applyNavPadding: Boolean = false,
    headerAction: EmberrBottomSheetAction? = null,
    contentHorizontalPadding: Dp = SheetHorizontalPadding,
    isDismissible: Boolean = true,
    desktopMaxWidth: Dp = FloatingDialogMaxWidth,
    scrollsContent: Boolean = true,
    floats: Boolean = false,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    val showsSheet = if (isDesktopPlatform) expanded else rememberShowAfterKeyboardCloses(isRequested = expanded)
    if (!showsSheet) return

    CompositionLocalProvider(LocalBringIntoViewSpec provides SheetBringIntoViewSpec) {
        if (isDesktopPlatform) {
            EmberrFloatingDialog(
                onDismiss = onDismiss,
                title = title,
                subtitle = subtitle,
                headerAction = headerAction,
                contentHorizontalPadding = contentHorizontalPadding,
                isDismissible = isDismissible,
                maxWidth = desktopMaxWidth,
                scrollsContent = scrollsContent,
                content = content
            )
        } else {
            EmberrModalBottomSheet(
                onDismiss = onDismiss,
                title = title,
                subtitle = subtitle,
                applyNavPadding = applyNavPadding,
                headerAction = headerAction,
                contentHorizontalPadding = contentHorizontalPadding,
                isDismissible = isDismissible,
                scrollsContent = scrollsContent,
                floats = floats,
                content = content
            )
        }
    }
}

@Composable
private fun EmberrFloatingDialog(
    onDismiss: () -> Unit,
    title: String?,
    subtitle: String?,
    headerAction: EmberrBottomSheetAction? = null,
    contentHorizontalPadding: Dp,
    isDismissible: Boolean,
    maxWidth: Dp,
    scrollsContent: Boolean,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    fun closeAnd(action: () -> Unit) {
        action()
        onDismiss()
    }

    val blurSource = LocalEmberrBlurSource.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    val dialogContentHazeState = remember { HazeState() }
    val dialogWindowTitleBar = LocalDialogWindowTitleBar.current
    val hasHeader = title != null || subtitle != null
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = if (hasHeader) with(LocalDensity.current) { headerHeightPx.toDp() } else 0.dp
    val contentScrollState = rememberScrollState()
    var isSelfScrollingContentScrolled by remember { mutableStateOf(false) }
    val headerFadeAlpha by remember(scrollsContent) {
        derivedStateOf {
            val isScrolled = if (scrollsContent) contentScrollState.value > 0 else isSelfScrollingContentScrolled
            if (isScrolled) 1f else 0f
        }
    }
    val headerSpace = remember(headerHeight) {
        EmberrSheetHeaderSpace(height = headerHeight, onContentScrolledChange = { isScrolled -> isSelfScrollingContentScrolled = isScrolled })
    }
    val contentTopPadding = if (scrollsContent) headerHeight else 0.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(0.9f)
                .emberrBorder(FloatingDialogShape, color = emberrSoftBorderColor)
                .clip(FloatingDialogShape)
                .then(
                    if (blurSource != null) {
                        Modifier.emberrBlur(
                            blurSource,
                            EmberrBlur.Thick.copy(tints = listOf(HazeTint(surfaceColor.copy(alpha = 0.92f))))
                        )
                    } else {
                        Modifier.background(surfaceColor)
                    }
                )
        ) {
            PaperTextureBox {
                dialogWindowTitleBar?.invoke()
                CompositionLocalProvider(LocalEmberrBlurSource provides dialogContentHazeState) {
                    Box(modifier = Modifier.fillMaxWidth().imePadding()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = FloatingDialogContentMaxHeight + headerHeight)
                                .hazeSource(state = dialogContentHazeState)
                                .then(if (scrollsContent) Modifier.verticalScroll(contentScrollState) else Modifier)
                                .padding(top = contentTopPadding)
                                .padding(horizontal = contentHorizontalPadding)
                        ) {
                            CompositionLocalProvider(LocalEmberrSheetHeaderSpace provides headerSpace.takeUnless { scrollsContent }) {
                                content { action -> closeAnd(action) }
                            }
                        }

                        if (hasHeader) {
                            EmberrSheetHeader(
                                title = title,
                                subtitle = subtitle,
                                headerAction = headerAction,
                                cardColor = surfaceColor,
                                hazeState = dialogContentHazeState,
                                fadeAlpha = headerFadeAlpha,
                                headerHeight = headerHeight,
                                onActionClick = { action -> if (action.closesSheet) closeAnd(action.onClick) else action.onClick() },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .onSizeChanged { size -> headerHeightPx = size.height }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmberrModalBottomSheet(
    onDismiss: () -> Unit,
    title: String?,
    subtitle: String?,
    applyNavPadding: Boolean,
    headerAction: EmberrBottomSheetAction? = null,
    contentHorizontalPadding: Dp,
    isDismissible: Boolean,
    scrollsContent: Boolean,
    floats: Boolean,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val latestIsDismissible by rememberUpdatedState(isDismissible)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue -> newValue != SheetValue.Hidden || latestIsDismissible }
    )
    val nonScrollingAreaDragRelay = rememberScrollableState { 0f }
    val keepScrolledContentAwayFromSheet = remember { KeepScrolledContentAwayFromSheet() }
    val sheetContentHazeState = remember { HazeState() }
    val hasHeader = title != null || subtitle != null
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = if (hasHeader) with(LocalDensity.current) { headerHeightPx.toDp() } else 0.dp
    val contentScrollState = rememberScrollState()
    var isSelfScrollingContentScrolled by remember { mutableStateOf(false) }
    val headerFadeAlpha by remember(scrollsContent) {
        derivedStateOf {
            val isScrolled = if (scrollsContent) contentScrollState.value > 0 else isSelfScrollingContentScrolled
            if (isScrolled) 1f else 0f
        }
    }
    val headerSpace = remember(headerHeight) {
        EmberrSheetHeaderSpace(height = headerHeight, onContentScrolledChange = { isScrolled -> isSelfScrollingContentScrolled = isScrolled })
    }
    val contentTopPadding = if (scrollsContent) headerHeight else 0.dp

    fun closeAnd(action: () -> Unit) {
        coroutineScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(250.milliseconds) {
                    sheetState.hide()
                }
            } finally {
                action()
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0) },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false
        )
    ) {
        KmpBackHandler(enabled = true) {
            if (!latestIsDismissible) return@KmpBackHandler
            coroutineScope.launch {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(250.milliseconds) {
                        sheetState.hide()
                    }
                } finally {
                    onDismiss()
                }
            }
        }

        // card
        Box(
            modifier = Modifier
                .nestedScroll(KeepLeftoverUpwardFlingAwayFromSheet)
                .nestedScroll(keepScrolledContentAwayFromSheet)
                .pointerInput(keepScrolledContentAwayFromSheet) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        keepScrolledContentAwayFromSheet.startNewGesture()
                    }
                }
                .scrollable(
                    state = nonScrollingAreaDragRelay,
                    orientation = Orientation.Vertical
                )
                .stableStatusBarsPadding()
                .fillMaxWidth()
                .then(
                    if (floats) {
                        Modifier
                            .navigationBarsPadding()
                            .padding(start = FloatingBottomSheetEdgeGap, end = FloatingBottomSheetEdgeGap, bottom = FloatingBottomSheetEdgeGap)
                    } else {
                        Modifier
                    }
                )
                .clip(if (floats) FloatingBottomSheetShape else BottomSheetShape)
                .sheetCardBackground()
        ) {
            PaperTextureBox {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .then(if (applyNavPadding) Modifier.padding(bottom = 16.dp) else Modifier)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hazeSource(state = sheetContentHazeState)
                            .then(if (scrollsContent) Modifier.verticalScroll(contentScrollState) else Modifier)
                            .padding(top = contentTopPadding)
                            .padding(horizontal = contentHorizontalPadding)
                    ) {
                        CompositionLocalProvider(
                            LocalIndication provides NoRippleIndicationNodeFactory,
                            LocalEmberrSheetHeaderSpace provides headerSpace.takeUnless { scrollsContent }
                        ) {
                            content { action -> closeAnd(action) }
                        }
                    }

                    if (hasHeader) {
                        EmberrSheetHeader(
                            title = title,
                            subtitle = subtitle,
                            headerAction = headerAction,
                            cardColor = sheetCardColor(),
                            hazeState = sheetContentHazeState,
                            fadeAlpha = headerFadeAlpha,
                            headerHeight = headerHeight,
                            onActionClick = { action -> if (action.closesSheet) closeAnd(action.onClick) else action.onClick() },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .onSizeChanged { size -> headerHeightPx = size.height }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmberrSheetHeader(
    title: String?,
    subtitle: String?,
    headerAction: EmberrBottomSheetAction?,
    cardColor: Color,
    hazeState: HazeState,
    fadeAlpha: Float,
    headerHeight: Dp,
    onActionClick: (EmberrBottomSheetAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerBlurStyle = EmberrBlur.Regular.copy(
        backgroundColor = cardColor,
        tints = listOf(HazeTint(cardColor.copy(alpha = 0.45f))),
        fallbackTint = HazeTint(cardColor)
    )
    Column(modifier = modifier.fillMaxWidth()) {
        EmberrTopHeaderBar(
            showBackButton = false,
            reserveBackButtonSpace = false,
            showsSidebarToggle = false,
            topEdgeFadeAlpha = fadeAlpha,
            topEdgeColor = cardColor,
            topEdgeBlurHeight = headerHeight + SheetHeaderFadeOverhang,
            topEdgeGradientHeight = headerHeight + SheetHeaderFadeOverhang * 2,
            flatFadeHeight = headerHeight + 4.dp,
            hazeState = hazeState,
            hazeStyle = headerBlurStyle,
            applyStatusBarPadding = false,
            contentPadding = topHeaderBarPadding(top = SheetHeaderTopPadding, horizontal = SheetHorizontalPadding, bottom = SheetHeaderBottomPadding),
            centerContent = {
                Column(modifier = Modifier.weight(1f)) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            actions = {
                if (headerAction != null) {
                    TopBarIconButton(
                        icon = headerAction.icon,
                        contentDescription = headerAction.contentDescription,
                        bgColor = sheetCardColor(),
                        tint = headerAction.tint ?: MaterialTheme.colorScheme.onSurface,
                        shadowElevation = EmberrShadowElevation.None,
                        tooltip = headerAction.tooltip,
                        onClick = { onActionClick(headerAction) }
                    )
                }
            }
        )
        if (!isDesktopPlatform) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = SheetHorizontalPadding).padding(bottom = SheetDividerGap),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
            )
        }
    }
}
