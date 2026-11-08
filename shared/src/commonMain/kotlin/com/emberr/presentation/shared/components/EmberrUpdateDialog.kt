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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.components.FullScreenImageScreen
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.PaperTextureBox
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

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
private val UpdateScreenshotWidthToHeightRatio = if (isDesktopPlatform) 1834f / 1019f else 407f / 858f
private val UpdateScreenshotMaxHeight = if (isDesktopPlatform) 400.dp else 360.dp

private class UpdateNotesSection(
    val heading: String,
    val items: List<String>,
    val isNew: Boolean = false,
    val screenshotPaths: List<String> = emptyList()
)

private val CurrentUpdateNotes = listOfNotNull(
    UpdateNotesSection(
        heading = "Credential Block",
        isNew = true,
        items = listOf(
            "Save logins right in your notes. Type **/** and pick **Advanced Blocks → Credentials**.",
            "Comes with a password generator, and you can copy fields with a tap.",
            "Passwords and notes stay encrypted and never show up in search, AI chat or widgets."
        )
    ),
    if (isDesktopPlatform) {
        null
    } else {
        UpdateNotesSection(
            heading = "App Lock",
            isNew = true,
            items = listOf(
                "Lock Emberr with your fingerprint, face or screen lock. Turn it on in **Settings → Security**."
            )
        )
    },
    UpdateNotesSection(
        heading = "People",
        isNew = true,
        items = listOf(
            "Add someone once and use them everywhere. Assign them in databases, add them to meetings, and mention them in notes with **@**.",
            "Each person has their own page with their details and events. Find it under **⋯ → People** on Home.",
            "Import your phone contacts or a .vcf file with the **+** button."
        )
    ),
    UpdateNotesSection(
        heading = "Calendar",
        items = listOf(
            "Events that go past midnight now show on both days.",
            "Repeating events and tasks now show up in widgets.",
            "Unfinished daily items stay on their day and don't move to the next one."
        )
    ),
    UpdateNotesSection(
        heading = "Editor",
        items = (if (isDesktopPlatform) {
            listOf("Change how wide notes are from a note's **⋯ → Note Width**.")
        } else {
            emptyList()
        }) + listOf(
            "Tap anywhere on an empty page to start typing.",
            "Don't want the daily quote? Turn it off in **Settings → Appearance**."
        )
    ),
    UpdateNotesSection(
        heading = "Others",
        items = listOf(
            "UI and UX improvements across the app.",
            "New emoji picker.",
            "Cleaner settings."
        ) + (if (isDesktopPlatform) {
            listOf("Faster startup.")
        } else {
            emptyList()
        }) + listOf(
            "Plenty of small fixes."
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
    }
}

@Composable
private fun UpdateNotesSectionView(section: UpdateNotesSection) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = buildAnnotatedString {
                append(section.heading)
                if (section.isNew) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(" (New)") }
                }
            },
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
                    text = textWithBoldMarkedParts(item),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
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
