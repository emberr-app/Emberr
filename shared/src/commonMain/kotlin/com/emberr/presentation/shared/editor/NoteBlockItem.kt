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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
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
import com.emberr.domain.model.DEFAULT_ALL_DAY_REMINDER_HOUR
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.LinkedNoteBlock
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
import com.emberr.presentation.shared.editor.blockViews.media.AudioBlockView
import com.emberr.presentation.shared.editor.blockViews.media.BookmarkBlockView
import com.emberr.presentation.shared.editor.blockViews.callout.CalloutOptionsMenu
import com.emberr.presentation.shared.editor.blockViews.CanvasBlockView
import com.emberr.presentation.shared.editor.blockViews.media.DocumentBlockView
import com.emberr.presentation.shared.editor.blockViews.media.ImageBlockView
import com.emberr.presentation.shared.editor.blockViews.LinkedNoteBlockView
import com.emberr.presentation.shared.editor.blockViews.TableBlockView
import com.emberr.presentation.shared.editor.blockViews.UnknownBlockView
import com.emberr.presentation.shared.editor.blockViews.callout.accentColor
import com.emberr.presentation.shared.editor.blockViews.database.DatabaseBlockView
import com.emberr.presentation.shared.editor.blockViews.callout.icon
import com.emberr.presentation.shared.editor.blockViews.property.PropertyBlockView
import com.emberr.presentation.shared.editor.formatting.HighlightColorCircles
import com.emberr.presentation.shared.editor.formatting.HighlightColorMenuWidth
import com.emberr.presentation.shared.editor.formatting.TextFormatContextMenu
import com.emberr.presentation.shared.editor.links.HoveredLink
import com.emberr.presentation.shared.editor.links.LinkContextMenu
import com.emberr.presentation.shared.editor.links.LinkHoverCard
import com.emberr.presentation.shared.editor.links.NOTE_LINK_TAG
import com.emberr.presentation.shared.editor.links.hoveredLinkAt
import com.emberr.presentation.shared.editor.links.linkHover
import com.emberr.presentation.shared.editor.links.openLinksOnPress
import com.emberr.presentation.shared.editor.links.rememberLinkHoverState
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import com.emberr.presentation.shared.editor.links.withInteractiveLinksHighlighted
import com.emberr.presentation.shared.editor.markdown.MarkdownClipboardActions
import com.emberr.presentation.shared.editor.markdown.MarkdownShortcut
import com.emberr.presentation.shared.editor.markdown.PastedText
import com.emberr.presentation.shared.editor.markdown.copyEmberrMarkdown
import com.emberr.presentation.shared.editor.markdown.inlineMarkdownOf
import com.emberr.presentation.shared.editor.markdown.markdownShortcutAfterTyping
import com.emberr.presentation.shared.editor.markdown.pastedTextToReadAsMarkdown
import com.emberr.presentation.shared.editor.selection.BlockTextPosition
import com.emberr.presentation.shared.editor.selection.LocalCrossBlockSelection
import com.emberr.presentation.shared.editor.selection.SelectableTextField
import com.emberr.presentation.shared.editor.selection.SelectionStep
import com.emberr.presentation.shared.editor.selection.selectionStepFor
import com.emberr.presentation.shared.editor.selection.textInsertedBetween
import com.emberr.presentation.shared.editor.text.ScrollOnlyWhenHiddenBringIntoViewSpec
import com.emberr.presentation.shared.editor.text.showCursorLineInsteadOfOtherLines
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
import kotlin.time.TimeSource

val DefaultBlockShape = RoundedCornerShape(12.dp)

private const val NOTE_LINK_MARKER = "](emberr://note/"
private val NoteLinkRegex = """\[([^\]]+)\]\(emberr://note/([^)]+)\)""".toRegex()
private val TrailingNoteLinkRegex = """\[([^\]]+)\]\(emberr://note/([^)]+)\)$""".toRegex()
private val EmptyLineSelectionWidth = 6.dp
private val BlockBoundaryEditGap = 100.milliseconds

private fun storedOffsetBesideSkippingNoteLinks(text: String, storedOffset: Int, forward: Boolean): Int? {
    val candidate = if (forward) storedOffset + 1 else storedOffset - 1
    if (candidate !in 0..text.length) return null
    val noteLinkAroundCandidate = NoteLinkRegex.findAll(text).firstOrNull { link ->
        candidate > link.range.first && candidate <= link.range.last
    }
    return when {
        noteLinkAroundCandidate == null -> candidate
        forward -> noteLinkAroundCandidate.range.last + 1
        else -> noteLinkAroundCandidate.range.first
    }
}

private fun TextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    TextAlignment.LEFT -> TextAlign.Left
    TextAlignment.RIGHT -> TextAlign.Right
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.JUSTIFY -> TextAlign.Justify
}

