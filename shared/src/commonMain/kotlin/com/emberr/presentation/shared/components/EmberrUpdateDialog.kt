package com.emberr.presentation.shared.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.components.FullScreenImageScreen
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.maximize_2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration.Companion.milliseconds

private val UpdatePeekGhostSize = 56.dp
private val UpdatePeekVisibleHeight = 32.dp
private val UpdatePeekDelayAfterLaunch = 800.milliseconds
private val UpdatePeekGoodbyeDuration = 1500.milliseconds
private val UpdatePillGrowsFromBottom = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
private val UpdateDialogShape = RoundedCornerShape(22.dp)
private val UpdateDialogMaxHeight = 620.dp
private val UpdateDialogDesktopMaxWidth = 760.dp
private val UpdateDialogDesktopMaxHeight = 720.dp
private const val UpdateDialogDesktopWidthFraction = 0.6f
private const val UpdateDialogDesktopHeightFraction = 0.8f
private val UpdateDialogPadding = 24.dp
private val UpdateDialogHeaderBlurHeight = 110.dp
private val UpdateDialogHeaderGradientHeight = 130.dp
private val UpdateScreenshotShape = RoundedCornerShape(12.dp)
private const val UpdateScreenshotWidthToHeightRatio = 1834f / 1019f

private class UpdateNotesSection(
    val heading: String,
    val items: List<String>,
    val screenshotPaths: List<String> = emptyList()
)

private val CurrentUpdateNotes = listOf(
    UpdateNotesSection(
        heading = "Movies and Shows",
        items = listOf(
            "Movie and Watchlist blocks",
            "Search movies and shows from TMDB",
            "Details with trailer, cast, seasons, reviews and where to watch",
            "Actor pages with their best-known roles",
            "List, board and gallery views",
            "Watchlist views, each with its own sort and filters",
            "Watch status and rating shared across notes in a space",
            "Ticket-style cards tinted by the poster"
        ),
        screenshotPaths = listOf(
            "files/whats_new/movies0.png",
            "files/whats_new/movies1.png",
            "files/whats_new/movies2.png",
            "files/whats_new/movies3.png"
        )
    ),
    UpdateNotesSection(
        heading = "Editor",
        items = listOf(
            "Insert the current time or date from the slash menu"
        ) + if (isDesktopPlatform) {
            emptyList()
        } else {
            listOf("Time and date buttons on the keyboard toolbar")
        },
        screenshotPaths = listOf(
            "files/whats_new/editor1.png",
            "files/whats_new/editor2.png"
        )
    ),
    UpdateNotesSection(
        heading = "Self-Host Sync",
        items = listOf(
            "Faster setup with live progress",
            "See the recovery passphrase again on connected devices",
            "Server address with a copy button",
            "Faster syncs that skip unchanged files",
            "Count of notes waiting to sync"
        ) + if (isDesktopPlatform) {
            emptyList()
        } else {
            listOf("Background sync every 30 minutes when something changed")
        }
    ),
    UpdateNotesSection(
        heading = "Export",
        items = listOf(
            "Notes with attachments export as a zip with an attachments folder"
        )
    ),
    UpdateNotesSection(
        heading = "Others",
        items = (if (isDesktopPlatform) {
            listOf(
                "Smoother, sharper small images",
                "No more syncing every time a screen opens"
            )
        } else {
            listOf("Back button clears selected database rows first")
        }) + listOf(
            "Light mode fixes in the self-host dialogs"
        )
    )
)

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
                label = if (isSayingGoodbye) "See you later!" else "What's new?",
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

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (isDesktopPlatform) {
                        Modifier
                            .fillMaxWidth(UpdateDialogDesktopWidthFraction)
                            .widthIn(max = UpdateDialogDesktopMaxWidth)
                            .fillMaxHeight(UpdateDialogDesktopHeightFraction)
                            .heightIn(max = UpdateDialogDesktopMaxHeight)
                    } else {
                        Modifier
                            .widthIn(max = 560.dp)
                            .fillMaxWidth(0.92f)
                            .heightIn(max = UpdateDialogMaxHeight)
                    }
                )
                .clip(UpdateDialogShape)
                .background(dialogColor)
        ) {
            Box(modifier = Modifier.weight(1f, fill = false)) {
                val dialogHazeState = remember { HazeState() }
                var headerHeightPx by remember { mutableFloatStateOf(0f) }
                var buttonAreaHeightPx by remember { mutableFloatStateOf(0f) }
                val notesScrollState = rememberScrollState()
                val headerFadeAlpha by remember {
                    derivedStateOf { if (notesScrollState.value > 0) 1f else 0f }
                }
                val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
                val buttonAreaHeight = with(LocalDensity.current) { buttonAreaHeightPx.toDp() }
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
                        .padding(top = headerHeight + 16.dp, bottom = buttonAreaHeight),
                    verticalArrangement = Arrangement.spacedBy(40.dp)
                ) {
                    CurrentUpdateNotes.forEach { section ->
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
                                    text = "What's new",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                appVersionName?.let { version ->
                                    Text(
                                        text = "Version $version",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    )
                }

                EmberrButtonPrimary(
                    text = "Got it",
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates -> buttonAreaHeightPx = coordinates.size.height.toFloat() }
                        .fillMaxWidth()
                        .padding(UpdateDialogPadding)
                )
            }
        }
    }
}

@Composable
private fun UpdateNotesSectionView(section: UpdateNotesSection) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = section.heading,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (section.screenshotPaths.isNotEmpty()) {
            UpdateScreenshotCarousel(section.screenshotPaths)
        }

        section.items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(UpdateScreenshotWidthToHeightRatio)
                .clip(UpdateScreenshotShape)
                .emberrBorder(UpdateScreenshotShape)
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                UpdateScreenshot(
                    path = screenshotPaths[page],
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage((page + 1) % screenshotPaths.size) }
                        }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                TopBarIconButton(
                    icon = painterResource(Res.drawable.maximize_2),
                    contentDescription = "View full screen",
                    bgColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tint = MaterialTheme.colorScheme.primary,
                    size = 32.dp,
                    iconSize = 16.dp,
                    tooltip = "View full screen",
                    tooltipPosition = IconTooltipPosition.Start,
                    onClick = { fullScreenScreenshotPath = screenshotPaths[pagerState.currentPage] }
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
