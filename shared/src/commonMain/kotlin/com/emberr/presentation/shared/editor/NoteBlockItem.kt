package com.emberr.presentation.shared.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.DEFAULT_ALL_DAY_REMINDER_HOUR
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.UnknownBlock
import com.emberr.domain.model.VoiceBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.highlightColorNameOrNull
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.model.lastDate
import com.emberr.domain.model.textAlignmentOrNull
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.triggerHapticFeedback
import com.emberr.domain.util.time.formatTimeOfDay
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.MinimalTimePickerDialog
import com.emberr.presentation.shared.components.ReminderPresetMenu
import com.emberr.presentation.shared.components.TimePresetMenu
import com.emberr.presentation.shared.editor.blockViews.callout.CalloutOptionsMenu
import com.emberr.presentation.shared.editor.blockViews.callout.accentColor
import com.emberr.presentation.shared.editor.blockViews.callout.icon
import com.emberr.presentation.shared.editor.formatting.HighlightColorCircles
import com.emberr.presentation.shared.editor.formatting.HighlightColorMenuWidth
import com.emberr.presentation.shared.editor.formatting.TextFormatContextMenu
import com.emberr.presentation.shared.editor.links.HoveredLink
import com.emberr.presentation.shared.editor.links.LinkContextMenu
import com.emberr.presentation.shared.editor.links.LinkHoverCard
import com.emberr.presentation.shared.editor.links.hoveredLinkAt
import com.emberr.presentation.shared.editor.links.linkHover
import com.emberr.presentation.shared.editor.links.openLinksOnPress
import com.emberr.presentation.shared.editor.links.rememberLinkHoverState
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import com.emberr.presentation.shared.editor.markdown.MarkdownClipboardActions
import com.emberr.presentation.shared.editor.markdown.copyEmberrMarkdown
import com.emberr.presentation.shared.editor.markdown.inlineMarkdownOf
import com.emberr.presentation.shared.editor.selection.LocalCrossBlockSelection
import com.emberr.presentation.shared.editor.text.ScrollOnlyWhenHiddenBringIntoViewSpec
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.LocalEmberrFontStyle
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.fontFamilyFor
import com.emberr.ui.theme.highlightBackgroundFor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar_add
import emberr.shared.generated.resources.clock_circle
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import com.emberr.presentation.shared.editor.links.CanvasLinkMenu
import com.emberr.presentation.shared.editor.links.DatabaseLinkMenu
import com.emberr.presentation.shared.editor.links.NoteLinkMenu
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.filteredSlashMenuSections
import com.emberr.presentation.shared.editor.selection.BlockSelectionOverlay

val DefaultBlockShape = RoundedCornerShape(12.dp)