// All NoteBlock subtypes are @Immutable: never mutate in place, always .copy()
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
    isFirstChildOfToggleOrCallout: Boolean = false,
    emptyNotePlaceholder: String = "",
    selectionRequest: SelectionRequest? = null,
    validNoteIds: Set<String> = emptySet(),
    onRightClick: ((Offset) -> Unit)? = null,
    onShiftClick: ((String) -> Unit)? = null,
) {
    // STANDARD BLOCK LOGIC
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

    // LOCAL STATE FOR INSTANT TYPING
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

    val isTextBased = block !is BookmarkBlock && block !is ImageBlock && block !is DocumentBlock && block !is TableBlock && block !is VoiceBlock && block !is SolidDividerBlock && block !is ThreeDotDividerBlock && block !is LinkedNoteBlock && block !is CanvasBlock && block !is PropertyBlock && block !is DatabaseBlock && block !is UnknownBlock
    LaunchedEffect(focusRequest?.nonce) {
        if (focusRequest == null || focusRequest.id != block.id) return@LaunchedEffect

        if (!isTextBased) {
            // No text field in this branch, so no FocusRequester is attached. Consume the
            // request and leave the existing input session alone rather than failing into
            // a state where nothing is focused.
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

    // STYLING
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
    var selectionOverlayTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    val leaveBlockSelectionAt: (Offset) -> Unit = leave@{ clickInWindow ->
        val selection = crossBlockSelection ?: return@leave
        val caret = selection.positionAt(clickInWindow, mustBeInsideBlock = true)
            ?.takeIf { it.blockId == block.id && selection.hasTextField(block.id) }
        if (caret != null) selection.host?.placeCaret(caret) else selection.host?.clearBlockSelection()
    }

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
                            !latestInSelectionMode
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

    // RENDER BLOCK CONTENT
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
                    { if (!GlobalEditorState.pressStartedOnDatabaseViewPill) actions.onToggleSelection(block.id) }
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

        if (isNoteLinkMenuActiveHere) {
            NoteLinkMenu(
                expanded = true,
                onDismissRequest = onDismissNoteLinkMenu,
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
                loadDatabases = { actions.getLinkableDatabases() },
                onDatabaseSelected = { databaseId ->
                    actions.onInsertLinkedDatabaseBlock(databaseId)
                    onDismissDatabaseLinkMenu()
                }
            )
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

                        if (isSlashMenuActiveHere) {
                            val slashRect = remember(text, richTextTransformation, textLayoutResult) {
                                displayedCharacterRect(text, text.lastIndexOf('/'), richTextTransformation, textLayoutResult) ?: Rect.Zero
                            }
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset(slashRect.left.roundToInt(), slashRect.top.roundToInt()) }
                                    .size(width = 1.dp, height = with(density) { slashRect.height.toDp() })
                            ) {
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
                        when (block) {
                            is BookmarkBlock -> BookmarkBlockView(
                                block, inSelectionMode,
                                { actions.onToggleSelection(block.id) },
                                { actions.onUrlSubmit(block.id, it) }
                            )
                            is LinkedNoteBlock -> LinkedNoteBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) },
                                onOpenNote = { actions.onNoteLinkClick(block.linkedNoteId) },
                                getNoteMetadata = { actions.getNoteMetadata(it) }
                            )
                            is ImageBlock -> {
                                val imageLinkFetches by actions.imageLinkFetches.collectAsState()
                                ImageBlockView(
                                    block, inSelectionMode,
                                    onToggleSelection = { actions.onToggleSelection(block.id) },
                                    onRequestPicker = { actions.onRequestImagePicker(block.id) },
                                    onRequestCamera = { actions.onRequestCamera(block.id) },
                                    imageLinkFetch = imageLinkFetches[block.id],
                                    onImageLinkSubmitted = { address -> actions.onImageLinkSubmitted(block.id, address) },
                                    onDelete = { actions.onDeleteBlock(block.id) }
                                )
                            }
                            is DocumentBlock -> DocumentBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) },
                                onRequestPicker = { actions.onRequestDocumentPicker(block.id) },
                                onOpenFile = { filePath, mimeType -> actions.onOpenFile(filePath, mimeType) }
                            )
                            is TableBlock -> TextFormatContextMenu(onToggleFormat = { actions.onToggleFormat(it) }) {
                                TableBlockView(
                                    block = block,
                                    inSelectionMode = inSelectionMode,
                                    onDelete = { actions.onDeleteBlock(block.id) },
                                    onUpdateTable = { actions.onUpdateTable(block.id, it) },
                                    onUpdateTableStyle = { cellStyles, rowStyles, columnStyles ->
                                        actions.onUpdateTableStyle(block.id, cellStyles, rowStyles, columnStyles)
                                    }
                                )
                            }
                            is CanvasBlock -> CanvasBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) }
                            )
                            is PropertyBlock -> PropertyBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onUpdateText = { actions.onUpdatePropertyText(block.id, it) },
                                onUpdateDate = { actions.onUpdatePropertyDate(block.id, it) },
                                onUpdateTags = { actions.onUpdatePropertyTags(block.id, it) },
                                onUpdateChecked = { actions.onUpdatePropertyChecked(block.id, it) },
                                runAfterKeyboardCloses = afterKeyboardCloses
                            )
                            is DatabaseBlock -> actions.databaseBlockEditor?.let { databaseBlockEditor ->
                                DatabaseBlockView(
                                    block = block,
                                    editor = databaseBlockEditor,
                                    inSelectionMode = inSelectionMode,
                                    onToggleSelection = { actions.onToggleSelection(block.id) },
                                    onOpenRow = { actions.onNoteLinkClick(it) },
                                    onDelete = { actions.onDeleteBlock(block.id) },
                                    runAfterKeyboardCloses = afterKeyboardCloses
                                )
                            }
                            is VoiceBlock -> AudioBlockView(
                                block = block,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = { actions.onToggleSelection(block.id) },
                                onRemoveVoice = { actions.onRemoveVoice(block.id) },
                                onStartRecording = { actions.onStartRecording() },
                                onStopRecording = { cancel -> actions.onStopRecording(block.id, cancel) },
                                onPlayAudio = { path, onComplete -> actions.onPlayAudio(path, onComplete) },
                                onStopAudio = { actions.onStopAudio() }
                            )
                            is SolidDividerBlock -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                        .height(1.5.dp)
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
                                )
                            }
                            is ThreeDotDividerBlock -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val dotSize = 6.dp
                                    val dotColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                    Spacer(Modifier.width(16.dp))
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                    Spacer(Modifier.width(16.dp))
                                    Box(Modifier.size(dotSize).clip(CircleShape).background(dotColor))
                                }
                            }
                            is UnknownBlock -> UnknownBlockView()
                        }
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

        if (isSelected) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 8.dp + desktopExtraPadding)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.15f else 0.1f))
            )
        }

        if (inSelectionMode) {
            val selectedBlocksClickModifier = if (isDesktopPlatform) {
                Modifier
                    .onGloballyPositioned { selectionOverlayTopLeftInWindow = it.positionInWindow() }
                    .pointerInput(block.id) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val pressedKeys = currentEvent.keyboardModifiers
                            val togglesThisBlock = pressedKeys.isCtrlPressed || pressedKeys.isMetaPressed
                            val release = waitForUpOrCancellation() ?: return@awaitEachGesture
                            release.consume()
                            if (togglesThisBlock) {
                                actions.onToggleSelection(block.id)
                            } else {
                                leaveBlockSelectionAt(selectionOverlayTopLeftInWindow + release.position)
                            }
                        }
                    }
            } else {
                Modifier.clickable(onClick = { actions.onToggleSelection(block.id) })
            }
            Box(Modifier.matchParentSize().then(selectedBlocksClickModifier))
        }
    }
}

