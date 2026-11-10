package com.emberr.presentation.whatsnew

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrFrontGhost
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.LocalDialogWindowTitleBar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.TopHeaderTitlePlacement
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.desktopDialogSize
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.fullScreenDialogProperties
import com.emberr.presentation.shared.components.mobileDialogSize
import com.emberr.presentation.shared.components.orHighContrastLine
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.shared.editor.components.FullScreenImageScreen
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.PaperTextureBox
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.x
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.whatsnew_new
import emberr.shared.generated.resources.whatsnew_see_you_later
import emberr.shared.generated.resources.whatsnew_title
import emberr.shared.generated.resources.whatsnew_version
import emberr.shared.generated.resources.whatsnew_whats_new_question
import org.jetbrains.compose.resources.stringResource

private val UpdatePeekGhostSize = 56.dp
private val UpdatePeekVisibleHeight = 32.dp
private val UpdatePeekDelayAfterLaunch = 800.milliseconds
private val UpdatePeekGoodbyeDuration = 1500.milliseconds
private val UpdatePillGrowsFromBottom = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
private val UpdateDialogShape = RoundedCornerShape(22.dp)
private val UpdateDialogPadding = 24.dp
private val UpdateDialogHeaderBlurHeight = 110.dp
private val UpdateDialogHeaderGradientHeight = 130.dp
private val UpdateScreenshotShape = RoundedCornerShape(12.dp)
private val UpdateSectionGap = 28.dp
private val UpdateScreenshotWidthToHeightRatio = if (isDesktopPlatform) 1834f / 1019f else 407f / 858f
private val UpdateScreenshotMaxHeight = if (isDesktopPlatform) 400.dp else 360.dp

@Composable
fun EmberrUpdatePeek(
    isDue: Boolean,
    canShow: Boolean,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    onPeekingChange: (Boolean) -> Unit = {}
) {
    var hasWaitedAfterLaunch by remember { mutableStateOf(false) }
    var isDialogOpen by remember { mutableStateOf(false) }
    var isSayingGoodbye by remember { mutableStateOf(false) }
    var isGone by remember { mutableStateOf(false) }

    LaunchedEffect(isDue) {
        if (isDue) {
            delay(UpdatePeekDelayAfterLaunch)
            hasWaitedAfterLaunch = true
        }
    }

    LaunchedEffect(isSayingGoodbye) {
        if (isSayingGoodbye) {
            delay(UpdatePeekGoodbyeDuration)
            isGone = true
        }
    }

    val isPeeking = isDue && hasWaitedAfterLaunch && canShow && !isGone

    LaunchedEffect(isPeeking) {
        onPeekingChange(isPeeking)
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AnimatedVisibility(
            visible = isPeeking,
            enter = fadeIn(tween(250, delayMillis = 300)) + scaleIn(
                animationSpec = tween(250, delayMillis = 300, easing = FastOutSlowInEasing),
                initialScale = 0.8f,
                transformOrigin = UpdatePillGrowsFromBottom
            ),
            exit = fadeOut(tween(300)) + scaleOut(
                animationSpec = tween(300),
                targetScale = 0.8f,
                transformOrigin = UpdatePillGrowsFromBottom
            )
        ) {
            UpdatePeekPill(
                label = stringResource(if (isSayingGoodbye) Res.string.whatsnew_see_you_later else Res.string.whatsnew_whats_new_question),
                hazeState = hazeState,
                isClickable = !isSayingGoodbye,
                onClick = { isDialogOpen = true }
            )
        }

        Spacer(Modifier.height(6.dp))

        PeekingGhost(isPeeking = isPeeking)
    }

    if (isDialogOpen) {
        EmberrUpdateDialog(
            onDismissRequest = {
                isDialogOpen = false
                isSayingGoodbye = true
            }
        )
    }
}

