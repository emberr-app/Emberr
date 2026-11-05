package com.emberr.presentation.shared.editor

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.CredentialsBlock
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
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.UnknownBlock
import com.emberr.domain.model.VoiceBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.editor.blockViews.callout.accentColor
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
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import com.emberr.presentation.shared.editor.selection.BlockSelectionOverlay
import com.emberr.presentation.shared.editor.blockViews.checkbox.CheckboxReminderPickerDialogs
import com.emberr.presentation.shared.editor.blockViews.checkbox.CheckboxReminderRow
import com.emberr.presentation.shared.editor.blockViews.checkbox.ReminderPickerState
import com.emberr.presentation.shared.editor.slash.DesktopSlashMenuPopups
import com.emberr.presentation.shared.editor.slash.rememberSlashMenuSections
import com.emberr.presentation.shared.editor.text.BlockLeadingMarker
import com.emberr.presentation.shared.editor.text.IsolatedEditorTextField
import com.emberr.presentation.shared.editor.text.RichTextVisualTransformation
import com.emberr.presentation.shared.editor.text.blockContentModifier
import com.emberr.presentation.shared.editor.text.blockEndPadding
import com.emberr.presentation.shared.editor.text.blockStartPadding
import com.emberr.presentation.shared.editor.text.blockTextStyle
import com.emberr.presentation.shared.editor.text.blockVerticalPadding
import com.emberr.presentation.shared.editor.text.displayedCharacterRect

val DefaultBlockShape = RoundedCornerShape(12.dp)

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

    val reminderPickerState = remember { ReminderPickerState() }
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
            reminderPickerState.isReminderPickerOpening = true
            focusManager.clearFocus()
            keyboardController?.hide()
            scope.launch {
                withTimeoutOrNull(600.milliseconds) {
                    snapshotFlow { imeInsets.getBottom(density) }.first { it == 0 }
                }
                delay(50L.milliseconds)
                action()
                reminderPickerState.isReminderPickerOpening = false
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

    val isDarkTheme = LocalAppIsDark.current
    val textStyle = blockTextStyle(block, isDarkTheme)

    val internalVerticalPadding = blockVerticalPadding(block)

    val clipboardManager = LocalClipboardManager.current
    val markdownClipboardActions = remember { MarkdownClipboardActions() }
    val customTextSelectionColors = TextSelectionColors(
        handleColor = MaterialTheme.colorScheme.primary,
        backgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    )

    val desktopExtraPadding = if (isDesktopPlatform) 16.dp else 0.dp
    val startPadding = blockStartPadding(block, displayedIndentationLevel, desktopExtraPadding)
    val endPadding = blockEndPadding(block, desktopExtraPadding)

    val isSlashMenuActiveHere = isDesktopPlatform && isActiveBlock && showSlashMenu
    val isNoteLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showNoteLinkMenu
    val isCanvasLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showCanvasLinkMenu
    val isDatabaseLinkMenuActiveHere = isDesktopPlatform && isActiveBlock && showDatabaseLinkMenu

    var isChoosingHighlightColor by remember { mutableStateOf(false) }

    val slashMenuSections = rememberSlashMenuSections(
        isSlashMenuActiveHere = isSlashMenuActiveHere,
        slashQuery = slashQuery,
        customProperties = customProperties,
        actions = actions,
        onDismissSlashMenu = onDismissSlashMenu,
        onChooseHighlightColor = { isChoosingHighlightColor = true }
    )
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
            BlockLeadingMarker(block = block, textStyle = textStyle, actions = actions, afterKeyboardCloses = afterKeyboardCloses)

            val textFieldWrapperModifier = blockContentModifier(block)
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
                            DesktopSlashMenuPopups(
                                anchorRect = slashRect,
                                isSlashMenuActiveHere = isSlashMenuActiveHere,
                                slashMenuSections = slashMenuSections,
                                slashMenuSelectedIndex = slashMenuSelectedIndex,
                                onDismissSlashMenu = onDismissSlashMenu,
                                isNoteLinkMenuActiveHere = isNoteLinkMenuActiveHere,
                                onDismissNoteLinkMenu = onDismissNoteLinkMenu,
                                isCanvasLinkMenuActiveHere = isCanvasLinkMenuActiveHere,
                                onDismissCanvasLinkMenu = onDismissCanvasLinkMenu,
                                isDatabaseLinkMenuActiveHere = isDatabaseLinkMenuActiveHere,
                                onDismissDatabaseLinkMenu = onDismissDatabaseLinkMenu,
                                onLinkMenuBack = onLinkMenuBack,
                                allLinkableNotes = allLinkableNotes,
                                actions = actions
                            )
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
                        CheckboxReminderRow(
                            block = block,
                            actions = actions,
                            isActiveBlock = isActiveBlock,
                            pickerState = reminderPickerState,
                            isKeyboardOpen = isKeyboardOpen,
                            afterKeyboardCloses = afterKeyboardCloses
                        )
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
            CheckboxReminderPickerDialogs(block = block, actions = actions, pickerState = reminderPickerState)
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