@Composable
fun IsolatedEditorTextField(
    modifier: Modifier = Modifier,
    initialText: String,
    placeholderText: String = "",
    placeholderColor: Color = Color.Unspecified,
    blockId: String,
    isCodeBlock: Boolean,
    textStyle: TextStyle,
    inSelectionMode: Boolean,
    focusRequester: FocusRequester,
    onUpdateText: (String, String) -> Unit,
    onEnterPressed: (String, String, String) -> Unit,
    onShiftEnterPressed: (String, String, String) -> Unit = onEnterPressed,
    onBackspaceOnEmpty: (String) -> Unit,
    onBackspaceAtStart: (String) -> Unit = {},
    onDeleteAtEnd: (String) -> Unit = {},
    backspaceLeavesTopOfNote: (String) -> Boolean = { false },
    breaksLineOnShiftEnter: Boolean = false,
    onIndentRequested: (increase: Boolean) -> Unit = {},
    onMarkdownShortcut: (blockId: String, literalText: String, shortcut: MarkdownShortcut) -> Boolean = { _, _, _ -> false },
    onPasteMarkdown: (blockId: String, pastedText: PastedText) -> Boolean = { _, _ -> false },
    onCopyAsMarkdown: ((start: Int, end: Int) -> Unit)? = null,
    markdownClipboardActions: MarkdownClipboardActions? = null,
    onTextLayout: (TextLayoutResult) -> Unit,
    allLinkableNotes: List<NoteMetadataEntity>,
    onCreateLinkedNote: (String) -> String,
    onOpenNote: (String) -> Unit,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    selectionRequest: SelectionRequest? = null,
    focusRequest: FocusRequest? = null,
    displayedCursorOffsetFromTap: Int? = null,
    onCursorFromTapPlaced: () -> Unit = {},
    isSlashMenuOpen: Boolean = false,
    onSlashMenuNavigate: (delta: Int) -> Unit = {},
    onSlashMenuConfirm: () -> Unit = {},
    onSlashMenuDismiss: () -> Unit = {}
) {
    var tfv by remember { mutableStateOf(TextFieldValue(initialText, TextRange.Zero)) }
    var lastSentText by remember { mutableStateOf(initialText) }
    val textsSentButNotYetEchoed = remember { mutableListOf<String>() }
    var keyboardTextHandedToEditor by remember { mutableStateOf<String?>(null) }

    var mentionQuery by remember { mutableStateOf<String?>(null) }
    var mentionStartIndex by remember { mutableIntStateOf(-1) }

    var localTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFieldFocused by remember { mutableStateOf(false) }
    var isFocusRequestedByTap by remember { mutableStateOf(false) }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)

    var mentionSelectedIndex by remember(mentionQuery) { mutableIntStateOf(0) }

    val crossBlockSelection = LocalCrossBlockSelection.current
    var fieldCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestFieldText by rememberUpdatedState(tfv.text)
    val latestVisualTransformation by rememberUpdatedState(visualTransformation)
    val selectableTextField = remember {
        object : SelectableTextField {
            private fun offsetMapping() = latestVisualTransformation.filter(AnnotatedString(latestFieldText)).offsetMapping
            private fun attachedCoordinates() = fieldCoordinates?.takeIf { it.isAttached }
            private fun displayedOffsetOf(storedOffset: Int, layout: TextLayoutResult) =
                offsetMapping().originalToTransformed(storedOffset.coerceIn(0, latestFieldText.length)).coerceIn(0, layout.layoutInput.text.length)

            override fun storedOffsetAt(pointInWindow: Offset): Int? {
                val coordinates = attachedCoordinates() ?: return null
                val layout = localTextLayoutResult ?: return null
                val displayedOffset = layout.getOffsetForPosition(coordinates.windowToLocal(pointInWindow))
                return offsetMapping().transformedToOriginal(displayedOffset)
            }

            override fun storedOffsetBeside(storedOffset: Int, forward: Boolean): Int? =
                storedOffsetBesideSkippingNoteLinks(latestFieldText, storedOffset, forward)

            override fun storedOffsetOnNextLine(storedOffset: Int, downward: Boolean, windowX: Float?): Int? {
                val layout = localTextLayoutResult ?: return null
                val displayedOffset = displayedOffsetOf(storedOffset, layout)
                val targetLine = layout.getLineForOffset(displayedOffset) + if (downward) 1 else -1
                if (targetLine !in 0 until layout.lineCount) return null
                val goalXInField = windowX?.let { x -> attachedCoordinates()?.windowToLocal(Offset(x, 0f))?.x }
                val pointOnTargetLine = Offset(
                    goalXInField ?: layout.getHorizontalPosition(displayedOffset, usePrimaryDirection = true),
                    (layout.getLineTop(targetLine) + layout.getLineBottom(targetLine)) / 2
                )
                return offsetMapping().transformedToOriginal(layout.getOffsetForPosition(pointOnTargetLine))
            }

            override fun storedOffsetOnOuterLine(onFirstLine: Boolean, windowX: Float?): Int? {
                if (windowX == null) return if (onFirstLine) 0 else latestFieldText.length
                val coordinates = attachedCoordinates() ?: return null
                val layout = localTextLayoutResult ?: return null
                val line = if (onFirstLine) 0 else layout.lineCount - 1
                val pointOnLine = Offset(
                    coordinates.windowToLocal(Offset(windowX, 0f)).x,
                    (layout.getLineTop(line) + layout.getLineBottom(line)) / 2
                )
                return offsetMapping().transformedToOriginal(layout.getOffsetForPosition(pointOnLine))
            }

            override fun cursorRectInWindow(storedOffset: Int): Rect? {
                val coordinates = attachedCoordinates() ?: return null
                val layout = localTextLayoutResult ?: return null
                val cursorRect = layout.getCursorRect(displayedOffsetOf(storedOffset, layout))
                return Rect(coordinates.localToWindow(cursorRect.topLeft), cursorRect.size)
            }

            override fun selectionIfFocused(): TextRange? = if (isFieldFocused) tfv.selection else null

            override fun collapseSelectionIfFocused() {
                if (!isFieldFocused || tfv.selection.collapsed) return
                tfv = tfv.copy(selection = TextRange(tfv.selection.max), composition = null)
                GlobalEditorState.currentSelection = tfv.selection
            }
        }
    }
    DisposableEffect(crossBlockSelection, blockId) {
        crossBlockSelection?.registerTextField(blockId, selectableTextField)
        onDispose { crossBlockSelection?.forgetTextField(blockId, selectableTextField) }
    }

    fun cursorRectAt(storedOffset: Int, layout: TextLayoutResult): Rect {
        val displayedOffset = latestVisualTransformation.filter(AnnotatedString(tfv.text)).offsetMapping
            .originalToTransformed(storedOffset.coerceIn(0, tfv.text.length))
            .coerceIn(0, layout.layoutInput.text.length)
        return layout.getCursorRect(displayedOffset)
    }

    suspend fun revealCursor(storedOffset: Int) {
        val layout = localTextLayoutResult ?: return
        runCatching { bringIntoViewRequester.bringIntoView(cursorRectAt(storedOffset, layout)) }
    }

    val cursorRectWhileFocused: () -> Rect? = remember {
        {
            val layout = localTextLayoutResult
            if (isFieldFocused && layout != null && tfv.selection.collapsed) cursorRectAt(tfv.selection.end, layout) else null
        }
    }

    val caretRevealScope = rememberCoroutineScope()
    fun placeCaretInsideField(storedOffset: Int) {
        tfv = tfv.copy(selection = TextRange(storedOffset), composition = null)
        GlobalEditorState.currentSelection = tfv.selection
        caretRevealScope.launch { revealCursor(storedOffset) }
    }

    fun runBlockBoundaryEdit(edit: () -> Unit) {
        val lastEdit = GlobalEditorState.lastBlockBoundaryEdit
        if (lastEdit != null && lastEdit.elapsedNow() < BlockBoundaryEditGap) return
        GlobalEditorState.lastBlockBoundaryEdit = TimeSource.Monotonic.markNow()
        edit()
    }

    fun insertLineBreakAtCursor() {
        val selectionStart = tfv.selection.min
        val newText = tfv.text.substring(0, selectionStart) + "\n" + tfv.text.substring(tfv.selection.max)
        tfv = tfv.copy(text = newText, selection = TextRange(selectionStart + 1), composition = null)
        GlobalEditorState.currentSelection = tfv.selection
        lastSentText = newText
        textsSentButNotYetEchoed += newText
        onUpdateText(blockId, newText)
    }

    fun isSelectionFocusAtEdge(step: SelectionStep): Boolean {
        val focusOffset = tfv.selection.end
        val layout = localTextLayoutResult ?: return false
        val displayedFocus = latestVisualTransformation.filter(AnnotatedString(tfv.text)).offsetMapping
            .originalToTransformed(focusOffset)
            .coerceIn(0, layout.layoutInput.text.length)
        return when (step) {
            SelectionStep.CharacterBack -> focusOffset == 0
            SelectionStep.CharacterForward -> focusOffset == tfv.text.length
            SelectionStep.LineUp -> layout.getLineForOffset(displayedFocus) == 0
            SelectionStep.LineDown -> layout.getLineForOffset(displayedFocus) == layout.lineCount - 1
        }
    }
    val crossBlockSelectedRange by remember(crossBlockSelection, blockId) {
        derivedStateOf { crossBlockSelection?.selectedTextRangeIn(blockId, tfv.text.length) }
    }
    val showsEmptyLineSelected by remember(crossBlockSelection, blockId) {
        derivedStateOf { tfv.text.isEmpty() && crossBlockSelection?.continuesPastBlock(blockId) == true }
    }
    val emptyLineSelectionWidth = with(LocalDensity.current) { EmptyLineSelectionWidth.toPx() }
    val displayedCrossBlockSelectedRange = remember(crossBlockSelectedRange, tfv.text, visualTransformation) {
        crossBlockSelectedRange?.let { range ->
            val offsetMapping = visualTransformation.filter(AnnotatedString(tfv.text)).offsetMapping
            TextRange(offsetMapping.originalToTransformed(range.start), offsetMapping.originalToTransformed(range.end))
        }
    }
    val crossBlockSelectionColor = LocalTextSelectionColors.current.backgroundColor
    val isTextSelectedAcrossBlocks by remember(crossBlockSelection) {
        derivedStateOf { crossBlockSelection?.isActive == true }
    }
    var hidesTextMenuUntilTouched by remember { mutableStateOf(false) }
    LaunchedEffect(isTextSelectedAcrossBlocks) {
        if (isTextSelectedAcrossBlocks) hidesTextMenuUntilTouched = true
    }

    fun copySelectionAsMarkdown(): Boolean {
        val copySelectedText = onCopyAsMarkdown ?: return false
        if (tfv.selection.collapsed) return false
        copySelectedText(tfv.selection.min, tfv.selection.max)
        return true
    }

    fun deleteSelection(): Boolean {
        if (tfv.selection.collapsed) return false
        val selectionStart = tfv.selection.min
        val newText = tfv.text.removeRange(selectionStart, tfv.selection.max)
        tfv = tfv.copy(text = newText, selection = TextRange(selectionStart), composition = null)
        lastSentText = newText
        textsSentButNotYetEchoed += newText
        onUpdateText(blockId, newText)
        return true
    }

    fun cutSelectionAsMarkdown(): Boolean = copySelectionAsMarkdown() && deleteSelection()

    SideEffect {
        markdownClipboardActions?.copySelection = { copySelectionAsMarkdown() }
        markdownClipboardActions?.cutSelection = { cutSelectionAsMarkdown() }
        markdownClipboardActions?.deleteSelection = { deleteSelection() }
    }

    LaunchedEffect(isFieldFocused, imeBottom, tfv.selection.end) {
        if (!isFieldFocused || imeBottom <= 0) return@LaunchedEffect
        delay(50.milliseconds)
        val layout = localTextLayoutResult ?: return@LaunchedEffect
        val cursorRect = runCatching {
            layout.getCursorRect(tfv.selection.end.coerceIn(0, layout.layoutInput.text.length))
        }.getOrNull() ?: return@LaunchedEffect
        runCatching { bringIntoViewRequester.bringIntoView(cursorRect) }
    }

    LaunchedEffect(initialText) {
        if (tfv.text == initialText) {
            lastSentText = initialText
            textsSentButNotYetEchoed.clear()
            return@LaunchedEffect
        }
        if (initialText in textsSentButNotYetEchoed) return@LaunchedEffect
        textsSentButNotYetEchoed.clear()
        val wasAtEnd = tfv.selection.start == tfv.text.length
        val safeStart = if (wasAtEnd) initialText.length else tfv.selection.start.coerceAtMost(initialText.length)
        val safeEnd = if (wasAtEnd) initialText.length else tfv.selection.end.coerceAtMost(initialText.length)
        tfv = tfv.copy(text = initialText, selection = TextRange(safeStart, safeEnd), composition = null)
        lastSentText = initialText
    }

    // Toolbar "@" press. Mirrors exactly what typing '@' does in onValueChange: insert the
    // character at the cursor, record where the mention starts, open the picker with an empty query.
    LaunchedEffect(EditorEventBus.insertMentionSignal) {
        val (targetId, _) = EditorEventBus.insertMentionSignal ?: return@LaunchedEffect
        if (targetId != blockId || !isFieldFocused) return@LaunchedEffect

        val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
        // onValueChange only treats '@' as a mention at index 0 or after whitespace,
        // so pad it here or the picker would open and immediately fail to re-match on the
        // next keystroke.
        val needsSpace = cursor > 0 && !tfv.text[cursor - 1].isWhitespace()
        val insert = if (needsSpace) " @" else "@"

        val newText = tfv.text.substring(0, cursor) + insert + tfv.text.substring(cursor)
        val newCursor = cursor + insert.length

        tfv = tfv.copy(text = newText, selection = TextRange(newCursor), composition = null)
        lastSentText = newText          // keeps LaunchedEffect(initialText) from clobbering us
        textsSentButNotYetEchoed += newText
        mentionStartIndex = newCursor - 1
        mentionQuery = ""
        onUpdateText(blockId, newText)
    }

    LaunchedEffect(EditorEventBus.insertTextSignal) {
        val request = EditorEventBus.insertTextSignal ?: return@LaunchedEffect
        if (request.blockId != blockId || !isFieldFocused) return@LaunchedEffect

        val selectionStart = tfv.selection.min
        val selectionEnd = tfv.selection.max
        val spaceBefore = if (selectionStart > 0 && !tfv.text[selectionStart - 1].isWhitespace()) " " else ""
        val spaceAfter = if (selectionEnd < tfv.text.length && tfv.text[selectionEnd].isWhitespace()) "" else " "
        val insertedText = spaceBefore + request.text + spaceAfter
        val newText = tfv.text.substring(0, selectionStart) + insertedText + tfv.text.substring(selectionEnd)

        tfv = tfv.copy(text = newText, selection = TextRange(selectionStart + insertedText.length), composition = null)
        GlobalEditorState.currentSelection = tfv.selection
        lastSentText = newText
        textsSentButNotYetEchoed += newText
        onUpdateText(blockId, newText)
    }

