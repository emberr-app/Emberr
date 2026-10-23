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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val UpdatePeekGhostSize = 56.dp
private val UpdatePeekVisibleHeight = 32.dp
private val UpdatePeekDelayAfterLaunch = 800.milliseconds
private val UpdatePeekGoodbyeDuration = 1500.milliseconds
private val UpdatePillGrowsFromBottom = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
private val UpdateDialogShape = RoundedCornerShape(22.dp)
private val UpdateDialogMaxHeight = 620.dp
private val UpdateDialogPadding = 24.dp
private val UpdateDialogHeaderBlurHeight = 110.dp
private val UpdateDialogHeaderGradientHeight = 130.dp

private class UpdateNotesSection(val heading: String, val items: List<String>)

private val CurrentUpdateNotes = listOf(
    UpdateNotesSection(
        heading = "Calendar Enhancements",
        items = listOf(
            "Drag events to a new time or day",
            "Resize events by dragging their edges",
            "Move repeating events, one or all",
            "All-day and multi-day events",
            "Redesigned event sheets",
            "Start and end times on events",
            "Checked events greyed out",
            "Show or hide categories"
        ) + if (isDesktopPlatform) {
            listOf(
                "Side panel with a mini month",
                "Two-finger date swipe"
            )
        } else {
            emptyList()
        }
    ),
    UpdateNotesSection(
        heading = "Shared Daily Notes and Calendar",
        items = listOf(
            "One daily note and calendar across all spaces",
            "Per-space calendar toggles",
            "Account-wide tasks Inbox",
            "Daily notes in search, media and trash",
            "Daily notes sync across devices",
            "Daily notes and calendar in backups"
        )
    ),
    UpdateNotesSection(
        heading = "Cover Images and GIFs",
        items = listOf(
            "Animated GIFs",
            "Add note covers using web links",
            "Reposition covers"
        )
    ),
    UpdateNotesSection(
        heading = "Editor",
        items = listOf(
            "Smoother typing and moving between blocks with Backspace, Enter and arrow keys"
        ) + if (isDesktopPlatform) {
            listOf("Drag a box to select blocks")
        } else {
            emptyList()
        }
    ),
    UpdateNotesSection(
        heading = "Others",
        items = listOf(
            "New Blur and Color top bar styles"
        ) + (if (isDesktopPlatform) {
            listOf(
                "Top bar fade",
                "Sidebar button in headers",
                "Drag and drop files to import",
                "Right-click title bar for window menu"
            )
        } else {
            emptyList()
        }) + listOf(
            "Joplin location fields skipped on import",
            "Sub-notes stay in their parent's space",
            "Duplicate category names are blocked"
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
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = UpdateDialogMaxHeight)
                .clip(UpdateDialogShape)
                .background(dialogColor)
        ) {
            Box(modifier = Modifier.weight(1f, fill = false)) {
                val dialogHazeState = remember { HazeState() }
                var headerHeightPx by remember { mutableFloatStateOf(0f) }
                var buttonAreaHeightPx by remember { mutableFloatStateOf(0f) }
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
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = UpdateDialogPadding)
                        .padding(top = headerHeight + 16.dp, bottom = buttonAreaHeight),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
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
