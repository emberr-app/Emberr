package com.emberr.presentation.shared.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.VoiceBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.editableTextOrNull
import com.emberr.domain.model.idsOfBlocksInsideAToggleOrCallout
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.smoothWheelScroll
import com.emberr.presentation.shared.editor.blockViews.callout.calloutContentPadding
import com.emberr.presentation.shared.editor.blockViews.callout.calloutFramesByRow
import com.emberr.presentation.shared.editor.blockViews.callout.displayedIndentationLevelOf
import com.emberr.presentation.shared.editor.blockViews.callout.drawCalloutFrames
import com.emberr.presentation.shared.editor.markdown.copyEmberrBlocks
import com.emberr.presentation.shared.editor.selection.BlockTextPosition
import com.emberr.presentation.shared.editor.selection.CrossBlockSelectionBar
import com.emberr.presentation.shared.editor.selection.CrossBlockSelectionCommands
import com.emberr.presentation.shared.editor.selection.CrossBlockSelectionHandles
import com.emberr.presentation.shared.editor.selection.CrossBlockSelectionHost
import com.emberr.presentation.shared.editor.selection.CrossBlockSelectionState
import com.emberr.presentation.shared.editor.selection.LocalCrossBlockSelection
import com.emberr.presentation.shared.editor.selection.SelectionStep
import com.emberr.presentation.shared.editor.selection.TextCaret
import com.emberr.presentation.shared.editor.selection.TextSelectionMenu
import com.emberr.presentation.shared.editor.selection.TextSelectionMenuActions
import com.emberr.presentation.shared.editor.selection.blocksCutToRange
import com.emberr.presentation.shared.editor.selection.drawBlockSelectionBox
import com.emberr.presentation.shared.editor.selection.handleCrossBlockSelectionKey
import com.emberr.presentation.shared.editor.selection.selectAcrossBlocksByDragging
import com.emberr.presentation.shared.editor.selection.selectTextAcrossBlocksByTouch
import com.emberr.presentation.shared.editor.selection.selectionStepFor
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.x
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import com.emberr.presentation.shared.editor.slash.buildSlashMenuSections
import com.emberr.presentation.shared.editor.slash.rememberCustomProperties
import com.emberr.presentation.shared.editor.text.ClearanceAwareBringIntoViewSpec
import com.emberr.presentation.shared.editor.toolbar.HeightWithoutKeyboard
import com.emberr.presentation.shared.editor.toolbar.MobileMenuState
import com.emberr.presentation.shared.editor.toolbar.keepsHeightWhileKeyboardShows