@Composable
private fun PeekingGhost(isPeeking: Boolean) {
    Box(
        modifier = Modifier
            .size(width = UpdatePeekGhostSize, height = UpdatePeekVisibleHeight)
            .clipToBounds()
    ) {
        AnimatedVisibility(
            visible = isPeeking,
            modifier = Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true),
            enter = slideInVertically(tween(450, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight },
            exit = slideOutVertically(tween(800, easing = FastOutSlowInEasing)) { fullHeight -> fullHeight }
        ) {
            EmberrFrontGhost(modifier = Modifier.size(UpdatePeekGhostSize))
        }
    }
}

@Composable
private fun UpdatePeekPill(
    label: String,
    hazeState: HazeState?,
    isClickable: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .emberrBorder(CircleShape)
            .customEmberrShadow(CircleShape)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
            .clickable(enabled = isClickable, onClick = onClick)
    ) {
        AnimatedContent(
            targetState = label,
            transitionSpec = { fadeIn(tween(220, delayMillis = 90)) togetherWith fadeOut(tween(90)) },
            label = "update-peek-pill-label"
        ) { visibleLabel ->
            Text(
                text = visibleLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun EmberrUpdateDialog(onDismissRequest: () -> Unit) {
    val dialogColor = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background
    val dialogWindowTitleBar = LocalDialogWindowTitleBar.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = fullScreenDialogProperties()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            dialogWindowTitleBar?.invoke()
            PaperTextureBox(
                modifier = Modifier
                    .then(
                        if (isDesktopPlatform) {
                            Modifier.desktopDialogSize()
                        } else {
                            Modifier
                                .mobileDialogSize()
                                .safeDrawingPadding()
                        }
                    )
                    .clip(UpdateDialogShape)
                    .background(dialogColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        val dialogHazeState = remember { HazeState() }
                        var headerHeightPx by remember { mutableFloatStateOf(0f) }
                        val notesScrollState = rememberScrollState()
                        val headerFadeAlpha by remember {
                            derivedStateOf { if (notesScrollState.value > 0) 1f else 0f }
                        }
                        val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
                        val headerBlurStyle = EmberrBlur.Regular.copy(
                            backgroundColor = dialogColor,
                            tints = listOf(HazeTint(dialogColor.copy(alpha = 0.45f))),
                            fallbackTint = HazeTint(dialogColor)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .hazeSource(state = dialogHazeState)
                                .verticalScroll(notesScrollState)
                                .padding(horizontal = UpdateDialogPadding)
                                .padding(top = headerHeight + 16.dp, bottom = UpdateDialogPadding),
                            verticalArrangement = Arrangement.spacedBy(UpdateSectionGap)
                        ) {
                            CurrentUpdateNotes.forEachIndexed { index, section ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
                                    )
                                }
                                UpdateNotesSectionView(section)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .onGloballyPositioned { coordinates -> headerHeightPx = coordinates.size.height.toFloat() }
                        ) {
                            EmberrTopHeaderBar(
                                titlePlacement = TopHeaderTitlePlacement.Start,
                                showBackButton = false,
                                reserveBackButtonSpace = false,
                                showsSidebarToggle = false,
                                topEdgeFadeAlpha = headerFadeAlpha,
                                topEdgeColor = dialogColor,
                                topEdgeBlurHeight = UpdateDialogHeaderBlurHeight,
                                topEdgeGradientHeight = UpdateDialogHeaderGradientHeight,
                                flatFadeHeight = headerHeight + 4.dp,
                                hazeState = dialogHazeState,
                                hazeStyle = headerBlurStyle,
                                applyStatusBarPadding = false,
                                contentPadding = topHeaderBarPadding(
                                    top = UpdateDialogPadding,
                                    horizontal = UpdateDialogPadding,
                                    bottom = 8.dp
                                ),
                                leadingContent = {
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.whatsnew_title),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        appVersionName?.let { version ->
                                            Text(
                                                text = stringResource(Res.string.whatsnew_version, version),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    TopBarIconButton(
                                        icon = painterResource(Res.drawable.x),
                                        contentDescription = stringResource(Res.string.common_close),
                                        bgColor = Color.Transparent,
                                        tint = MaterialTheme.colorScheme.primary,
                                        hazeState = dialogHazeState,
                                        hazeStyle = headerBlurStyle,
                                        tooltip = stringResource(Res.string.common_close),
                                        onClick = onDismissRequest
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateNotesSectionView(section: UpdateNotesSection) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        UpdateNotesSectionHeading(section)

        if (section.screenshotPaths.isNotEmpty()) {
            UpdateScreenshotCarousel(section.screenshotPaths)
        }

        section.preview?.let { preview ->
            Box(modifier = Modifier.padding(bottom = 6.dp)) { preview() }
        }

        section.description?.let { description ->
            Text(
                text = textWithBoldMarkedParts(stringResource(description)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        section.items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = textWithBoldMarkedParts(stringResource(item)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun UpdateNotesSectionHeading(section: UpdateNotesSection) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(section.heading),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (section.isNew) {
                UpdateNewPill()
            }
        }
        section.whereToFind?.let { whereToFind ->
            Text(
                text = stringResource(whereToFind),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun newPillColor(): Color =
    if (LocalAppIsDark.current) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface

@Composable
private fun UpdateNewPill() {
    Text(
        text = stringResource(Res.string.whatsnew_new),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(CircleShape)
            .background(newPillColor())
            .emberrBorder(CircleShape)
            .padding(horizontal = 9.dp, vertical = 2.dp)
    )
}

private fun textWithBoldMarkedParts(text: String): AnnotatedString = buildAnnotatedString {
    text.split("**").forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(part) }
        } else {
            append(part)
        }
    }
}

@Composable
private fun UpdateScreenshotCarousel(screenshotPaths: List<String>) {
    val pagerState = rememberPagerState { screenshotPaths.size }
    val coroutineScope = rememberCoroutineScope()
    var fullScreenScreenshotPath by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.padding(bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val pageWidth = minOf(maxWidth, UpdateScreenshotMaxHeight * UpdateScreenshotWidthToHeightRatio)
            val sidePadding = (maxWidth - pageWidth) / 2

            HorizontalPager(
                state = pagerState,
                pageSize = PageSize.Fixed(pageWidth),
                pageSpacing = 12.dp,
                contentPadding = PaddingValues(horizontal = sidePadding),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(pageWidth / UpdateScreenshotWidthToHeightRatio)
            ) { page ->
                UpdateScreenshot(
                    path = screenshotPaths[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(UpdateScreenshotShape)
                        .clickable { fullScreenScreenshotPath = screenshotPaths[page] }
                )
            }
        }

        Row {
            screenshotPaths.indices.forEach { page ->
                val dotColor = if (pagerState.currentPage == page) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .clickable { coroutineScope.launch { pagerState.animateScrollToPage(page) } }
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }

    fullScreenScreenshotPath?.let { path ->
        FullScreenUpdateScreenshot(path = path, onBack = { fullScreenScreenshotPath = null })
    }
}

@Composable
private fun UpdateScreenshot(path: String, modifier: Modifier) {
    val request = rememberUpdateScreenshotRequest(path) ?: return

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun FullScreenUpdateScreenshot(path: String, onBack: () -> Unit) {
    Dialog(onDismissRequest = onBack, properties = fullScreenDialogProperties()) {
        FullScreenImageScreen(
            request = rememberUpdateScreenshotRequest(path),
            localImagePath = null,
            hasLocalFile = false,
            thumbnailBoundsInRoot = null,
            imageWidthToHeightRatio = UpdateScreenshotWidthToHeightRatio,
            onBack = onBack,
            onDownload = {},
            onCopy = {},
            onDelete = {},
            showsActionBar = false
        )
    }
}

@Composable
private fun rememberUpdateScreenshotRequest(path: String): ImageRequest? {
    val screenshotBytes by produceState<ByteArray?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) { runCatching { Res.readBytes(path) }.getOrNull() }
    }
    val context = LocalPlatformContext.current
    return remember(screenshotBytes, context) {
        screenshotBytes?.let { bytes ->
            ImageRequest.Builder(context)
                .data(bytes)
                .memoryCacheKey(path)
                .build()
        }
    }
}
