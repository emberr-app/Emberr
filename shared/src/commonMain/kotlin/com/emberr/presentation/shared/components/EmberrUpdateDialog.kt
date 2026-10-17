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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emberr.domain.util.system.appVersionName
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val UpdatePeekGhostSize = 56.dp
private val UpdatePeekVisibleHeight = 32.dp
private val UpdatePeekDelayAfterLaunch = 800.milliseconds
private val UpdatePeekGoodbyeDuration = 1500.milliseconds
private val UpdatePillGrowsFromBottom = TransformOrigin(pivotFractionX = 0.5f, pivotFractionY = 1f)
private val UpdateDialogShape = RoundedCornerShape(22.dp)
private val UpdateDialogMaxHeight = 560.dp

private class UpdateNotesSection(val heading: String, val items: List<String>)

private val CurrentUpdateNotes = listOf(
    UpdateNotesSection(
        heading = "Added",
        items = listOf(
            "Select, copy and paste text across several blocks",
            "Typed and pasted Markdown turns into blocks and formatting",
            "Search your calendar events, and see them in the main search",
            "Choose your first day of the week, with new Calendar settings for categories",
            "Choose a 12-hour, 24-hour or system time format",
            "Rename, favorite and delete notes right from search results"
        ) + if (isDesktopPlatform) {
            listOf(
                "Split view: open up to four screens side by side",
                "Hover tooltips on icon buttons",
                "Zoom full screen images with Ctrl and scroll",
                "A calendar view switcher"
            )
        } else {
            listOf(
                "Customize the keyboard toolbar: add, remove and drag to reorder buttons",
                "Long-press tooltips on icon buttons"
            )
        }
    ),
    UpdateNotesSection(
        heading = "Improved",
        items = listOf(
            "The cursor lands exactly where you tap in text, tables and databases",
            "Database blocks scroll smoothly, and each device remembers the open database view",
            "Pasted blocks keep their images"
        ) + if (isDesktopPlatform) {
            listOf(
                "Menus open submenus on hover",
                "A new note menu with Note, Canvas and Templates",
                "Ctrl+Z and Ctrl+Y work even without a cursor",
                "The sidebar stays collapsed after a restart",
                "Emberr follows display scale changes on Linux without a restart"
            )
        } else {
            listOf(
                "The new note menu opens right next to its button",
                "Indent buttons added to the keyboard toolbar"
            )
        }
    ),
    UpdateNotesSection(
        heading = "Fixed",
        items = listOf(
            "Images no longer stretch when a note opens",
            "Tapping a table cell no longer makes the page jump"
        ) + (if (isDesktopPlatform) listOf("Right-click works on text selected across blocks") else emptyList()) + listOf(
            "Fixed some more bugs and improved UI/UX"
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
                .background(
                    if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.background
                )
                .padding(24.dp)
        ) {
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

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                CurrentUpdateNotes.forEach { section ->
                    UpdateNotesSectionView(section)
                }
            }

            Spacer(Modifier.height(24.dp))

            EmberrButtonPrimary(
                text = "Got it",
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            )
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