// Updates cursor placement when a FocusRequest targets this block with placeCursorAtEnd = true.
    LaunchedEffect(focusRequest?.nonce) {
        if (focusRequest?.id == blockId && focusRequest.placeCursorAtEnd) {
            tfv = tfv.copy(selection = TextRange(tfv.text.length), composition = null)
            withFrameNanos {}
            if (isFieldFocused) revealCursor(tfv.text.length)
        }
    }

    LaunchedEffect(displayedCursorOffsetFromTap) {
        val displayedOffset = displayedCursorOffsetFromTap ?: return@LaunchedEffect
        val cursorOffset = visualTransformation.filter(AnnotatedString(tfv.text)).offsetMapping.transformedToOriginal(displayedOffset)
        tfv = tfv.copy(selection = TextRange(cursorOffset), composition = null)
        isFocusRequestedByTap = true
        onCursorFromTapPlaced()
    }

    LaunchedEffect(isFocusRequestedByTap) {
        if (!isFocusRequestedByTap) return@LaunchedEffect
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
        isFocusRequestedByTap = false
    }

    LaunchedEffect(selectionRequest?.nonce) {
        if (selectionRequest?.blockId == blockId) {
            val selection = selectionRequest.selection
            tfv = tfv.copy(
                text = initialText,
                selection = TextRange(
                    selection.start.coerceIn(0, initialText.length),
                    selection.end.coerceIn(0, initialText.length)
                ),
                composition = null
            )
            lastSentText = initialText
            withFrameNanos {}
            if (isFieldFocused) revealCursor(tfv.selection.end)
        }
    }

    fun insertNoteLink(safeTitle: String, noteId: String) {
        val lastAt = mentionStartIndex
        if (lastAt != -1) {
            val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
            val markdownLink = "[$safeTitle](emberr://note/$noteId) "

            val textBefore = tfv.text.substring(0, lastAt)
            val textAfter = tfv.text.substring(cursor)

            val newText = textBefore + markdownLink + textAfter
            val newCursor = textBefore.length + markdownLink.length

            tfv = tfv.copy(text = newText, selection = TextRange(newCursor), composition = null)
            onUpdateText(blockId, newText)
        }
        mentionQuery = null
        mentionStartIndex = -1
    }

    LaunchedEffect(blockId) {
        EditorEventBus.confirmMentionEvent.collect { (safeTitle, noteId) ->
            if (mentionQuery == null || !isFieldFocused) return@collect
            insertNoteLink(safeTitle, noteId)
        }
    }

    LaunchedEffect(blockId) {
        EditorEventBus.confirmMentionAndOpenEvent.collect { (safeTitle, noteId) ->
            if (mentionQuery == null || !isFieldFocused) return@collect
            insertNoteLink(safeTitle, noteId)
            onOpenNote(noteId)
        }
    }

    LaunchedEffect(blockId) {
        EditorEventBus.cancelMentionEvent.collect {
            if (mentionQuery == null || !isFieldFocused) return@collect
            mentionQuery = null
            mentionStartIndex = -1
        }
    }

    val filteredMentionNotes = remember(mentionQuery, allLinkableNotes) {
        val query = mentionQuery.orEmpty()
        allLinkableNotes.filter { it.title.contains(query, ignoreCase = true) }
    }

    val mentionMenuEntries = remember(mentionQuery, filteredMentionNotes) {
        val query = mentionQuery.orEmpty()
        buildList {
            add(
                SlashMenuItemData("Create new note", SlashMenuIcon.Vector(Icons.AutoMirrored.Filled.NoteAdd)) {
                    val newNoteId = onCreateLinkedNote("Untitled")
                    insertNoteLink("Untitled", newNoteId)
                    onOpenNote(newNoteId)
                }
            )
            filteredMentionNotes.forEach { note ->
                val icon = note.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Vector(Icons.Default.Description)
                add(
                    SlashMenuItemData(note.title.ifEmpty { "Untitled" }, icon) {
                        val safeTitle = note.title.replace("[", "").replace("]", "").ifEmpty { "Untitled" }
                        insertNoteLink(safeTitle, note.noteId)
                    }
                )
            }
            if (query.isNotBlank()) {
                add(
                    SlashMenuItemData("New \"$query\" note", SlashMenuIcon.Vector(Icons.Default.Add)) {
                        val safeTitle = query.replace("[", "").replace("]", "").trim().ifEmpty { "Untitled" }
                        val newNoteId = onCreateLinkedNote(safeTitle)
                        insertNoteLink(safeTitle, newNoteId)
                    }
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        if (tfv.text.isEmpty() && placeholderText.isNotEmpty()) {
            Text(
                text = placeholderText,
                style = textStyle.copy(
                    color = placeholderColor.takeOrElse { MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f) }
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        BasicTextField(
            value = tfv,
            visualTransformation = visualTransformation,
            onTextLayout = { result ->
                localTextLayoutResult = result
                onTextLayout(result)
            },
            onValueChange = { newValue ->
                if (newValue.text == keyboardTextHandedToEditor) return@BasicTextField
                keyboardTextHandedToEditor = null
                if (isTextSelectedAcrossBlocks) {
                    if (newValue.text == tfv.text) {
                        tfv = newValue
                    } else {
                        keyboardTextHandedToEditor = newValue.text
                        crossBlockSelection?.host?.replaceSelectionWithTypedText(textInsertedBetween(tfv.text, newValue.text))
                    }
                    return@BasicTextField
                }
                val isSelectingByTouch = crossBlockSelection?.isBeingTouched == true && !newValue.selection.collapsed
                GlobalEditorState.currentSelection = if (isSelectingByTouch) TextRange(newValue.selection.end) else newValue.selection
                val newText = newValue.text
                val cursor = newValue.selection.start

                if (cursor > 0 && cursor <= newText.length) {
                    val textUpToCursor = newText.substring(0, cursor)
                    val lastAt = textUpToCursor.lastIndexOf('@')
                    val isValidAt = lastAt != -1 && (lastAt == 0 || textUpToCursor[lastAt - 1] == ' ' || textUpToCursor[lastAt - 1] == '\n')

                    if (isValidAt && !textUpToCursor.substring(lastAt).contains(" ")) {
                        mentionQuery = textUpToCursor.substring(lastAt + 1)
                        mentionStartIndex = lastAt
                    } else {
                        mentionQuery = null
                        mentionStartIndex = -1
                    }
                } else {
                    mentionQuery = null
                    mentionStartIndex = -1
                }

                val pastedText = if (!isCodeBlock) pastedTextToReadAsMarkdown(tfv.text, newText, GlobalEditorState.lastCopiedMarkdown) else null
                val insertedText = textInsertedBetween(tfv.text, newText)
                val isPlainTyping = !isCodeBlock && newValue.composition == null && newValue.selection.collapsed
                val markdownShortcut = if (isPlainTyping) markdownShortcutAfterTyping(tfv.text, newText, newValue.selection.start) else null
                if (pastedText != null && onPasteMarkdown(blockId, pastedText)) {
                    keyboardTextHandedToEditor = newText
                    mentionQuery = null
                    mentionStartIndex = -1
                } else if (markdownShortcut != null && onMarkdownShortcut(blockId, newText, markdownShortcut)) {
                    tfv = TextFieldValue(markdownShortcut.convertedText, TextRange(markdownShortcut.cursorAfter))
                    lastSentText = markdownShortcut.convertedText
                    textsSentButNotYetEchoed += newText
                    textsSentButNotYetEchoed += markdownShortcut.convertedText
                    mentionQuery = null
                    mentionStartIndex = -1
                } else if (!isCodeBlock && insertedText.contains('\n')) {
                    val splitIndex = tfv.text.commonPrefixWith(newText).length + insertedText.indexOf('\n')
                    tfv = tfv.copy(composition = null)
                    onEnterPressed(blockId, newText.substring(0, splitIndex), newText.substring(splitIndex + 1))
                } else {
                    tfv = newValue
                    lastSentText = newText
                    textsSentButNotYetEchoed += newText
                    onUpdateText(blockId, newText)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .showCursorLineInsteadOfOtherLines(cursorRectWhileFocused)
                .onGloballyPositioned { fieldCoordinates = it }
                .then(
                    if (isDesktopPlatform) {
                        Modifier
                    } else {
                        Modifier
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                    hidesTextMenuUntilTouched = false
                                }
                            }
                            .filterTextContextMenuComponents {
                                !isTextSelectedAcrossBlocks && !hidesTextMenuUntilTouched && tfv.selection.collapsed
                            }
                    }
                )
                .drawBehind {
                    val layout = localTextLayoutResult ?: return@drawBehind
                    if (showsEmptyLineSelected) {
                        val cursorRect = layout.getCursorRect(0)
                        drawRect(
                            color = crossBlockSelectionColor,
                            topLeft = Offset(cursorRect.left, layout.getLineTop(0)),
                            size = Size(emptyLineSelectionWidth, layout.getLineBottom(0) - layout.getLineTop(0))
                        )
                        return@drawBehind
                    }
                    val range = displayedCrossBlockSelectedRange ?: return@drawBehind
                    val textLength = layout.layoutInput.text.length
                    val start = range.start.coerceIn(0, textLength)
                    val end = range.end.coerceIn(start, textLength)
                    if (start < end) drawPath(layout.getPathForRange(start, end), crossBlockSelectionColor)
                }
                .focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusChanged {
                    isFieldFocused = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    val isBackspace = event.key == Key.Backspace
                    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter

                    val selectsWholeNote = crossBlockSelection != null &&
                        event.type == KeyEventType.KeyDown &&
                        (event.isCtrlPressed || event.isMetaPressed) &&
                        event.key == Key.A &&
                        tfv.selection.min == 0 &&
                        tfv.selection.max == tfv.text.length
                    if (selectsWholeNote && crossBlockSelection?.selectWholeNote() == true) return@onPreviewKeyEvent true

                    val extendsSelectionPastBlock = crossBlockSelection != null &&
                        event.type == KeyEventType.KeyDown &&
                        event.isShiftPressed &&
                        !event.isCtrlPressed
                    if (extendsSelectionPastBlock) {
                        val step = selectionStepFor(event.key)
                        val startedSelectingAcrossBlocks = step != null &&
                            isSelectionFocusAtEdge(step) &&
                            crossBlockSelection?.startFromTextField(blockId, tfv.selection, step) == true
                        if (startedSelectingAcrossBlocks) return@onPreviewKeyEvent true
                    }

                    if (isSlashMenuOpen && event.type == KeyEventType.KeyDown) {
                        when (event.key) {
                            Key.DirectionDown -> {
                                onSlashMenuNavigate(1)
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionUp -> {
                                onSlashMenuNavigate(-1)
                                return@onPreviewKeyEvent true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                onSlashMenuConfirm()
                                return@onPreviewKeyEvent true
                            }
                            Key.Escape -> {
                                onSlashMenuDismiss()
                                return@onPreviewKeyEvent true
                            }
                            else -> {}
                        }
                    }

                    if (mentionQuery != null && event.type == KeyEventType.KeyDown) {
                        val entryCount = mentionMenuEntries.size
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (entryCount > 0) mentionSelectedIndex = (mentionSelectedIndex + 1) % entryCount
                                return@onPreviewKeyEvent true
                            }
                            Key.DirectionUp -> {
                                if (entryCount > 0) mentionSelectedIndex = (mentionSelectedIndex - 1 + entryCount) % entryCount
                                return@onPreviewKeyEvent true
                            }
                            Key.Enter, Key.NumPadEnter -> {
                                mentionMenuEntries.getOrNull(mentionSelectedIndex)?.action?.invoke()
                                return@onPreviewKeyEvent true
                            }
                            Key.Escape -> {
                                mentionQuery = null
                                mentionStartIndex = -1
                                return@onPreviewKeyEvent true
                            }
                            else -> {}
                        }
                    }

                    val isPlainKeyDown = event.type == KeyEventType.KeyDown &&
                        !event.isShiftPressed &&
                        !event.isCtrlPressed &&
                        !event.isMetaPressed
                    val caretStep = when (event.key) {
                        Key.DirectionUp -> SelectionStep.LineUp
                        Key.DirectionDown -> SelectionStep.LineDown
                        Key.DirectionLeft -> SelectionStep.CharacterBack.takeIf { isSelectionFocusAtEdge(it) }
                        Key.DirectionRight -> SelectionStep.CharacterForward.takeIf { isSelectionFocusAtEdge(it) }
                        else -> null
                    }
                    val movedCaret = isPlainKeyDown &&
                        caretStep != null &&
                        tfv.selection.collapsed &&
                        crossBlockSelection?.moveCaret(
                            from = BlockTextPosition(blockId, tfv.selection.end),
                            step = caretStep,
                            selectsBlocksWithoutText = isDesktopPlatform
                        ) { offset -> placeCaretInsideField(offset) } == true
                    if (movedCaret) return@onPreviewKeyEvent true

                    val movesCaretByPage = isDesktopPlatform &&
                        isPlainKeyDown &&
                        tfv.selection.collapsed &&
                        (event.key == Key.PageUp || event.key == Key.PageDown)
                    val movedCaretByPage = movesCaretByPage &&
                        crossBlockSelection?.moveCaretByPage(BlockTextPosition(blockId, tfv.selection.end), downward = event.key == Key.PageDown) == true
                    if (movedCaretByPage) return@onPreviewKeyEvent true

                    val isNoteEdgeKey = event.type == KeyEventType.KeyDown &&
                        event.isCtrlPressed &&
                        (event.key == Key.MoveHome || event.key == Key.MoveEnd)
                    if (isNoteEdgeKey) {
                        val toEnd = event.key == Key.MoveEnd
                        val handled = when {
                            !event.isShiftPressed -> crossBlockSelection?.moveCaretToEdgeOfNote(toEnd) == true
                            isDesktopPlatform -> crossBlockSelection?.selectFromTextFieldToEdgeOfNote(blockId, tfv.selection, toEnd) == true
                            else -> false
                        }
                        if (handled) return@onPreviewKeyEvent true
                    }

                    val isCopyShortcut = event.type == KeyEventType.KeyDown &&
                        event.isCtrlPressed &&
                        (event.key == Key.C || event.key == Key.X)
                    if (isCopyShortcut) {
                        val copiedAsMarkdown = if (event.key == Key.X) cutSelectionAsMarkdown() else copySelectionAsMarkdown()
                        if (copiedAsMarkdown) return@onPreviewKeyEvent true
                    }

                    val deletesAtEnd = event.key == Key.Delete &&
                        event.type == KeyEventType.KeyDown &&
                        tfv.selection.collapsed &&
                        tfv.selection.start == tfv.text.length
                    if (deletesAtEnd) {
                        runBlockBoundaryEdit { onDeleteAtEnd(blockId) }
                        return@onPreviewKeyEvent true
                    }

                    val backspacesAtStart = isBackspace &&
                        event.type == KeyEventType.KeyDown &&
                        tfv.selection.collapsed &&
                        tfv.selection.start == 0
                    if (backspacesAtStart) {
                        runBlockBoundaryEdit {
                            val movedToTitle = backspaceLeavesTopOfNote(blockId) && crossBlockSelection?.moveCaretAboveBlocks() == true
                            when {
                                movedToTitle -> Unit
                                tfv.text.isEmpty() -> onBackspaceOnEmpty(blockId)
                                else -> onBackspaceAtStart(blockId)
                            }
                        }
                        return@onPreviewKeyEvent true
                    }

                    if (isBackspace && event.type == KeyEventType.KeyDown) {
                        val cursor = tfv.selection.start
                        if (cursor > 0 && tfv.selection.collapsed) {
                            val textBeforeCursor = tfv.text.substring(0, cursor)
                            val match = TrailingNoteLinkRegex.find(textBeforeCursor)

                            if (match != null) {
                                val textBeforeLink = textBeforeCursor.substring(0, match.range.first)
                                val textAfterCursor = tfv.text.substring(cursor)
                                val newText = textBeforeLink + textAfterCursor

                                tfv = tfv.copy(text = newText, selection = TextRange(textBeforeLink.length))
                                onUpdateText(blockId, newText)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    val insertsLineBreak = isEnter && event.isShiftPressed && breaksLineOnShiftEnter && tfv.text.isNotEmpty()
                    if (insertsLineBreak) {
                        if (event.type == KeyEventType.KeyDown) insertLineBreakAtCursor()
                        return@onPreviewKeyEvent true
                    }
                    if (isEnter && (!isCodeBlock || event.isShiftPressed)) {
                        if (event.type == KeyEventType.KeyDown) {
                            val textBefore = tfv.text.substring(0, tfv.selection.min)
                            val textAfter = tfv.text.substring(tfv.selection.max)
                            if (event.isShiftPressed) {
                                onShiftEnterPressed(blockId, textBefore, textAfter)
                            } else {
                                onEnterPressed(blockId, textBefore, textAfter)
                            }
                        }
                        return@onPreviewKeyEvent true
                    }
                    if (event.key == Key.Tab && !isCodeBlock) {
                        if (event.type == KeyEventType.KeyDown) onIndentRequested(!event.isShiftPressed)
                        return@onPreviewKeyEvent true
                    }
                    false
                },
            textStyle = textStyle,
            cursorBrush = SolidColor(if (isTextSelectedAcrossBlocks) Color.Transparent else MaterialTheme.colorScheme.primary),
            enabled = !inSelectionMode
        )

        if (isDesktopPlatform && mentionQuery != null) {
            val mentionRect = remember(tfv.text, mentionStartIndex, visualTransformation, localTextLayoutResult) {
                displayedCharacterRect(tfv.text, mentionStartIndex, visualTransformation, localTextLayoutResult) ?: Rect.Zero
            }
            Box(
                modifier = Modifier
                    .offset { IntOffset(mentionRect.left.roundToInt(), mentionRect.top.roundToInt()) }
                    .size(width = 1.dp, height = with(density) { mentionRect.height.toDp() })
            ) {
                EmberrDesktopMenu(
                    expanded = true,
                    onDismissRequest = { mentionQuery = null; mentionStartIndex = -1 },
                    properties = PopupProperties(focusable = false),
                    modifier = Modifier
                        .width(290.dp)
                        .heightIn(max = 400.dp)
                ) {
                    SlashMenuList(
                        sections = listOf(SlashMenuSectionData("Link to Note", mentionMenuEntries)),
                        selectedIndex = mentionSelectedIndex
                    )
                }
            }
        }
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

private fun displayedCharacterRect(
    text: String,
    characterIndex: Int,
    visualTransformation: VisualTransformation,
    textLayout: TextLayoutResult?
): Rect? {
    if (characterIndex !in text.indices || textLayout == null) return null
    val displayedCharacterIndex = visualTransformation.filter(AnnotatedString(text)).offsetMapping.originalToTransformed(characterIndex)
    val displayedTextLength = textLayout.layoutInput.text.length
    return textLayout.getCursorRect(displayedCharacterIndex.coerceIn(0, displayedTextLength))
}

// Replaces raw note link syntax (e.g. "[title](emberr://note/id)") with styled display text ("@title")
// and applies inline rich-text styles (bold, italic, strike, underline). Computes offset mappings
// so cursor movements and tap targets accurately correspond to the underlying text.
data class RichTextVisualTransformation(
    private val linkColor: Color,
    private val fadedColor: Color,
    private val isDarkTheme: Boolean,
    private val validNoteIds: Set<String>,
    private val inlineSpans: List<InlineSpan> = emptyList(),
    private val hoveredLink: HoveredLink? = null,
    private val noteTitlesById: Map<String, String> = emptyMap()
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text

        if (inlineSpans.isEmpty() && !originalText.contains(NOTE_LINK_MARKER)) {
            return TransformedText(text.withInteractiveLinksHighlighted(hoveredLink), OffsetMapping.Identity)
        }

        val matches = NoteLinkRegex.findAll(originalText).toList()
        if (matches.isEmpty() && inlineSpans.isEmpty()) {
            return TransformedText(text.withInteractiveLinksHighlighted(hoveredLink), OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder()
        val mapping = IntArray(originalText.length * 2 + 50)
        val inverse = IntArray(originalText.length + 1)

        var originalIndex = 0
        var transformedIndex = 0
        data class PendingLink(val noteId: String, val start: Int, val end: Int, val isMissing: Boolean)
        val pendingLinks = mutableListOf<PendingLink>()

        for (match in matches) {
            while (originalIndex < match.range.first) {
                mapping[transformedIndex] = originalIndex
                inverse[originalIndex] = transformedIndex
                builder.append(originalText[originalIndex])
                originalIndex++
                transformedIndex++
            }

            val noteId = match.groupValues[2]
            val title = noteTitlesById[noteId]?.ifEmpty { "Untitled" } ?: match.groupValues[1]
            val linkText = "@$title"
            val isMissing = !validNoteIds.contains(noteId)

            val linkStartTransformed = transformedIndex
            builder.append(linkText)
            pendingLinks.add(PendingLink(noteId, linkStartTransformed, transformedIndex + linkText.length, isMissing))

            for (i in linkText.indices) {
                mapping[transformedIndex] = match.range.last + 1
                transformedIndex++
            }

            for (i in match.range) {
                inverse[i] = linkStartTransformed
            }
            originalIndex = match.range.last + 1
        }

        while (originalIndex < originalText.length) {
            mapping[transformedIndex] = originalIndex
            inverse[originalIndex] = transformedIndex
            builder.append(originalText[originalIndex])
            originalIndex++
            transformedIndex++
        }

        val finalTransformedLength = transformedIndex
        val finalOriginalLength = originalText.length

        mapping[finalTransformedLength] = finalOriginalLength
        inverse[finalOriginalLength] = finalTransformedLength

        pendingLinks.forEach { link ->
            val finalColor = if (link.isMissing) fadedColor else linkColor
            val decoration = if (link.isMissing) TextDecoration.LineThrough else TextDecoration.None
            builder.addStyle(SpanStyle(color = finalColor, fontWeight = FontWeight.SemiBold, textDecoration = decoration), link.start, link.end)
            builder.addStringAnnotation(tag = NOTE_LINK_TAG, annotation = link.noteId, start = link.start, end = link.end)
        }

        inlineSpans.forEach { span ->
            val start = span.start.coerceIn(0, finalOriginalLength)
            val end = span.end.coerceIn(0, finalOriginalLength)
            if (start >= end) return@forEach
            val transformedStart = inverse[start]
            val transformedEnd = inverse[end]
            if (transformedStart >= transformedEnd) return@forEach
            val decoration = when {
                span.strikeThrough && span.underline -> TextDecoration.LineThrough + TextDecoration.Underline
                span.strikeThrough -> TextDecoration.LineThrough
                span.underline -> TextDecoration.Underline
                else -> null
            }
            builder.addStyle(
                SpanStyle(
                    fontWeight = if (span.bold) FontWeight.Bold else null,
                    fontStyle = if (span.italic) FontStyle.Italic else null,
                    textDecoration = decoration,
                    background = if (span.highlight) {
                        highlightBackgroundFor(span.highlightColorName, isDarkTheme)
                    } else Color.Unspecified
                ),
                transformedStart,
                transformedEnd
            )
        }

        return TransformedText(
            builder.toAnnotatedString().withInteractiveLinksHighlighted(hoveredLink),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset >= finalOriginalLength) return finalTransformedLength
                    return inverse[offset]
                }
                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    if (offset >= finalTransformedLength) return finalOriginalLength
                    return mapping[offset]
                }
            }
        )
    }
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