private val LinkMenuBlockTypes = setOf("linked_note", "linked_canvas", "linked_database")

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EditorScreen(
    modifier: Modifier = Modifier,
    blocks: List<NoteBlock>,
    actions: EditorActions,
    focusRequest: FocusRequest?,
    selectionRequest: SelectionRequest? = null,
    selectedBlockIds: Set<String>,
    selectionMenuContent: (@Composable (onCloseMenu: () -> Unit) -> Unit)? = null,
    onClearSelection: () -> Unit = {},
    onDeleteSelectedBlocks: () -> Unit = {},
    onSelectBlocks: ((Set<String>) -> Unit)? = null,
    bottomContentPadding: Dp = 0.dp,
    topContentPadding: Dp = 0.dp,
    toolbarOffset: Dp = 0.dp,
    headerContent: (@Composable LazyItemScope.() -> Unit)? = null,
    sectionLabelFor: ((NoteBlock) -> String?)? = null,
    trailingLabelFor: ((NoteBlock) -> String?)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    emptyNotePlaceholder: String = "",
    singleTapStartsTyping: Boolean = false,
    mobileMenuState: MobileMenuState = MobileMenuState.MAIN,
    onMobileMenuStateChange: (MobileMenuState) -> Unit = {},
    slashQuery: String = "",
    onSlashQueryChange: (String) -> Unit = {},
    showNoteLinkMenu: Boolean = false,
    onDismissNoteLinkMenu: () -> Unit = {},
    showCanvasLinkMenu: Boolean = false,
    onDismissCanvasLinkMenu: () -> Unit = {},
    showDatabaseLinkMenu: Boolean = false,
    onDismissDatabaseLinkMenu: () -> Unit = {},
    onMentionQueryChange: (String?) -> Unit = {},
    allLinkableNotes: List<NoteMetadataEntity> = emptyList(),
    isCurrentActivePage: Boolean = true,
    onScrollStateChange: (Boolean) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    topBarClearancePx: Float = 0f,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    contentMaxWidth: Dp = Dp.Unspecified,
    extraBlockSidePadding: Dp = 0.dp
) {
    val isSelectionMode = selectedBlockIds.isNotEmpty()
    val focusManager = LocalFocusManager.current
    val showsSelectionMenuOnRightClick = isDesktopPlatform && selectionMenuContent != null
    var editorTopLeftInWindow by remember { mutableStateOf(Offset.Zero) }
    var selectionAnchorBlockId by remember { mutableStateOf<String?>(null) }
    var selectionMenuPositionInEditor by remember { mutableStateOf<Offset?>(null) }
    val closeSelectionMenu: () -> Unit = remember { { selectionMenuPositionInEditor = null } }
    LaunchedEffect(isSelectionMode) { if (!isSelectionMode) selectionMenuPositionInEditor = null }
    val selectionShortcutFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isSelectionMode, selectionMenuPositionInEditor) {
        val needsKeyFocus = isSelectionMode && selectionMenuPositionInEditor == null
        if (needsKeyFocus) {
            runCatching { selectionShortcutFocusRequester.requestFocus() }
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    var activeBlockId by remember { mutableStateOf<String?>(null) }
    val currentBlocks by rememberUpdatedState(blocks)

    var localFocusRequest by remember { mutableStateOf<FocusRequest?>(null) }
    val activeFocusRequest = focusRequest ?: localFocusRequest

    var focusRequestGeneration by remember { mutableIntStateOf(0) }
    LaunchedEffect(activeFocusRequest?.nonce) {
        if (activeFocusRequest != null) focusRequestGeneration++
    }

    var focusHandoffInFlight by remember { mutableStateOf(false) }
    LaunchedEffect(focusRequestGeneration) {
        if (focusRequestGeneration == 0) return@LaunchedEffect
        focusHandoffInFlight = true
        delay(500.milliseconds)
        focusHandoffInFlight = false
    }

    if (!isDesktopPlatform && isCurrentActivePage) {
        val closeDensity = LocalDensity.current
        val imeBottom = WindowInsets.ime.getBottom(closeDensity)
        val keyboardClosedThresholdPx = remember(closeDensity) { with(closeDensity) { 2.dp.toPx() } }
        val isKeyboardOpen = imeBottom > keyboardClosedThresholdPx
        LaunchedEffect(isKeyboardOpen, focusHandoffInFlight) {
            if (isKeyboardOpen || focusHandoffInFlight) return@LaunchedEffect
            delay(250.milliseconds)
            focusManager.clearFocus(force = true)
            activeBlockId = null
            GlobalEditorState.currentlyFocusedBlockId = null
            localFocusRequest = null
        }
    }

    var showSlashMenu by remember { mutableStateOf(false) }

    var isSlashKilled by remember { mutableStateOf(false) }
    val previousTextMap = remember { mutableMapOf<String, String>() }
    val customProperties = rememberCustomProperties()
    val slashMenuSearchTexts = remember(customProperties) {
        buildSlashMenuSections(
            onChangeBlockType = {},
            onToggleFormat = {},
            onAdjustIndentation = {},
            onSetAlignment = {},
            onInsertMediaBlock = {},
            onInsertText = {},
            usesTwentyFourHourClock = false,
            customProperties = customProperties
        ).flatMap { section -> section.items.flatMap { item -> listOf(item.label) + item.searchWords } }
    }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }
    LaunchedEffect(isScrolled) {
        onScrollStateChange(isScrolled)
    }

    val latestBlocks by rememberUpdatedState(blocks)
    val latestActiveBlockId by rememberUpdatedState(activeBlockId)
    val latestSlashQuery by rememberUpdatedState(slashQuery)
    val latestMobileMenuState by rememberUpdatedState(mobileMenuState)
    val latestOnMobileMenuStateChange by rememberUpdatedState(onMobileMenuStateChange)
    val latestOnSlashQueryChange by rememberUpdatedState(onSlashQueryChange)
    val latestOnMentionQueryChange by rememberUpdatedState(onMentionQueryChange)
    val latestOnUndo by rememberUpdatedState(onUndo)
    val latestOnRedo by rememberUpdatedState(onRedo)
    val latestOnClearSelection by rememberUpdatedState(onClearSelection)
    val latestOnDeleteSelectedBlocks by rememberUpdatedState(onDeleteSelectedBlocks)
    val latestSelectedBlockIds by rememberUpdatedState(selectedBlockIds)
    val latestOnSelectBlocks by rememberUpdatedState(onSelectBlocks)

    val undoOrRedoOnShortcut: (KeyEvent) -> Boolean = remember {
        { keyEvent ->
            val isUsableShortcut = keyEvent.type == KeyEventType.KeyDown &&
                keyEvent.isCtrlPressed &&
                GlobalEditorState.focusedCanvasBlockId == null &&
                GlobalEditorState.openCredentialsFormCount == 0
            when {
                !isUsableShortcut -> false
                keyEvent.key == Key.Z -> {
                    if (keyEvent.isShiftPressed) latestOnRedo() else latestOnUndo()
                    true
                }
                keyEvent.key == Key.Y -> {
                    latestOnRedo()
                    true
                }
                else -> false
            }
        }
    }
    if (isDesktopPlatform) {
        DisposableEffect(undoOrRedoOnShortcut) {
            GlobalEditorState.undoOrRedoShortcutOfLastUsedEditor = undoOrRedoOnShortcut
            onDispose {
                if (GlobalEditorState.undoOrRedoShortcutOfLastUsedEditor === undoOrRedoOnShortcut) {
                    GlobalEditorState.undoOrRedoShortcutOfLastUsedEditor = null
                }
            }
        }
    }

    fun deleteSelectedBlocksOnDeleteKey(keyEvent: KeyEvent): Boolean {
        val isDeleteKey = keyEvent.key == Key.Delete || keyEvent.key == Key.Backspace
        if (!isSelectionMode || !isDeleteKey || keyEvent.type != KeyEventType.KeyDown) return false
        closeSelectionMenu()
        latestOnDeleteSelectedBlocks()
        return true
    }

    val clearSelectionOnTap: () -> Unit = remember {
        { if (isDesktopPlatform) latestOnClearSelection() }
    }

    val clearSlashAndExecute: (() -> Unit) -> Unit = { executionBlock ->
        actions.onClearSlashQuery()
        if (isDesktopPlatform) showSlashMenu = false
        latestOnMobileMenuStateChange(MobileMenuState.MAIN)
        latestOnSlashQueryChange("")
        executionBlock()
    }

    var isDesktopLinkMenuFromSlash by remember { mutableStateOf(false) }
    val reopenSlashMenu: () -> Unit = remember {
        {
            showSlashMenu = true
            activeBlockId?.let { blockId -> localFocusRequest = FocusRequest(id = blockId, placeCursorAtEnd = true) }
        }
    }
    val dismissNoteLinkMenu: () -> Unit = remember(onDismissNoteLinkMenu) {
        {
            isDesktopLinkMenuFromSlash = false
            onDismissNoteLinkMenu()
        }
    }
    val dismissCanvasLinkMenu: () -> Unit = remember(onDismissCanvasLinkMenu) {
        {
            isDesktopLinkMenuFromSlash = false
            onDismissCanvasLinkMenu()
        }
    }
    val dismissDatabaseLinkMenu: () -> Unit = remember(onDismissDatabaseLinkMenu) {
        {
            isDesktopLinkMenuFromSlash = false
            onDismissDatabaseLinkMenu()
        }
    }
    val removeSlashTextBeforeLink: () -> Unit = {
        if (isDesktopLinkMenuFromSlash) {
            isDesktopLinkMenuFromSlash = false
            clearSlashAndExecute {}
        }
    }

    var pendingCaretAfterEdit by remember { mutableStateOf<TextCaret?>(null) }
    var crossBlockMenuPositionInEditor by remember { mutableStateOf<Offset?>(null) }

    val wrappedActions = remember(actions) {
        object : EditorActions by actions {
            override fun onPasteMarkdown(blockId: String, replacedStart: Int, replacedEnd: Int, markdown: String): TextCaret? =
                actions.onPasteMarkdown(blockId, replacedStart, replacedEnd, markdown)?.also { pendingCaretAfterEdit = it }
            override fun onEnterPressed(id: String, before: String, after: String) {
                if (before.isEmpty()) listState.requestScrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
                actions.onEnterPressed(id, before, after)
            }
            override fun onBackspaceAtStartOfText(id: String): TextCaret? =
                actions.onBackspaceAtStartOfText(id)?.also { pendingCaretAfterEdit = it }
            override fun onDeleteAtEndOfText(id: String): TextCaret? =
                actions.onDeleteAtEndOfText(id)?.also { pendingCaretAfterEdit = it }
            override fun onToggleSelection(id: String) {
                selectionAnchorBlockId = id
                actions.onToggleSelection(id)
            }
            override fun onClearFocusRequest() {
                localFocusRequest = null
                actions.onClearFocusRequest()
            }
            override fun onUpdateText(id: String, text: String) {
                actions.onUpdateText(id, text)

                val hasSeenThisBlockBefore = previousTextMap.containsKey(id)
                val prevText = previousTextMap[id] ?: ""
                previousTextMap[id] = text

                val onlySelectionChanged = hasSeenThisBlockBefore && text == prevText
                if (onlySelectionChanged) return

                val slashAdded = text.count { it == '/' } > prevText.count { it == '/' }
                if (slashAdded) {
                    isSlashKilled = false
                }

                val arrivedInOneChunk = hasSeenThisBlockBefore && text.length - prevText.length > 1
                if (arrivedInOneChunk) {
                    isSlashKilled = true
                }

                val lastSlashIndex = text.lastIndexOf('/')
                val isCalloutTitle = findBlockRecursive(latestBlocks, id) is CalloutBlock
                val opensASlashCommand = !isCalloutTitle && lastSlashIndex != -1 && (
                    lastSlashIndex == 0 ||
                        text[lastSlashIndex - 1] == ' ' ||
                        text[lastSlashIndex - 1] == '\n'
                    )
                val textAfterSlash = if (opensASlashCommand) text.substring(lastSlashIndex + 1) else ""

                if (opensASlashCommand) {
                    if (textAfterSlash.contains(" ")) {
                        isSlashKilled = true
                    }

                    if (!isSlashKilled && textAfterSlash.length > 2) {
                        val hasMatch = slashMenuSearchTexts.any { it.contains(textAfterSlash, ignoreCase = true) }
                        if (!hasMatch) {
                            isSlashKilled = true
                        }
                    }
                }

                if (opensASlashCommand && !isSlashKilled) {
                    if (isDesktopPlatform) {
                        showSlashMenu = true
                    } else {
                        latestOnMobileMenuStateChange(MobileMenuState.SLASH)
                    }
                    latestOnSlashQueryChange(textAfterSlash)
                } else {
                    if (isDesktopPlatform) {
                        showSlashMenu = false
                    } else if (latestMobileMenuState == MobileMenuState.SLASH) {
                        latestOnMobileMenuStateChange(MobileMenuState.MAIN)
                    }
                    latestOnSlashQueryChange("")
                }

                val lastAtIndex = text.lastIndexOf('@')
                val opensAMention = lastAtIndex != -1 && (
                    lastAtIndex == 0 || text[lastAtIndex - 1] == ' ' || text[lastAtIndex - 1] == '\n'
                    )
                val textAfterAt = if (opensAMention) text.substring(lastAtIndex + 1) else ""
                val mentionActive = opensAMention && !textAfterAt.contains(' ')

                if (!isDesktopPlatform) {
                    if (mentionActive) {
                        latestOnMobileMenuStateChange(MobileMenuState.MENTION)
                    } else if (latestMobileMenuState == MobileMenuState.MENTION) {
                        latestOnMobileMenuStateChange(MobileMenuState.MAIN)
                    }
                }
                latestOnMentionQueryChange(if (mentionActive) textAfterAt else null)
            }
            override fun onChangeBlockType(type: String) = clearSlashAndExecute { actions.onChangeBlockType(type) }
            override fun onToggleFormat(format: String) = clearSlashAndExecute { actions.onToggleFormat(format) }
            override fun onAdjustIndentation(increase: Boolean) = clearSlashAndExecute { actions.onAdjustIndentation(increase) }
            override fun onSetBlockAlignment(alignment: TextAlignment) = clearSlashAndExecute { actions.onSetBlockAlignment(alignment) }
            override fun onInsertMediaBlock(type: String) {
                if (isDesktopPlatform && type in LinkMenuBlockTypes) {
                    isDesktopLinkMenuFromSlash = true
                    showSlashMenu = false
                    actions.onInsertMediaBlock(type)
                } else {
                    clearSlashAndExecute { actions.onInsertMediaBlock(type) }
                }
            }
            override fun onInsertLinkedNoteBlock(noteId: String) {
                removeSlashTextBeforeLink()
                actions.onInsertLinkedNoteBlock(noteId)
            }
            override fun onInsertCanvasBlock(canvasNoteId: String) {
                removeSlashTextBeforeLink()
                actions.onInsertCanvasBlock(canvasNoteId)
            }
            override fun onInsertLinkedDatabaseBlock(databaseId: String) {
                removeSlashTextBeforeLink()
                actions.onInsertLinkedDatabaseBlock(databaseId)
            }
            override fun onReplaceSlashQuery(replacement: String) {
                if (isDesktopPlatform) showSlashMenu = false
                latestOnMobileMenuStateChange(MobileMenuState.MAIN)
                latestOnSlashQueryChange("")
                actions.onReplaceSlashQuery(replacement)
            }
            override fun onTogglePin() = actions.onTogglePin()
            override fun onNoteLinkClick(noteId: String) = actions.onNoteLinkClick(noteId)
            override fun onCreateLinkedNote(title: String): String = actions.onCreateLinkedNote(title)
        }
    }

    LaunchedEffect(isCurrentActivePage) {
        if (!isCurrentActivePage) return@LaunchedEffect
        launch {
            EditorEventBus.insertSlashEvent.collect {
                val targetId = GlobalEditorState.currentlyFocusedBlockId ?: latestActiveBlockId
                if (targetId != null) {
                    val block = findBlockRecursive(latestBlocks, targetId)
                    val currentText = when (block) {
                        is TextBlock -> block.text
                        is HeadingBlock -> block.text
                        is CheckboxBlock -> block.text
                        is BulletedListBlock -> block.text
                        is NumberedListBlock -> block.text
                        is ToggleBlock -> block.text
                        is QuoteBlock -> block.text
                        else -> null
                    }
                    if (currentText != null) {
                        wrappedActions.onUpdateText(targetId, "$currentText/")
                        localFocusRequest = FocusRequest(id = targetId, placeCursorAtEnd = true)
                    }
                }
            }
        }
        launch {
            EditorEventBus.refocusActiveBlockEvent.collect {
                val targetId = GlobalEditorState.currentlyFocusedBlockId ?: latestActiveBlockId
                if (targetId != null) localFocusRequest = FocusRequest(id = targetId, placeCursorAtEnd = true)
            }
        }
        launch {
            EditorEventBus.cleanupSlashEvent.collect {
                val targetId = GlobalEditorState.currentlyFocusedBlockId ?: latestActiveBlockId
                if (targetId != null) {
                    val block = findBlockRecursive(latestBlocks, targetId)
                    val currentText = when (block) {
                        is TextBlock -> block.text
                        is HeadingBlock -> block.text
                        is CheckboxBlock -> block.text
                        is BulletedListBlock -> block.text
                        is NumberedListBlock -> block.text
                        is ToggleBlock -> block.text
                        is QuoteBlock -> block.text
                        else -> null
                    }
                    if (currentText != null) {
                        val lastSlashIndex = currentText.lastIndexOf('/')
                        if (lastSlashIndex != -1 && lastSlashIndex == currentText.length - 1 - latestSlashQuery.length) {
                            actions.onUpdateText(targetId, currentText.substring(0, lastSlashIndex))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(activeFocusRequest?.nonce) {
        val request = activeFocusRequest ?: return@LaunchedEffect
        withFrameNanos {}
        withFrameNanos {}

        val index = currentBlocks.indexOfFirst { it.id == request.id }
        if (index != -1) {
            val hasHeader = if (headerContent != null) 1 else 0
            val hasStats = if (currentBlocks.any { it is CheckboxBlock }) 1 else 0
            val targetLazyColumnIndex = index + hasHeader + hasStats

            val layoutInfo = listState.layoutInfo
            val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetLazyColumnIndex }

            if (itemInfo == null) {
                try {
                    val viewportHeight = layoutInfo.viewportSize.height
                    val offset = if (viewportHeight > 0) -(viewportHeight / 3) else 0
                    listState.scrollToItem(index = targetLazyColumnIndex, scrollOffset = offset)
                } catch (_: Exception) {}
            } else if (itemInfo.size <= layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset) {
                val itemBottom = itemInfo.offset + itemInfo.size
                val viewportBottom = layoutInfo.viewportEndOffset

                if (itemBottom > viewportBottom) {
                    try {
                        listState.animateScrollBy((itemBottom - viewportBottom).toFloat() + 60f)
                    } catch (_: Exception) {}
                } else if (itemInfo.offset < layoutInfo.viewportStartOffset) {
                    try {
                        listState.animateScrollBy((itemInfo.offset - layoutInfo.viewportStartOffset).toFloat() - 60f)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val density = LocalDensity.current

    val bringIntoViewSpec = remember(topBarClearancePx, density) {
        ClearanceAwareBringIntoViewSpec(
            topInsetPx = topBarClearancePx + with(density) { 16.dp.toPx() },
            bottomInsetPx = with(density) { 64.dp.toPx() },
            maxHeightShownWholePx = with(density) { 64.dp.toPx() }
        )
    }

    val dynamicBottomPadding by animateDpAsState(
        targetValue = if (mobileMenuState != MobileMenuState.MAIN) 280.dp else 100.dp,
        label = "menuPadding"
    )

    LaunchedEffect(mobileMenuState) {
        if (mobileMenuState != MobileMenuState.MAIN) {
            activeBlockId?.let { id ->
                val index = currentBlocks.indexOfFirst { it.id == id }
                if (index != -1) {
                    val hasHeader = if (headerContent != null) 1 else 0
                    val hasStats = if (currentBlocks.any { it is CheckboxBlock }) 1 else 0
                    val targetIndex = index + hasHeader + hasStats

                    withFrameNanos {}

                    val layoutInfo = listState.layoutInfo
                    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == targetIndex }

                    if (itemInfo != null) {
                        val menuHeightPx = with(density) { 340.dp.toPx() }
                        val viewportBottom = layoutInfo.viewportEndOffset
                        val menuTopPx = viewportBottom - menuHeightPx
                        val itemBottomPx = itemInfo.offset + itemInfo.size

                        if (itemBottomPx > menuTopPx) {
                            try {
                                listState.animateScrollBy((itemBottomPx - menuTopPx) + 60f)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }

    val immutableSelectedIds = remember(selectedBlockIds) { selectedBlockIds.toImmutableSet() }
    val validNoteIds = remember(allLinkableNotes) { allLinkableNotes.mapTo(HashSet()) { it.noteId } }

    val onFocusBlock: (String) -> Unit = remember(wrappedActions) {
        { focusedId ->
            activeBlockId = focusedId
            GlobalEditorState.currentlyFocusedBlockId = focusedId
            wrappedActions.onFocusBlock(focusedId)
        }
    }
    val onDismissSlash: () -> Unit = remember { { showSlashMenu = false } }

    val selectBlockRangeTo: (String) -> Unit = remember(actions) {
        { targetBlockId ->
            val visibleBlockIds = latestBlocks.map { it.id }
            val anchorIndex = visibleBlockIds.indexOf(selectionAnchorBlockId)
            val targetIndex = visibleBlockIds.indexOf(targetBlockId)

            if (anchorIndex < 0 || targetIndex < 0) {
                wrappedActions.onToggleSelection(targetBlockId)
            } else {
                val alreadySelectedIds = latestSelectedBlockIds
                for (index in minOf(anchorIndex, targetIndex)..maxOf(anchorIndex, targetIndex)) {
                    val blockIdInRange = visibleBlockIds[index]
                    if (blockIdInRange !in alreadySelectedIds) actions.onToggleSelection(blockIdInRange)
                }
            }
        }
    }

    val openSelectionMenuAt: (Offset) -> Unit = remember {
        { pressPositionInWindow ->
            focusManager.clearFocus()
            keyboardController?.hide()
            activeBlockId = null
            GlobalEditorState.currentlyFocusedBlockId = null
            localFocusRequest = null
            showSlashMenu = false
            selectionMenuPositionInEditor = pressPositionInWindow - editorTopLeftInWindow
        }
    }

    val crossBlockSelection = remember { CrossBlockSelectionState() }
    val blockIdsInOrder = remember(blocks) { blocks.map { it.id } }
    SideEffect { crossBlockSelection.blockIdsInOrder = blockIdsInOrder }
    var localSelectionRequest by remember { mutableStateOf<SelectionRequest?>(null) }
    LaunchedEffect(selectionRequest?.nonce) { localSelectionRequest = null }
    val clipboardManager = LocalClipboardManager.current
    val latestWrappedActions by rememberUpdatedState(wrappedActions)
    val latestAllLinkableNotes by rememberUpdatedState(allLinkableNotes)
    val wholeBlockSelectionTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)

    fun placeCaretInBlock(blockId: String, selection: TextRange) {
        activeBlockId = blockId
        GlobalEditorState.currentlyFocusedBlockId = blockId
        latestWrappedActions.onFocusBlock(blockId)
        localFocusRequest = FocusRequest(id = blockId)
        localSelectionRequest = SelectionRequest(blockId = blockId, selection = selection)
    }

    LaunchedEffect(blocks, pendingCaretAfterEdit) {
        val caret = pendingCaretAfterEdit ?: return@LaunchedEffect
        val caretBlock = blocks.firstOrNull { it.id == caret.position.blockId } ?: return@LaunchedEffect
        if (caretBlock.editableTextOrNull() != caret.blockText) return@LaunchedEffect
        pendingCaretAfterEdit = null
        placeCaretInBlock(caret.position.blockId, TextRange(caret.position.offset))
    }

    val crossBlockCommands = remember {
        object : CrossBlockSelectionCommands {
            override fun copy() {
                val range = crossBlockSelection.range ?: return
                val idsInSelection = crossBlockSelection.blockIdsInSelection().toSet()
                val blocksInSelection = latestWrappedActions.onBlocksToCopy(range, latestBlocks.filter { it.id in idsInSelection })
                clipboardManager.copyEmberrBlocks(blocksCutToRange(blocksInSelection, range), latestAllLinkableNotes)
            }

            override fun cut() {
                copy()
                replaceWith("")
            }

            override fun paste() {
                val pastedText = clipboardManager.getText()?.text?.takeIf { it.isNotEmpty() } ?: return
                pasteText(pastedText)
            }

            override fun pasteText(text: String) {
                val range = crossBlockSelection.range ?: return
                val caret = latestWrappedActions.onPasteOverTextAcrossBlocks(range, crossBlockSelection.blockIdsInSelection(), text) ?: return
                crossBlockSelection.clear()
                pendingCaretAfterEdit = caret
            }

            override fun replaceWith(text: String) {
                val range = crossBlockSelection.range ?: return
                val caret = latestWrappedActions.onReplaceTextAcrossBlocks(range, crossBlockSelection.blockIdsInSelection(), text) ?: return
                crossBlockSelection.clear()
                pendingCaretAfterEdit = caret
            }

            override fun splitAtSelection() {
                val range = crossBlockSelection.range ?: return
                val caret = latestWrappedActions.onSplitAtTextAcrossBlocks(range, crossBlockSelection.blockIdsInSelection()) ?: return
                crossBlockSelection.clear()
                pendingCaretAfterEdit = caret
            }

            override fun adjustIndentation(increase: Boolean) {
                latestWrappedActions.onAdjustIndentationOfBlocks(crossBlockSelection.blockIdsInSelection().toSet(), increase)
            }

            override fun toggleFormat(format: String) {
                val range = crossBlockSelection.range ?: return
                latestWrappedActions.onToggleFormatAcrossBlocks(range, crossBlockSelection.blockIdsInSelection(), format)
            }

            override fun collapseTo(position: BlockTextPosition) {
                crossBlockSelection.clear()
                val block = latestBlocks.firstOrNull { it.id == position.blockId }
                if (block?.editableTextOrNull() != null) placeCaretInBlock(position.blockId, TextRange(position.offset))
            }
        }
    }

    fun moveCaretFromSelectedBlock(keyEvent: KeyEvent): Boolean {
        val selectedBlockId = latestSelectedBlockIds.singleOrNull() ?: return false
        val isPlainKeyDown = keyEvent.type == KeyEventType.KeyDown &&
            !keyEvent.isShiftPressed &&
            !keyEvent.isCtrlPressed &&
            !keyEvent.isMetaPressed
        val step = selectionStepFor(keyEvent.key)?.takeIf { isPlainKeyDown } ?: return false
        return crossBlockSelection.moveCaretFromSelectedBlock(selectedBlockId, step)
    }

    val crossBlockScrollScope = rememberCoroutineScope()
    var hasFocusInsideEditor by remember { mutableStateOf(false) }
    val latestTopBarClearancePx by rememberUpdatedState(topBarClearancePx)
    val crossBlockRevealMarginPx = with(LocalDensity.current) { 48.dp.toPx() }
    DisposableEffect(crossBlockSelection) {
        crossBlockSelection.host = object : CrossBlockSelectionHost {
            override fun onSelectingStarted() {
                if (isDesktopPlatform) {
                    runCatching { selectionShortcutFocusRequester.requestFocus() }
                    activeBlockId = null
                    GlobalEditorState.currentlyFocusedBlockId = null
                    localFocusRequest = null
                } else {
                    crossBlockSelection.collapseFocusedTextSelection()
                    if (!hasFocusInsideEditor) runCatching { selectionShortcutFocusRequester.requestFocus() }
                }
                showSlashMenu = false
            }

            override fun replaceSelectionWithTypedText(typedText: String) {
                when {
                    typedText == "\n" -> crossBlockCommands.splitAtSelection()
                    typedText.contains('\n') -> crossBlockCommands.pasteText(typedText)
                    else -> crossBlockCommands.replaceWith(typedText)
                }
            }

            override fun placeCaret(position: BlockTextPosition) {
                clearBlockSelection()
                placeCaretInBlock(position.blockId, TextRange(position.offset))
            }

            override fun clearBlockSelection() {
                if (latestSelectedBlockIds.isNotEmpty()) latestOnClearSelection()
            }

            override fun selectBlock(blockId: String) {
                latestOnClearSelection()
                activeBlockId = null
                GlobalEditorState.currentlyFocusedBlockId = null
                localFocusRequest = null
                latestWrappedActions.onToggleSelection(blockId)
            }

            override fun textLengthOf(blockId: String): Int? =
                latestBlocks.firstOrNull { it.id == blockId }?.editableTextOrNull()?.length

            override fun revealFocus(step: SelectionStep) {
                val movesDown = step == SelectionStep.LineDown || step == SelectionStep.CharacterForward
                val neededScroll = crossBlockSelection.scrollNeededToShowFocus(latestTopBarClearancePx, crossBlockRevealMarginPx)
                if (neededScroll == null && scrolledToFocusBlockFarAway()) return
                val scrollDistance = neededScroll ?: if (movesDown) crossBlockRevealMarginPx else -crossBlockRevealMarginPx
                if (scrollDistance != 0f) crossBlockScrollScope.launch { listState.scrollBy(scrollDistance) }
            }

            private fun scrolledToFocusBlockFarAway(): Boolean {
                val focusBlockId = crossBlockSelection.focusPosition?.blockId ?: return false
                val blockIndex = latestBlocks.indexOfFirst { it.id == focusBlockId }.takeIf { it != -1 } ?: return false
                val hasHeader = if (headerContent != null) 1 else 0
                val hasStats = if (latestBlocks.any { it is CheckboxBlock }) 1 else 0
                val itemIndex = blockIndex + hasHeader + hasStats
                if (listState.layoutInfo.visibleItemsInfo.any { it.index == itemIndex }) return false
                crossBlockScrollScope.launch { listState.scrollToItem(itemIndex) }
                return true
            }

            override fun scrollByPage(downward: Boolean, afterScroll: (didScroll: Boolean) -> Unit) {
                crossBlockScrollScope.launch {
                    val pageHeight = (listState.layoutInfo.viewportSize.height - latestTopBarClearancePx - crossBlockRevealMarginPx)
                        .coerceAtLeast(crossBlockRevealMarginPx)
                    val scrolled = listState.scrollBy(if (downward) pageHeight else -pageHeight)
                    withFrameNanos {}
                    withFrameNanos {}
                    afterScroll(scrolled != 0f)
                }
            }
        }
        onDispose { crossBlockSelection.host = null }
    }

    val selectionBoxColor = MaterialTheme.colorScheme.primary
    val crossBlockDragModifier = if (isDesktopPlatform) {
        Modifier
            .selectAcrossBlocksByDragging(
                selection = crossBlockSelection,
                scrollableState = listState,
                topInsetPx = topBarClearancePx,
                isBlockSelectionActive = { latestSelectedBlockIds.isNotEmpty() },
                focusedBlockId = { latestActiveBlockId },
                onPress = { pendingCaretAfterEdit = null },
                onRightClickInsideSelection = { pressInWindow -> crossBlockMenuPositionInEditor = pressInWindow - editorTopLeftInWindow },
                onDragEndedInsideOneBlock = { blockId, selectedRange -> placeCaretInBlock(blockId, selectedRange) },
                selectedBlockIds = { latestSelectedBlockIds },
                selectBlocksWithBox = if (onSelectBlocks != null) ({ ids -> latestOnSelectBlocks?.invoke(ids) }) else null,
                onSelectingBlocksWithBoxStarted = {
                    runCatching { selectionShortcutFocusRequester.requestFocus() }
                    activeBlockId = null
                    GlobalEditorState.currentlyFocusedBlockId = null
                    localFocusRequest = null
                    showSlashMenu = false
                }
            )
            .drawBlockSelectionBox(crossBlockSelection, selectionBoxColor)
    } else {
        Modifier.selectTextAcrossBlocksByTouch(
            selection = crossBlockSelection,
            scrollableState = listState,
            topInsetPx = topBarClearancePx,
            isBlockSelectionActive = { latestSelectedBlockIds.isNotEmpty() }
        )
    }

    if (!isDesktopPlatform) {
        val isSelectingTextAcrossBlocks by remember { derivedStateOf { crossBlockSelection.isActive } }
        KmpBackHandler(enabled = isSelectingTextAcrossBlocks) { crossBlockSelection.clear() }
        DisposableEffect(isSelectingTextAcrossBlocks) {
            if (isSelectingTextAcrossBlocks) {
                GlobalEditorState.clearTextSelectionAcrossBlocks = { crossBlockSelection.clear() }
            }
            onDispose { GlobalEditorState.clearTextSelectionAcrossBlocks = null }
        }
        LaunchedEffect(isSelectionMode) {
            if (isSelectionMode) crossBlockSelection.clear()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .onGloballyPositioned { editorTopLeftInWindow = it.positionInWindow() }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (isDesktopPlatform) GlobalEditorState.undoOrRedoShortcutOfLastUsedEditor = undoOrRedoOnShortcut
                    GlobalEditorState.pressStartedOnNewDatabaseRow = false
                    GlobalEditorState.pressStartedOnViewPill = false
                    while (true) {
                        val change = awaitPointerEvent(PointerEventPass.Initial).changes
                            .firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
                        if (!change.pressed) {
                            if (!GlobalEditorState.pressStartedOnNewDatabaseRow) GlobalEditorState.tapsOutsideNewDatabaseRows++
                            return@awaitEachGesture
                        }
                    }
                }
            }
            .then(
                if (isDesktopPlatform) {
                    Modifier
                        .focusRequester(selectionShortcutFocusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent { keyEvent ->
                            if (handleCrossBlockSelectionKey(keyEvent, crossBlockSelection, crossBlockCommands)) return@onPreviewKeyEvent true
                            if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            if (keyEvent.key == Key.Escape) {
                                if (!isSelectionMode) return@onPreviewKeyEvent false
                                closeSelectionMenu()
                                latestOnClearSelection()
                                return@onPreviewKeyEvent true
                            }
                            if (deleteSelectedBlocksOnDeleteKey(keyEvent)) return@onPreviewKeyEvent true
                            if (moveCaretFromSelectedBlock(keyEvent)) return@onPreviewKeyEvent true
                            undoOrRedoOnShortcut(keyEvent)
                        }
                        .onKeyEvent { keyEvent ->
                            val isEscapeWhileTyping = keyEvent.type == KeyEventType.KeyDown &&
                                keyEvent.key == Key.Escape &&
                                activeBlockId != null
                            if (!isEscapeWhileTyping) return@onKeyEvent false
                            focusManager.clearFocus()
                            activeBlockId = null
                            GlobalEditorState.currentlyFocusedBlockId = null
                            localFocusRequest = null
                            showSlashMenu = false
                            true
                        }
                } else {
                    Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            GlobalEditorState.pressStartedOnCanvasBlock = false
                            if (GlobalEditorState.focusedCanvasBlockId == null) return@awaitEachGesture
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                    .firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                                if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) return@awaitEachGesture
                                if (!change.pressed) {
                                    if (!GlobalEditorState.pressStartedOnCanvasBlock) {
                                        change.consume()
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    }
                                    return@awaitEachGesture
                                }
                            }
                        }
                    }
                        .onFocusChanged { hasFocusInsideEditor = it.hasFocus }
                        .focusRequester(selectionShortcutFocusRequester)
                        .focusTarget()
                        .onPreviewKeyEvent { keyEvent ->
                            handleCrossBlockSelectionKey(keyEvent, crossBlockSelection, crossBlockCommands) ||
                                deleteSelectedBlocksOnDeleteKey(keyEvent)
                        }
                }
            )
    ) {
        val calloutFrames = remember(blocks) { calloutFramesByRow(blocks) }
        val idsOfChildBlocks = remember(blocks) { blocks.idsOfBlocksInsideAToggleOrCallout() }
        val stopTypingOnEmptySpaceTap: () -> Unit = {
            focusManager.clearFocus()
            keyboardController?.hide()
            activeBlockId = null
            GlobalEditorState.currentlyFocusedBlockId = null
            localFocusRequest = null
            showSlashMenu = false
            onMobileMenuStateChange(MobileMenuState.MAIN)
            clearSelectionOnTap()
            wrappedActions.onOutsideTap()
        }
        val startTypingAtEndOfNote: () -> Unit = {
            val lastBlock = currentBlocks.lastOrNull()
            if (lastBlock == null) {
                wrappedActions.onAddBlankBlock()
            } else {
                val isMediaBlock = lastBlock is BookmarkBlock
                        || lastBlock is ImageBlock
                        || lastBlock is DocumentBlock
                        || lastBlock is VoiceBlock
                        || lastBlock is CanvasBlock
                        || lastBlock is PropertyBlock
                        || lastBlock is DatabaseBlock
                        || lastBlock is MovieBlock
                        || lastBlock is WatchlistBlock
                        || lastBlock is CredentialsBlock

                if (isMediaBlock) {
                    wrappedActions.onFocusBlock(lastBlock.id)
                    wrappedActions.onAddBlankBlock()
                } else {
                    activeBlockId = lastBlock.id
                    GlobalEditorState.currentlyFocusedBlockId = lastBlock.id
                    wrappedActions.onFocusBlock(lastBlock.id)
                    localFocusRequest = FocusRequest(id = lastBlock.id, placeCursorAtEnd = true)
                }
            }
        }
        val latestOnEmptySpaceTap by rememberUpdatedState {
            if (singleTapStartsTyping && latestSelectedBlockIds.isEmpty()) {
                showSlashMenu = false
                startTypingAtEndOfNote()
            } else {
                stopTypingOnEmptySpaceTap()
            }
        }
        CompositionLocalProvider(
            LocalBringIntoViewSpec provides bringIntoViewSpec,
            LocalCrossBlockSelection provides crossBlockSelection
        ) {
        LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .smoothWheelScroll(listState)
                    .then(crossBlockDragModifier)
                    .pointerInput(singleTapStartsTyping) {
                        detectTapGestures(
                            onTap = { latestOnEmptySpaceTap() },
                            onDoubleTap = if (singleTapStartsTyping) null else { _ -> startTypingAtEndOfNote() }
                        )
                    },
                contentPadding = PaddingValues(top = topContentPadding, bottom = 0.dp)
            ) {
                if (headerContent != null) {
                    item(key = "page_header", contentType = "PageHeader") {
                        headerContent()
                    }
                }

                val allTasks = blocks.filterIsInstance<CheckboxBlock>()
                if (allTasks.isNotEmpty()) {
                    item(key = "stats_header", contentType = "StatsHeader") {
                        val doneCount = allTasks.count { it.isChecked }
                        val pendingCount = allTasks.size - doneCount

                        Row(
                            modifier = Modifier
                                .centeredWithMaxWidth(contentMaxWidth)
                                .fillMaxWidth()
                                .padding(horizontal = (if (isDesktopPlatform) 32.dp else 17.dp) + extraBlockSidePadding)
                                .padding(bottom = 20.dp, top = if (isDesktopPlatform) 0.dp else 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskBadge(
                                icon = Icons.Default.RadioButtonUnchecked,
                                label = "$pendingCount Pending"
                            )
                            TaskBadge(icon = Icons.Default.CheckCircle, label = "$doneCount Done")
                        }
                    }
                }

                itemsIndexed(
                    items = blocks,
                    key = { _, it -> it.id },
                    contentType = { _, it -> it::class.simpleName }
                ) { index, block ->
                    val targetedFocusRequest = when {
                        activeFocusRequest == null -> null
                        activeFocusRequest.id == block.id -> activeFocusRequest
                        else -> null
                    }

                    val previousBlock = blocks.getOrNull(index - 1)
                    val isFirstChildOfToggleOrCallout = (previousBlock is ToggleBlock || previousBlock is CalloutBlock) &&
                        block.indentationLevel == previousBlock.indentationLevel + 1

                    if (sectionLabelFor != null) {
                        val label = sectionLabelFor(block)
                        val previousLabel = previousBlock?.let(sectionLabelFor)
                        if (label != null && label != previousLabel) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .centeredWithMaxWidth(contentMaxWidth)
                                    .fillMaxWidth()
                                    .padding(
                                        start = (if (isDesktopPlatform) 36.dp else 20.dp) + extraBlockSidePadding,
                                        end = (if (isDesktopPlatform) 36.dp else 16.dp) + extraBlockSidePadding
                                    )
                                    .padding(top = if (index == 0) 0.dp else 20.dp, bottom = 8.dp)
                            )
                        }
                    }

                    val blockHasNoText = block.editableTextOrNull() == null
                    DisposableEffect(block.id) {
                        onDispose { crossBlockSelection.forgetBlockCoordinates(block.id) }
                    }
                    val crossBlockSelectionModifier = Modifier
                        .onGloballyPositioned { crossBlockSelection.rememberBlockCoordinates(block.id, it) }
                        .drawWithContent {
                            drawContent()
                            if (blockHasNoText && crossBlockSelection.isBlockInRange(block.id)) {
                                drawRoundRect(wholeBlockSelectionTint, cornerRadius = CornerRadius(8.dp.toPx()))
                            }
                        }
                    Box(
                        modifier = Modifier
                            .centeredWithMaxWidth(contentMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = extraBlockSidePadding)
                            .drawCalloutFrames(calloutFrames[index], MaterialTheme.colorScheme.surface)
                            .then(crossBlockSelectionModifier)
                    ) {
                        NoteBlockItem(
                            block = block,
                            displayedIndentationLevel = displayedIndentationLevelOf(block, calloutFrames[index]),
                            showsPinIcon = block.isPinned && block.id !in idsOfChildBlocks,
                            modifier = Modifier.calloutContentPadding(calloutFrames[index]),
                            allLinkableNotes = allLinkableNotes,
                            customProperties = customProperties,
                            actions = wrappedActions,
                            focusRequest = targetedFocusRequest,
                            selectedBlockIds = immutableSelectedIds,
                            inSelectionMode = isSelectionMode,
                            activeBlockId = activeBlockId,
                            onFocus = onFocusBlock,
                            showSlashMenu = showSlashMenu,
                            slashQuery = slashQuery,
                            onDismissSlashMenu = onDismissSlash,
                            showNoteLinkMenu = showNoteLinkMenu,
                            onDismissNoteLinkMenu = dismissNoteLinkMenu,
                            showCanvasLinkMenu = showCanvasLinkMenu,
                            onDismissCanvasLinkMenu = dismissCanvasLinkMenu,
                            showDatabaseLinkMenu = showDatabaseLinkMenu,
                            onDismissDatabaseLinkMenu = dismissDatabaseLinkMenu,
                            onLinkMenuBack = if (isDesktopLinkMenuFromSlash) reopenSlashMenu else null,
                            isFirstChildOfToggleOrCallout = isFirstChildOfToggleOrCallout,
                            emptyNotePlaceholder = if (blocks.size == 1) emptyNotePlaceholder else "",
                            selectionRequest = localSelectionRequest ?: selectionRequest,
                            validNoteIds = validNoteIds,
                            onRightClick = if (showsSelectionMenuOnRightClick) openSelectionMenuAt else null,
                            onShiftClick = selectBlockRangeTo,
                            trailingLabel = trailingLabelFor?.invoke(block)
                        )
                    }
                }
                if (blocks.isNotEmpty()) {
                    item(key = "bottom_tap_area", contentType = "BottomTapArea") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dynamicBottomPadding + bottomContentPadding + toolbarOffset)
                                .pointerInput(singleTapStartsTyping) {
                                    detectTapGestures(
                                        onTap = { latestOnEmptySpaceTap() },
                                        onDoubleTap = if (singleTapStartsTyping) null else { _ -> startTypingAtEndOfNote() }
                                    )
                                }
                        )
                    }
                }
            }
        }

        if (emptyContent != null) {
            val hasNothingButOneBlankLine = blocks.size == 1 &&
                    (blocks.first() as? TextBlock)?.text?.isBlank() == true
            val isEditorVisuallyEmpty = blocks.isEmpty() || hasNothingButOneBlankLine
            val keyboardInsets = WindowInsets.ime
            val emptyContentHeight = remember { HeightWithoutKeyboard() }

            AnimatedVisibility(
                visible = isEditorVisuallyEmpty && activeBlockId == null,
                enter = fadeIn(tween(durationMillis = 400, delayMillis = 200)),
                exit = fadeOut(tween(durationMillis = 180)),
                modifier = Modifier.keepsHeightWhileKeyboardShows(keyboardInsets, emptyContentHeight).fillMaxSize()
            ) {
                emptyContent()
            }
        }

        if (!isDesktopPlatform) {
            CrossBlockSelectionHandles(
                selection = crossBlockSelection,
                listState = listState,
                topInsetPx = topBarClearancePx
            )
            val canEditText = wrappedActions.canEditTextAcrossBlocks
            CrossBlockSelectionBar(
                selection = crossBlockSelection,
                listState = listState,
                topInsetPx = topBarClearancePx,
                editorTopLeftInWindow = editorTopLeftInWindow,
                actions = TextSelectionMenuActions(
                    copy = {
                        crossBlockCommands.copy()
                        crossBlockSelection.clear()
                    },
                    cut = if (canEditText) ({ crossBlockCommands.cut() }) else null,
                    paste = if (canEditText) ({ crossBlockCommands.paste() }) else null,
                    delete = if (canEditText) ({ crossBlockCommands.replaceWith("") }) else null,
                    selectAll = { crossBlockSelection.selectWholeNote() },
                    toggleFormat = if (canEditText) ({ format -> crossBlockCommands.toggleFormat(format) }) else null
                )
            )
        }

        EmberrVerticalScrollbar(
            listState = listState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topContentPadding, bottom = bottomContentPadding)
        )

        val crossBlockMenuPosition = crossBlockMenuPositionInEditor?.takeIf { crossBlockSelection.isActive }
        if (crossBlockMenuPosition != null) {
            val canEditText = wrappedActions.canEditTextAcrossBlocks
            TextSelectionMenu(
                positionInParent = crossBlockMenuPosition,
                actions = TextSelectionMenuActions(
                    copy = { crossBlockCommands.copy() },
                    cut = if (canEditText) ({ crossBlockCommands.cut() }) else null,
                    paste = if (canEditText) ({ crossBlockCommands.paste() }) else null,
                    delete = if (canEditText) ({ crossBlockCommands.replaceWith("") }) else null,
                    selectAll = { crossBlockSelection.selectWholeNote() },
                    toggleFormat = if (canEditText) ({ format -> crossBlockCommands.toggleFormat(format) }) else null
                ),
                onClose = {
                    crossBlockMenuPositionInEditor = null
                    runCatching { selectionShortcutFocusRequester.requestFocus() }
                }
            )
        }

        val selectionMenuPosition = selectionMenuPositionInEditor
        if (selectionMenuContent != null && selectionMenuPosition != null) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            selectionMenuPosition.x.roundToInt(),
                            selectionMenuPosition.y.roundToInt()
                        )
                    }
                    .size(1.dp)
            ) {
                EmberrDesktopMenu(
                    expanded = true,
                    onDismissRequest = {
                        closeSelectionMenu()
                        latestOnClearSelection()
                    },
                    modifier = Modifier.width(260.dp)
                ) {
                    val menuKeyFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { menuKeyFocusRequester.requestFocus() } }
                    Box(
                        modifier = Modifier
                            .focusRequester(menuKeyFocusRequester)
                            .focusTarget()
                            .onPreviewKeyEvent { keyEvent -> deleteSelectedBlocksOnDeleteKey(keyEvent) }
                    ) {
                        selectionMenuContent(closeSelectionMenu)
                    }
                }
            }
        }
    }
}

private fun findBlockRecursive(blocks: List<NoteBlock>, id: String): NoteBlock? =
    blocks.find { it.id == id }

@Composable
private fun TaskBadge(icon: ImageVector, label: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

internal fun Modifier.centeredWithMaxWidth(maxWidth: Dp): Modifier =
    fillMaxWidth().wrapContentWidth().widthIn(max = maxWidth)