private fun TextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    TextAlignment.LEFT -> TextAlign.Left
    TextAlignment.RIGHT -> TextAlign.Right
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.JUSTIFY -> TextAlign.Justify
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteBlockItem(
    block: NoteBlock,
    actions: EditorActions,
    displayedIndentationLevel: Int = block.indentationLevel,
    showsPinIcon: Boolean = block.isPinned,
    focusRequest: FocusRequest?,
    selectedBlockIds: ImmutableSet<String>,
    inSelectionMode: Boolean,
    activeBlockId: String?,
    onFocus: (String) -> Unit,
    modifier: Modifier = Modifier,
    showSlashMenu: Boolean = false,
    slashQuery: String = "",
    allLinkableNotes: List<NoteMetadataEntity> = emptyList(),
    customProperties: List<CustomPropertyEntity> = emptyList(),
    onDismissSlashMenu: () -> Unit = {},
    showNoteLinkMenu: Boolean = false,
    onDismissNoteLinkMenu: () -> Unit = {},
    showCanvasLinkMenu: Boolean = false,
    onDismissCanvasLinkMenu: () -> Unit = {},
    showDatabaseLinkMenu: Boolean = false,
    onDismissDatabaseLinkMenu: () -> Unit = {},
    onLinkMenuBack: (() -> Unit)? = null,
    isFirstChildOfToggleOrCallout: Boolean = false,
    emptyNotePlaceholder: String = "",
    selectionRequest: SelectionRequest? = null,
    validNoteIds: Set<String> = emptySet(),
    onRightClick: ((Offset) -> Unit)? = null,
    onShiftClick: ((String) -> Unit)? = null,
    trailingLabel: String? = null,
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    val isActiveBlock = block.id == activeBlockId
    val isSelected = selectedBlockIds.contains(block.id)

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }
    var displayedCursorOffsetFromTap by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    val crossBlockSelection = LocalCrossBlockSelection.current

    val text = when (block) {
        is CodeBlock -> block.code
        is QuoteBlock -> block.text
        is TextBlock -> block.text
        is HeadingBlock -> block.text
        is CheckboxBlock -> block.text
        is BulletedListBlock -> block.text
        is NumberedListBlock -> block.text
        is ToggleBlock -> block.text
        is CalloutBlock -> block.text
        is BookmarkBlock, is ImageBlock, is DocumentBlock, is TableBlock, is VoiceBlock -> ""
        else -> ""
    }

    val placeholderText = when {
        block is CheckboxBlock && block.reminderTimestamp != null -> "Untitled event"
        block is CalloutBlock -> block.calloutType.label
        isFirstChildOfToggleOrCallout -> "Type something..."
        else -> emptyNotePlaceholder
    }

    var showPresetMenu by remember { mutableStateOf(false) }
    var showTimePresetMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var isReminderPickerOpening by remember { mutableStateOf(false) }
    var showCalloutOptions by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val webLinkActions = rememberWebLinkActions()
    val linkHoverState = rememberLinkHoverState()
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen = imeInsets.getBottom(density) > 0

    val afterKeyboardCloses: (action: () -> Unit) -> Unit = { action ->
        if (!isKeyboardOpen) {
            action()
        } else {
            isReminderPickerOpening = true
            focusManager.clearFocus()
            keyboardController?.hide()
            scope.launch {
                withTimeoutOrNull(600.milliseconds) {
                    snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 }
                }
                delay(50L.milliseconds)
                action()
                isReminderPickerOpening = false
            }
        }
    }

    val isTextBased = block !is BookmarkBlock && block !is ImageBlock && block !is DocumentBlock && block !is TableBlock && block !is VoiceBlock && block !is SolidDividerBlock && block !is ThreeDotDividerBlock && block !is LinkedNoteBlock && block !is CanvasBlock && block !is PropertyBlock && block !is DatabaseBlock && block !is MovieBlock && block !is WatchlistBlock && block !is CredentialsBlock && block !is UnknownBlock
    LaunchedEffect(focusRequest?.nonce) {
        if (focusRequest == null || focusRequest.id != block.id) return@LaunchedEffect

        if (!isTextBased) {
            actions.onClearFocusRequest()
            return@LaunchedEffect
        }

        val grabbed = runCatching { focusRequester.requestFocus() }.isSuccess
        if (!grabbed) {
            withFrameNanos {}
            runCatching { focusRequester.requestFocus() }
        }
        if (!isKeyboardOpen) keyboardController?.show()
        actions.onClearFocusRequest()
    }

    val baseStyle = when (block) {
        is HeadingBlock -> TextStyle(
            fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
            fontSize = if (block.level == 1) {
                if (isDesktopPlatform) 32.sp else 26.sp
            } else {
                if (isDesktopPlatform) 24.sp else 20.sp
            },
            lineHeight = if (block.level == 1) {
                if (isDesktopPlatform) 40.sp else 32.sp
            } else {
                if (isDesktopPlatform) 32.sp else 26.sp
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        is CodeBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        is QuoteBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
        is CalloutBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = block.calloutType.accentColor()
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    val isCheckboxChecked = block is CheckboxBlock && block.isChecked
    val applyStrikeThrough = block.isStrikeThrough || isCheckboxChecked
    val isDarkTheme = LocalAppIsDark.current

    val textStyle = baseStyle.copy(
        fontWeight = if (block.isBold) FontWeight.Bold else baseStyle.fontWeight,
        fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            applyStrikeThrough && block.isUnderlined -> TextDecoration.LineThrough + TextDecoration.Underline
            applyStrikeThrough -> TextDecoration.LineThrough
            block.isUnderlined -> TextDecoration.Underline
            else -> TextDecoration.None
        },
        textAlign = block.textAlignmentOrNull()?.toComposeTextAlign() ?: TextAlign.Unspecified,
        color = if (isCheckboxChecked) MaterialTheme.colorScheme.outline else baseStyle.color,
        background = if (block.isHighlighted) {
            highlightBackgroundFor(block.highlightColorNameOrNull(), isDarkTheme)
        } else Color.Unspecified
    )

    val internalVerticalPadding = when (block) {
        is HeadingBlock -> 8.dp
        is CodeBlock -> 4.dp
        else -> 4.dp
    }

    val clipboardManager = LocalClipboardManager.current
    val markdownClipboardActions = remember { MarkdownClipboardActions() }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )

    val desktopExtraPadding = if (isDesktopPlatform) 16.dp else 0.dp
    val startPadding = when {
        block is TableBlock || block is DatabaseBlock -> (displayedIndentationLevel * 28).dp + desktopExtraPadding
        block is CheckboxBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
        block is BulletedListBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
        block is NumberedListBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
        block is ToggleBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
        else -> (16 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
    }
    val endPadding = (if (block is TableBlock || block is DatabaseBlock) 0.dp else 16.dp) + desktopExtraPadding

    val isSlashMenuActiveHere = isDesktopPlatform && isActiveBlock && showSlashMenu
    val isNoteLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showNoteLinkMenu
    val isCanvasLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showCanvasLinkMenu
    val isDatabaseLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showDatabaseLinkMenu

    var isChoosingHighlightColor by remember { mutableStateOf(false) }

    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val slashMenuSections = remember(slashQuery, isSlashMenuActiveHere, customProperties, usesTwentyFourHourClock) {
        if (isSlashMenuActiveHere) {
            filteredSlashMenuSections(
                query = slashQuery,
                onChangeBlockType = { actions.onChangeBlockType(it) },
                onToggleFormat = { format ->
                    if (format == "highlight") {
                        onDismissSlashMenu()
                        isChoosingHighlightColor = true
                    } else {
                        actions.onToggleFormat(format)
                    }
                },
                onAdjustIndentation = { actions.onAdjustIndentation(it) },
                onSetAlignment = { actions.onSetBlockAlignment(it) },
                onInsertMediaBlock = { actions.onInsertMediaBlock(it) },
                onInsertText = { actions.onReplaceSlashQuery("$it ") },
                usesTwentyFourHourClock = usesTwentyFourHourClock,
                customProperties = customProperties
            )
        } else {
            emptyList()
        }
    }
    val slashMenuFilteredItems = remember(slashMenuSections) { slashMenuSections.flatMap { it.items } }
    var slashMenuSelectedIndex by remember(slashQuery) { mutableIntStateOf(0) }

    var blockTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    val handleRightClick: (Offset) -> Unit = { pressPositionInBlock ->
        if (!isSelected) actions.onToggleSelection(block.id)
        onRightClick?.invoke(blockTopLeftInWindow + pressPositionInBlock)
    }
    val latestRightClickHandler by rememberUpdatedState(handleRightClick)
    val latestShiftClickHandler by rememberUpdatedState(onShiftClick)
    val latestInSelectionMode by rememberUpdatedState(inSelectionMode)

    val blockMouseModifier = if (isDesktopPlatform && (onRightClick != null || onShiftClick != null)) {
        Modifier
            .onGloballyPositioned { blockTopLeftInWindow = it.positionInWindow() }
            .pointerInput(block.id) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type != PointerEventType.Press) continue
                        val press = event.changes.firstOrNull() ?: continue
                        if (press.isConsumed) continue

                        val startsSelectionWithCtrlClick = event.buttons.isPrimaryPressed &&
                            (event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) &&
                            !latestInSelectionMode &&
                            !GlobalEditorState.isPointerOverWatchlistTitle
                        if (startsSelectionWithCtrlClick) {
                            press.consume()
                            actions.onToggleSelection(block.id)
                            continue
                        }
                        if (isFocused) continue

                        val opensContextMenu = event.buttons.isSecondaryPressed &&
                            linkHoverState.hoveredLink == null &&
                            !GlobalEditorState.isPointerOverDatabaseViewPill
                        val extendsSelection = event.buttons.isPrimaryPressed &&
                            event.keyboardModifiers.isShiftPressed &&
                            latestInSelectionMode

                        if (opensContextMenu) {
                            press.consume()
                            latestRightClickHandler(press.position)
                        } else if (extendsSelection) {
                            press.consume()
                            latestShiftClickHandler?.invoke(block.id)
                        }
                    }
                }
            }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(blockMouseModifier)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = if (isDesktopPlatform) {
                    null
                } else {
                    { if (!GlobalEditorState.pressStartedOnViewPill) actions.onToggleSelection(block.id) }
                }
            )
    ) {
        if (isChoosingHighlightColor) {
            EmberrDesktopMenu(
                expanded = true,
                onDismissRequest = { isChoosingHighlightColor = false },
                modifier = Modifier.width(HighlightColorMenuWidth)
            ) {
                HighlightColorCircles(
                    onColorChosen = { formatWithColor -> actions.onToggleFormat(formatWithColor) },
                    onClose = { isChoosingHighlightColor = false }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = startPadding, end = endPadding)
                .then(
                    if (isDesktopPlatform && crossBlockSelection != null) {
                        Modifier.onPlaced { crossBlockSelection.rememberBlockContentCoordinates(block.id, it) }
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = internalVerticalPadding),
            verticalAlignment = Alignment.Top
        ) {
            val iconOffset = when (block) {
                is HeadingBlock -> if (block.level == 1) 4.dp else 2.dp
                is CodeBlock -> 12.dp
                else -> (-2).dp
            }

            if (block is CheckboxBlock || block is BulletedListBlock || block is NumberedListBlock || block is ToggleBlock || block is CalloutBlock) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .offset(y = iconOffset)
                        .size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (block) {
                        is CheckboxBlock -> EmberrCheckbox(
                            checked = block.isChecked,
                            onCheckedChange = {
                                triggerHapticFeedback()
                                actions.onToggleCheckbox(block.id, it)
                            }
                        )
                        is BulletedListBlock -> Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(textStyle.color))
                        is NumberedListBlock -> Text("${block.number}.", style = textStyle.copy(fontSize = 17.sp))
                        is ToggleBlock -> {
                            val rotation by animateFloatAsState(if (block.isExpanded) 90f else 0f, label = "toggleRotation")
                            Icon(
                                Icons.Default.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(32.dp).rotate(rotation).clickable { actions.onToggleExpand(block.id) }
                            )
                        }
                        is CalloutBlock -> {
                            Icon(
                                painterResource(block.calloutType.icon()), "Callout options",
                                tint = block.calloutType.accentColor(),
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { afterKeyboardCloses { showCalloutOptions = true } }
                                    .padding(2.dp)
                            )
                            CalloutOptionsMenu(
                                expanded = showCalloutOptions,
                                block = block,
                                onDismiss = { showCalloutOptions = false },
                                onUpdateStyle = { calloutType, isFoldable -> actions.onUpdateCalloutStyle(block.id, calloutType, isFoldable) }
                            )
                        }
                    }
                }
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            val quoteAccentColor = Color(0xFFFFD54F)
            val DefaultBlockShape = RoundedCornerShape(12.dp)
            val textFieldWrapperModifier = (if (block is CodeBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, DefaultBlockShape).padding(12.dp)
            } else if (block is QuoteBlock) {
                Modifier.weight(1f).padding(horizontal = 4.dp).drawBehind {
                    drawLine(color = quoteAccentColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
                }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
            } else if (block is TableBlock) {
                Modifier.weight(1f)
            } else {
                Modifier.weight(1f).padding(horizontal = 4.dp)
            })
                .onFocusChanged { focusState ->
                    val currentlyFocused = focusState.isFocused || focusState.hasFocus
                    isFocused = currentlyFocused
                    if (currentlyFocused) onFocus(block.id) else GlobalEditorState.currentSelection = TextRange.Zero
                }

            val linkColor = MaterialTheme.colorScheme.primary
            val fadedLinkColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            val inlineSpans = block.inlineSpansOrEmpty()
            val noteTitlesById = remember(allLinkableNotes) { allLinkableNotes.associate { it.noteId to it.title } }
            val richTextTransformation: VisualTransformation = remember(
                block is CodeBlock, linkColor, fadedLinkColor, isDarkTheme, validNoteIds, inlineSpans, linkHoverState.hoveredLink, noteTitlesById
            ) {
                if (block is CodeBlock) VisualTransformation.None
                else RichTextVisualTransformation(linkColor, fadedLinkColor, isDarkTheme, validNoteIds, inlineSpans, linkHoverState.hoveredLink, noteTitlesById)
            }
            val latestText by rememberUpdatedState(text)
            val latestRichTextTransformation by rememberUpdatedState(richTextTransformation)

            Column(modifier = textFieldWrapperModifier) {
                if (isTextBased) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .linkHover(linkHoverState, validNoteIds) { textLayoutResult }
                            .openLinksOnPress(
                                validNoteIds = validNoteIds,
                                onOpenWebLink = { webLinkActions.openLink(it) },
                                onOpenNoteLink = { actions.onNoteLinkClick(it) },
                                onOpenEmail = { webLinkActions.openEmail(it) },
                                onOpenPhone = { webLinkActions.openPhone(it) },
                                onRightClickLink = { link, at -> linkHoverState.openMenuFor(link, at) },
                                currentTextLayout = { textLayoutResult }
                            )
                    ) {
                        CompositionLocalProvider(
                            LocalTextSelectionColors provides customTextSelectionColors,
                            LocalBringIntoViewSpec provides ScrollOnlyWhenHiddenBringIntoViewSpec
                        ) {

                            TextFormatContextMenu(
                                onToggleFormat = { actions.onToggleFormat(it) },
                                markdownClipboardActions = markdownClipboardActions
                            ) {
                                IsolatedEditorTextField(
                                    initialText = text,
                                    placeholderText = placeholderText,
                                    placeholderColor = if (block is CalloutBlock) block.calloutType.accentColor().copy(alpha = 0.7f) else Color.Unspecified,
                                    blockId = block.id,
                                    isCodeBlock = block is CodeBlock,
                                    textStyle = textStyle,
                                    inSelectionMode = inSelectionMode,
                                    focusRequester = focusRequester,
                                    onUpdateText = { id, newText -> actions.onUpdateText(id, newText) },
                                    onEnterPressed = { id, before, after -> actions.onEnterPressed(id, before, after) },
                                    onShiftEnterPressed = { id, before, after -> actions.onShiftEnterPressed(id, before, after) },
                                    onBackspaceOnEmpty = { id -> actions.onBackspaceOnEmpty(id) },
                                    onBackspaceAtStart = { id -> actions.onBackspaceAtStartOfText(id) },
                                    onDeleteAtEnd = { id -> actions.onDeleteAtEndOfText(id) },
                                    backspaceLeavesTopOfNote = { id -> actions.backspaceLeavesTopOfNote(id) },
                                    breaksLineOnShiftEnter = block is TextBlock || block is HeadingBlock,
                                    onIndentRequested = { increase -> actions.onAdjustIndentation(increase) },
                                    onMarkdownShortcut = { id, literalText, shortcut -> actions.onMarkdownShortcut(id, literalText, shortcut) },
                                    onPasteMarkdown = { id, pasted ->
                                        actions.onPasteMarkdown(id, pasted.replacedStart, pasted.replacedEnd, pasted.text) != null
                                    },
                                    markdownClipboardActions = markdownClipboardActions,
                                    onCopyAsMarkdown = if (block !is CodeBlock) {
                                        { start, end -> clipboardManager.copyEmberrMarkdown(inlineMarkdownOf(block, start, end)) }
                                    } else {
                                        null
                                    },
                                    allLinkableNotes = allLinkableNotes,
                                    onCreateLinkedNote = { actions.onCreateLinkedNote(it) },
                                    onOpenNote = { actions.onNoteLinkClick(it) },
                                    visualTransformation = richTextTransformation,
                                    selectionRequest = selectionRequest,
                                    focusRequest = focusRequest,
                                    onTextLayout = { textLayoutResult = it },
                                    displayedCursorOffsetFromTap = displayedCursorOffsetFromTap,
                                    onCursorFromTapPlaced = { displayedCursorOffsetFromTap = null },
                                    isSlashMenuOpen = isSlashMenuActiveHere,
                                    onSlashMenuNavigate = { delta ->
                                        val count = slashMenuFilteredItems.size
                                        if (count > 0) {
                                            slashMenuSelectedIndex = ((slashMenuSelectedIndex + delta) % count + count) % count
                                        }
                                    },
                                    onSlashMenuConfirm = {
                                        slashMenuFilteredItems.getOrNull(slashMenuSelectedIndex)?.action?.invoke()
                                    },
                                    onSlashMenuDismiss = onDismissSlashMenu
                                )
                            }

                        }

                        if (isSlashMenuActiveHere || isNoteLinkMenuActiveHere || isCanvasLinkMenuActiveHere || isDatabaseLinkMenuActiveHere) {
                            val slashRect = remember(text, richTextTransformation, textLayoutResult) {
                                displayedCharacterRect(text, text.lastIndexOf('/'), richTextTransformation, textLayoutResult) ?: Rect.Zero
                            }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(slashRect.left.roundToInt(), slashRect.top.roundToInt()) }
                                    .size(width = 1.dp, height = with(density) { slashRect.height.toDp() })
                            ) {
                                if (isSlashMenuActiveHere) {
                                    EmberrDesktopMenu(
                                        expanded = true,
                                        onDismissRequest = onDismissSlashMenu,
                                        properties = PopupProperties(focusable = false),
                                        modifier = Modifier
                                            .width(290.dp)
                                            .heightIn(max = 400.dp)
                                    ) {
                                        SlashMenuList(
                                            sections = slashMenuSections,
                                            selectedIndex = slashMenuSelectedIndex
                                        )
                                    }
                                }

                                if (isNoteLinkMenuActiveHere) {
                                    NoteLinkMenu(
                                        expanded = true,
                                        onDismissRequest = onDismissNoteLinkMenu,
                                        onBack = onLinkMenuBack?.let { goBack -> { onDismissNoteLinkMenu(); goBack() } },
                                        allLinkableNotes = allLinkableNotes,
                                        onNoteSelected = { noteId ->
                                            actions.onInsertLinkedNoteBlock(noteId)
                                            onDismissNoteLinkMenu()
                                        },
                                        onCreateNote = { title ->
                                            val newNoteId = actions.onCreateLinkedNote(title)
                                            actions.onInsertLinkedNoteBlock(newNoteId)
                                            onDismissNoteLinkMenu()
                                        },
                                        onCreateBlankNote = {
                                            val newNoteId = actions.onCreateLinkedNote("Untitled")
                                            actions.onInsertLinkedNoteBlock(newNoteId)
                                            onDismissNoteLinkMenu()
                                            actions.onNoteLinkClick(newNoteId)
                                        }
                                    )
                                }

                                if (isCanvasLinkMenuActiveHere) {
                                    CanvasLinkMenu(
                                        expanded = true,
                                        onDismissRequest = onDismissCanvasLinkMenu,
                                        onBack = onLinkMenuBack?.let { goBack -> { onDismissCanvasLinkMenu(); goBack() } },
                                        loadCanvases = { actions.getLinkableCanvases() },
                                        onCanvasSelected = { canvasNoteId ->
                                            actions.onInsertCanvasBlock(canvasNoteId)
                                            onDismissCanvasLinkMenu()
                                        }
                                    )
                                }

                                if (isDatabaseLinkMenuActiveHere) {
                                    DatabaseLinkMenu(
                                        expanded = true,
                                        onDismissRequest = onDismissDatabaseLinkMenu,
                                        onBack = onLinkMenuBack?.let { goBack -> { onDismissDatabaseLinkMenu(); goBack() } },
                                        loadDatabases = { actions.getLinkableDatabases() },
                                        onDatabaseSelected = { databaseId ->
                                            actions.onInsertLinkedDatabaseBlock(databaseId)
                                            onDismissDatabaseLinkMenu()
                                        }
                                    )
                                }
                            }
                        }

                        if (!isFocused && !inSelectionMode) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerHoverIcon(PointerIcon.Text)
                                    .pointerInput(block.id) {
                                        detectTapGestures(
                                            onTap = { pos ->
                                                val layoutResult = textLayoutResult
                                                val tappedLink = layoutResult?.hoveredLinkAt(pos, validNoteIds)

                                                when (tappedLink) {
                                                    is HoveredLink.Web -> webLinkActions.openLink(tappedLink.url)
                                                    is HoveredLink.Email -> webLinkActions.openEmail(tappedLink.email)
                                                    is HoveredLink.Phone -> webLinkActions.openPhone(tappedLink.phone)
                                                    is HoveredLink.Note -> actions.onNoteLinkClick(tappedLink.noteId)
                                                    null -> layoutResult?.let { displayedCursorOffsetFromTap = it.getOffsetForPosition(pos) }
                                                }
                                            },
                                            onLongPress = { pos ->
                                                when (val pressedLink = textLayoutResult?.hoveredLinkAt(pos, validNoteIds)) {
                                                    is HoveredLink.Web -> webLinkActions.copyLink(pressedLink.url)
                                                    is HoveredLink.Email -> webLinkActions.copyLink(pressedLink.email)
                                                    is HoveredLink.Phone -> webLinkActions.copyLink(pressedLink.phone)
                                                    else -> if (!isDesktopPlatform) {
                                                        val pressedWord = storedWordAt(pos, latestText, latestRichTextTransformation, textLayoutResult)
                                                        if (crossBlockSelection != null && pressedWord != null) {
                                                            crossBlockSelection.selectWord(block.id, pressedWord)
                                                        } else {
                                                            actions.onToggleSelection(block.id)
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                            )
                        }

                        LinkHoverCard(
                            hoverState = linkHoverState,
                            onOpenNoteLink = { actions.onNoteLinkClick(it) },
                            findNote = { noteId -> allLinkableNotes.find { it.noteId == noteId } }
                        )

                        LinkContextMenu(
                            hoverState = linkHoverState,
                            onOpenNoteLink = { actions.onNoteLinkClick(it) },
                            findNote = { noteId -> allLinkableNotes.find { it.noteId == noteId } }
                        )
                    }

                    if (block is CheckboxBlock) {
                        val hasReminder = block.reminderTimestamp != null
                        val isPickerOpen = showPresetMenu || showTimePresetMenu || showDatePicker ||
                            showTimePicker || isReminderPickerOpening
                        AnimatedVisibility(
                            visible = isActiveBlock || hasReminder || isPickerOpen,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (hasReminder) {
                                    IconTooltip(text = "More details") {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                            .clickable {
                                                val occurrenceDate = block.reminderTimestamp.let {
                                                    Instant.fromEpochMilliseconds(it)
                                                        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
                                                }
                                                afterKeyboardCloses {
                                                    actions.onOpenEventOptions(block.id, occurrenceDate)
                                                }
                                            }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.MoreVert, "Event options",
                                                modifier = Modifier.size(15.dp),
                                                tint = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                                Box {
                                    IconTooltip(text = "Date") {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    isReminderPickerOpening = true
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    scope.launch {
                                                        if (isKeyboardOpen) delay(500L.milliseconds) else delay(
                                                            50L.milliseconds
                                                        )
                                                        showPresetMenu = true
                                                        isReminderPickerOpening = false
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painterResource(Res.drawable.calendar_add), "Date",
                                                modifier = Modifier.size(15.dp),
                                                tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    ReminderPresetMenu(
                                        expanded = showPresetMenu,
                                        onDismiss = { showPresetMenu = false },
                                        onPresetSelected = {
                                            actions.onUpdateReminder(
                                                block.id,
                                                block.reminderTimestampOnNewDate(it),
                                                block.allDay
                                            )
                                        },
                                        onCustomSelected = { showDatePicker = true },
                                        customDateInitialTimestamp = block.reminderTimestamp,
                                        onCustomDateChosen = { timestamp ->
                                            actions.onUpdateReminder(block.id, block.reminderTimestampOnNewDate(timestamp), block.allDay)
                                        },
                                        onRemove = if (hasReminder) {
                                            { actions.onUpdateReminder(block.id, null, null) }
                                        } else null
                                    )
                                }
                                Box {
                                    IconTooltip(text = "Time") {
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    isReminderPickerOpening = true
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    scope.launch {
                                                        if (isKeyboardOpen) delay(500L.milliseconds) else delay(
                                                            50L.milliseconds
                                                        )
                                                        showTimePresetMenu = true
                                                        isReminderPickerOpening = false
                                                    }
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painterResource(Res.drawable.clock_circle), "Time",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (hasReminder) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    TimePresetMenu(
                                        expanded = showTimePresetMenu,
                                        onDismiss = { showTimePresetMenu = false },
                                        onPresetSelected = {
                                            actions.onUpdateReminder(
                                                block.id,
                                                it,
                                                null
                                            )
                                        },
                                        onCustomSelected = { showTimePicker = true },
                                        customTimeInitialTimestamp = block.reminderTimestamp,
                                        onCustomTimeChosen = { hour, minute ->
                                            actions.onUpdateReminder(block.id, reminderTimestampAtTime(block.reminderTimestamp, hour, minute), null)
                                        },
                                        isAllDay = block.allDay != null,
                                        onAllDaySelected = {
                                            if (block.allDay == null) {
                                                actions.onUpdateReminder(
                                                    block.id,
                                                    reminderTimestampAtTime(block.reminderTimestamp, DEFAULT_ALL_DAY_REMINDER_HOUR, 0),
                                                    AllDaySpan()
                                                )
                                            }
                                        }
                                    )
                                }
                                if (hasReminder) {
                                    val timeText = remember(block.reminderTimestamp, block.allDay, usesTwentyFourHourClock) {
                                        val instant = Instant.fromEpochMilliseconds(block.reminderTimestamp)
                                        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                        val allDay = block.allDay
                                        when {
                                            allDay == null -> "${shortDateLabel(dt.date)}, ${formatTimeOfDay(dt.hour, dt.minute, usesTwentyFourHourClock)}"
                                            allDay.lengthInDays > 1 -> "${shortDateLabel(dt.date)} – ${shortDateLabel(allDay.lastDate(dt.date))}, All day"
                                            else -> "${shortDateLabel(dt.date)}, All day"
                                        }
                                    }
                                    val timeZone = TimeZone.currentSystemDefault()
                                    val reminderDate = Instant.fromEpochMilliseconds(block.reminderTimestamp)
                                        .toLocalDateTime(timeZone).date
                                    val lastReminderDate = block.allDay?.lastDate(reminderDate) ?: reminderDate
                                    val isOverdue = !block.isChecked && lastReminderDate < Clock.System.todayIn(timeZone)
                                    Text(
                                        text = timeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOverdue) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    key(block.id) {
                        NonTextBlockContent(
                            block = block,
                            actions = actions,
                            inSelectionMode = inSelectionMode,
                            focusRequest = focusRequest,
                            afterKeyboardCloses = afterKeyboardCloses
                        )
                    }
                }
            }

            if (block is CalloutBlock && block.isFoldable) {
                val foldRotation by animateFloatAsState(
                    targetValue = if (block.isExpanded) 90f else 0f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "calloutFoldRotation"
                )
                Icon(
                    Icons.Default.ChevronRight, if (block.isExpanded) "Fold callout" else "Unfold callout",
                    tint = block.calloutType.accentColor(),
                    modifier = Modifier
                        .padding(end = if (showsPinIcon) 20.dp else 0.dp)
                        .size(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { actions.onToggleExpand(block.id) }
                        .rotate(foldRotation)
                )
            }

            if (trailingLabel != null) {
                Text(
                    text = trailingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    maxLines = 1,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }
        }

        if (showsPinIcon) {
            Icon(
                Icons.Default.PushPin, "Pinned",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 16.dp)
                    .size(15.dp)
            )
        }

        if (block is CheckboxBlock) {
            if (showDatePicker) {
                MinimalDatePickerDialog(
                    initialTimestamp = block.reminderTimestamp,
                    onDismiss = { showDatePicker = false },
                    onConfirm = { timestamp ->
                        actions.onUpdateReminder(block.id, block.reminderTimestampOnNewDate(timestamp), block.allDay)
                        showDatePicker = false
                    }
                )
            }
            if (showTimePicker) {
                MinimalTimePickerDialog(
                    initialTimestamp = block.reminderTimestamp,
                    onDismiss = { showTimePicker = false },
                    onConfirm = { hour, minute ->
                        actions.onUpdateReminder(block.id, reminderTimestampAtTime(block.reminderTimestamp, hour, minute), null)
                        showTimePicker = false
                    }
                )
            }
        }

        BlockSelectionOverlay(
            blockId = block.id,
            isSelected = isSelected,
            inSelectionMode = inSelectionMode,
            horizontalPadding = 8.dp + desktopExtraPadding,
            onToggleSelection = { actions.onToggleSelection(block.id) }
        )
    }
}

private fun storedWordAt(
    position: Offset,
    text: String,
    visualTransformation: VisualTransformation,
    textLayout: TextLayoutResult?
): TextRange? {
    if (textLayout == null) return null
    val line = textLayout.getLineForVerticalPosition(position.y)
    val isOnText = position.y <= textLayout.getLineBottom(line) &&
        position.x >= textLayout.getLineLeft(line) &&
        position.x <= textLayout.getLineRight(line)
    if (!isOnText) return null
    val displayedWord = textLayout.getWordBoundary(textLayout.getOffsetForPosition(position))
    if (displayedWord.collapsed) return null
    val offsetMapping = visualTransformation.filter(AnnotatedString(text)).offsetMapping
    return TextRange(offsetMapping.transformedToOriginal(displayedWord.start), offsetMapping.transformedToOriginal(displayedWord.end))
}

private fun shortDateLabel(date: LocalDate): String =
    "${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }} ${date.day}"

private fun CheckboxBlock.reminderTimestampOnNewDate(newTimestamp: Long): Long {
    val currentTimestamp = reminderTimestamp
    if (allDay == null || currentTimestamp == null) return newTimestamp
    val currentTime = Instant.fromEpochMilliseconds(currentTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
    return reminderTimestampAtTime(newTimestamp, currentTime.hour, currentTime.minute)
}

private fun reminderTimestampAtTime(currentTimestamp: Long?, hour: Int, minute: Int): Long {
    val timeZone = TimeZone.currentSystemDefault()
    val currentInstant = currentTimestamp?.let { Instant.fromEpochMilliseconds(it) } ?: Clock.System.now()
    val currentDateTime = currentInstant.toLocalDateTime(timeZone)
    val newDateTime = LocalDateTime(
        currentDateTime.year, currentDateTime.month.number, currentDateTime.day,
        hour, minute, 0, 0
    )
    return newDateTime.toInstant(timeZone).toEpochMilliseconds()
}
