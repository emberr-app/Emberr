package com.emberr.presentation.shared.editor.toolbar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.LinkableDatabase
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.SeparatePaperGrainBox
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.keyboard
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import com.emberr.presentation.shared.editor.EditorEventBus
import com.emberr.presentation.shared.editor.GlobalEditorState
import com.emberr.presentation.shared.editor.NOTE_TITLE_FIELD_ID
import com.emberr.presentation.shared.editor.links.CanvasLinkMenuContent
import com.emberr.presentation.shared.editor.links.DatabaseLinkMenuContent
import com.emberr.presentation.shared.editor.links.NoteLinkMenuContent
import com.emberr.presentation.shared.editor.mention.MENTION_RESULTS_PER_STEP
import com.emberr.presentation.shared.editor.mention.mentionMenuSections
import com.emberr.presentation.shared.editor.slash.DesktopSlashMenuContent
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.rememberCustomProperties
import com.emberr.presentation.shared.editor.text.currentDateText
import com.emberr.presentation.shared.editor.text.currentTimeText

enum class MobileMenuState { MAIN, SLASH, MENU, MENTION, LINK_TO_NOTE, LINK_TO_CANVAS, LINK_TO_DATABASE }

@Composable
fun EditorToolbar(
    modifier: Modifier = Modifier,
    mobileMenuState: MobileMenuState,
    onMenuStateChange: (MobileMenuState) -> Unit,
    query: String,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    onInsertMediaBlock: (String) -> Unit,
    onSelectCurrentBlock: () -> Unit,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    showHistory: Boolean = false,
    onClearSlashQuery: () -> Unit = {},
    onReplaceSlashQuery: (String) -> Unit = {},
    allLinkableNotes: List<NoteMetadataEntity> = emptyList(),
    mentionQuery: String = "",
    onMentionNoteSelected: (String) -> Unit = {},
    onMentionCreateNote: (String) -> Unit = {},
    onMentionCreateBlank: () -> Unit = {},
    onNoteLinkSelected: (String) -> Unit = {},
    onNoteLinkCreateNote: (String) -> Unit = {},
    onNoteLinkCreateBlank: () -> Unit = {},
    onCanvasLinkSelected: (String) -> Unit = {},
    loadLinkableCanvases: suspend () -> List<NoteMetadataEntity> = { emptyList() },
    onDatabaseLinkSelected: (String) -> Unit = {},
    loadLinkableDatabases: suspend () -> List<LinkableDatabase> = { emptyList() },
    hazeState: HazeState
) {
    if (isDesktopPlatform) return
    val customProperties = rememberCustomProperties()

    val keyboardController = LocalSoftwareKeyboardController.current
    val tint = MaterialTheme.colorScheme.primary
    val customIconSize = 18.dp
    var isChoosingHighlightColor by remember { mutableStateOf(false) }

    val isTextSelectedAcrossBlocks = GlobalEditorState.clearTextSelectionAcrossBlocks != null
    LaunchedEffect(mobileMenuState, isTextSelectedAcrossBlocks) { isChoosingHighlightColor = false }
    val isEnabled = !isTextSelectedAcrossBlocks
    val buttonTint = if (isEnabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    val settingsManager: SettingsManager = koinInject()
    val savedToolbarItems by settingsManager.keyboardToolbarItemsFlow.collectAsState(
        initial = settingsManager.getKeyboardToolbarItems()
    )
    val isNoteTitleFocused = GlobalEditorState.isNoteTitleFocused
    val toolbarItems = remember(savedToolbarItems, showHistory, isNoteTitleFocused) {
        if (isNoteTitleFocused) {
            listOf(KeyboardToolbarItem.CURRENT_TIME, KeyboardToolbarItem.CURRENT_DATE)
        } else {
            keyboardToolbarItemsToShow(keyboardToolbarItemsFrom(savedToolbarItems), showHistory)
        }
    }
    var isLinkPanelFromSlash by remember { mutableStateOf(false) }
    val finishSlashLink: () -> Unit = {
        if (isLinkPanelFromSlash) {
            isLinkPanelFromSlash = false
            onClearSlashQuery()
        }
    }
    val leaveLinkPanel: () -> Unit = {
        if (isLinkPanelFromSlash) {
            isLinkPanelFromSlash = false
            onMenuStateChange(MobileMenuState.SLASH)
            EditorEventBus.refocusActiveBlockEvent.tryEmit(Unit)
        } else {
            onMenuStateChange(MobileMenuState.MENU)
        }
    }
    val insertBlockFromToolbar: (String) -> Unit = { type ->
        isLinkPanelFromSlash = false
        onInsertMediaBlock(type)
        onMenuStateChange(mobileMenuStateAfterInserting(type))
    }
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val insertTextAtCursor: (String) -> Unit = { text ->
        val targetId = if (GlobalEditorState.isNoteTitleFocused) NOTE_TITLE_FIELD_ID else GlobalEditorState.currentlyFocusedBlockId
        targetId?.let { EditorEventBus.requestInsertText(it, text) }
    }
    val onToolbarItemClick: (KeyboardToolbarItem) -> Unit = { item ->
        when (item) {
            KeyboardToolbarItem.UNDO -> {
                GlobalEditorState.clearTextSelectionAcrossBlocks?.invoke()
                onUndo()
            }
            KeyboardToolbarItem.REDO -> {
                GlobalEditorState.clearTextSelectionAcrossBlocks?.invoke()
                onRedo()
            }
            KeyboardToolbarItem.LINK_TO_NOTE -> GlobalEditorState.currentlyFocusedBlockId?.let {
                EditorEventBus.requestInsertMention(it)
            }
            KeyboardToolbarItem.SELECT_BLOCK -> {
                keyboardController?.hide()
                onSelectCurrentBlock()
            }
            KeyboardToolbarItem.CURRENT_TIME -> insertTextAtCursor(currentTimeText(usesTwentyFourHourClock))
            KeyboardToolbarItem.CURRENT_DATE -> insertTextAtCursor(currentDateText())
            KeyboardToolbarItem.TEXT -> onChangeBlockType("text")
            KeyboardToolbarItem.HEADING_1 -> onChangeBlockType("h1")
            KeyboardToolbarItem.HEADING_2 -> onChangeBlockType("h2")
            KeyboardToolbarItem.CHECKBOX -> onChangeBlockType("checkbox")
            KeyboardToolbarItem.BULLETED_LIST -> onChangeBlockType("bullet")
            KeyboardToolbarItem.NUMBERED_LIST -> onChangeBlockType("number")
            KeyboardToolbarItem.TOGGLE_LIST -> onChangeBlockType("toggle")
            KeyboardToolbarItem.QUOTE -> onChangeBlockType("quote")
            KeyboardToolbarItem.CALLOUT -> onChangeBlockType("callout")
            KeyboardToolbarItem.CODE -> onChangeBlockType("code")
            KeyboardToolbarItem.VOICE_NOTE -> onChangeBlockType("voice")
            KeyboardToolbarItem.IMAGE -> insertBlockFromToolbar("image")
            KeyboardToolbarItem.DOCUMENT -> insertBlockFromToolbar("document")
            KeyboardToolbarItem.WEB_BOOKMARK -> insertBlockFromToolbar("bookmark")
            KeyboardToolbarItem.SIMPLE_TABLE -> insertBlockFromToolbar("table")
            KeyboardToolbarItem.DATABASE -> insertBlockFromToolbar("database")
            KeyboardToolbarItem.CANVAS -> insertBlockFromToolbar("canvas")
            KeyboardToolbarItem.LINKED_NOTE -> insertBlockFromToolbar("linked_note")
            KeyboardToolbarItem.LINKED_CANVAS -> insertBlockFromToolbar("linked_canvas")
            KeyboardToolbarItem.LINKED_DATABASE -> insertBlockFromToolbar("linked_database")
            KeyboardToolbarItem.SOLID_LINE_DIVIDER -> onChangeBlockType("divider_solid")
            KeyboardToolbarItem.THREE_DOTS_DIVIDER -> onChangeBlockType("divider_dots")
            KeyboardToolbarItem.BOLD -> onToggleFormat("bold")
            KeyboardToolbarItem.ITALIC -> onToggleFormat("italic")
            KeyboardToolbarItem.STRIKETHROUGH -> onToggleFormat("strike")
            KeyboardToolbarItem.UNDERLINE -> onToggleFormat("underline")
            KeyboardToolbarItem.HIGHLIGHT -> {
                isChoosingHighlightColor = !isChoosingHighlightColor
            }
            KeyboardToolbarItem.ALIGN_LEFT -> onSetAlignment(TextAlignment.LEFT)
            KeyboardToolbarItem.ALIGN_RIGHT -> onSetAlignment(TextAlignment.RIGHT)
            KeyboardToolbarItem.ALIGN_CENTER -> onSetAlignment(TextAlignment.CENTER)
            KeyboardToolbarItem.JUSTIFY -> onSetAlignment(TextAlignment.JUSTIFY)
            KeyboardToolbarItem.DECREASE_INDENT -> onAdjustIndentation(false)
            KeyboardToolbarItem.INCREASE_INDENT -> onAdjustIndentation(true)
            KeyboardToolbarItem.MORE_BLOCKS -> EditorEventBus.insertSlashEvent.tryEmit(Unit)
            KeyboardToolbarItem.DIVIDER -> Unit
        }
    }

    KmpBackHandler(enabled = mobileMenuState != MobileMenuState.MAIN) {
        onMenuStateChange(MobileMenuState.MAIN)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isChoosingHighlightColor) {
            FloatingHighlightColors(
                onColorChosen = onToggleFormat,
                onClose = { isChoosingHighlightColor = false }
            )
        }

        Surface(
            shape = DefaultCornerShape,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .emberrBorder(DefaultCornerShape)
                .customEmberrShadow(DefaultCornerShape)
                .clip(DefaultCornerShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
        ) {
            SeparatePaperGrainBox(shape = DefaultCornerShape, modifier = Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                        when (mobileMenuState) {
                            MobileMenuState.MAIN -> if (GlobalEditorState.hasTextSelection && !isTextSelectedAcrossBlocks && !isNoteTitleFocused) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    InlineFormatButtons(
                                        tint = tint,
                                        iconSize = customIconSize,
                                        onToggleFormat = onToggleFormat,
                                        onHighlightClick = { isChoosingHighlightColor = !isChoosingHighlightColor }
                                    )
                                    ToolbarButton(tooltip = "Close keyboard", onClick = { keyboardController?.hide() }) {
                                        Icon(painterResource(Res.drawable.keyboard), "Close Keyboard", tint = tint, modifier = Modifier.size(customIconSize))
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        toolbarItems.forEach { item ->
                                            if (item == KeyboardToolbarItem.DIVIDER) {
                                                ToolbarDivider(tint)
                                            } else {
                                                val isItemEnabled = when (item) {
                                                    KeyboardToolbarItem.UNDO -> canUndo && GlobalEditorState.openCredentialsFormCount == 0
                                                    KeyboardToolbarItem.REDO -> canRedo && GlobalEditorState.openCredentialsFormCount == 0
                                                    else -> isEnabled
                                                }
                                                val itemTint = when (item) {
                                                    KeyboardToolbarItem.UNDO, KeyboardToolbarItem.REDO ->
                                                        if (isItemEnabled) tint else tint.copy(alpha = 0.3f)
                                                    else -> buttonTint
                                                }
                                                ToolbarButton(enabled = isItemEnabled, tooltip = item.title, onClick = { onToolbarItemClick(item) }) {
                                                    KeyboardToolbarItemIcon(item, itemTint)
                                                }
                                            }
                                        }
                                    }

                                    if (toolbarItems.isNotEmpty()) ToolbarDivider(tint)
                                    ToolbarButton(tooltip = "Close keyboard", onClick = { keyboardController?.hide() }) {
                                        Icon(painterResource(Res.drawable.keyboard), "Close Keyboard", tint = tint, modifier = Modifier.size(customIconSize))
                                    }
                                }
                            }
                            MobileMenuState.SLASH -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                        DesktopSlashMenuContent(
                                            query = query,
                                            onChangeBlockType = {
                                                onClearSlashQuery()
                                                onChangeBlockType(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onToggleFormat = {
                                                onClearSlashQuery()
                                                onToggleFormat(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onAdjustIndentation = {
                                                onClearSlashQuery()
                                                onAdjustIndentation(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onSetAlignment = {
                                                onClearSlashQuery()
                                                onSetAlignment(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onInsertMediaBlock = {
                                                val nextMenuState = mobileMenuStateAfterInserting(it)
                                                isLinkPanelFromSlash = nextMenuState != MobileMenuState.MAIN
                                                if (!isLinkPanelFromSlash) onClearSlashQuery()
                                                onInsertMediaBlock(it)
                                                onMenuStateChange(nextMenuState)
                                            },
                                            onInsertText = {
                                                onReplaceSlashQuery("$it ")
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            customProperties = customProperties
                                        )
                                    }
                                }
                            }
                            MobileMenuState.MENU -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                                        DesktopSlashMenuContent(
                                            query = "",
                                            onChangeBlockType = {
                                                onChangeBlockType(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onToggleFormat = {
                                                onToggleFormat(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onAdjustIndentation = {
                                                onAdjustIndentation(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onSetAlignment = {
                                                onSetAlignment(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            onInsertMediaBlock = {
                                                isLinkPanelFromSlash = false
                                                onInsertMediaBlock(it)
                                                onMenuStateChange(mobileMenuStateAfterInserting(it))
                                            },
                                            onInsertText = {
                                                insertTextAtCursor(it)
                                                onMenuStateChange(MobileMenuState.MAIN)
                                            },
                                            customProperties = customProperties
                                        )
                                    }
                                }
                            }
                            MobileMenuState.MENTION -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
                                        var shownNoteCount by remember(mentionQuery) { mutableIntStateOf(MENTION_RESULTS_PER_STEP) }
                                        var shownPeopleCount by remember(mentionQuery) { mutableIntStateOf(MENTION_RESULTS_PER_STEP) }
                                        val sections = remember(mentionQuery, allLinkableNotes, shownNoteCount, shownPeopleCount) {
                                            mentionMenuSections(
                                                query = mentionQuery,
                                                mentionableNotes = allLinkableNotes,
                                                shownNoteCount = shownNoteCount,
                                                shownPeopleCount = shownPeopleCount,
                                                onShowMoreNotes = { shownNoteCount += MENTION_RESULTS_PER_STEP },
                                                onShowMorePeople = { shownPeopleCount += MENTION_RESULTS_PER_STEP },
                                                onCreateBlankNote = onMentionCreateBlank,
                                                onCreateNote = onMentionCreateNote,
                                                onPick = { note -> onMentionNoteSelected(note.noteId) }
                                            )
                                        }
                                        SlashMenuList(sections = sections)
                                    }
                                }
                            }
                            MobileMenuState.LINK_TO_NOTE -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    NoteLinkMenuContent(
                                        allLinkableNotes = allLinkableNotes,
                                        onNoteSelected = {
                                            finishSlashLink()
                                            onNoteLinkSelected(it)
                                            onMenuStateChange(MobileMenuState.MAIN)
                                        },
                                        onCreateNote = {
                                            finishSlashLink()
                                            onNoteLinkCreateNote(it)
                                            onMenuStateChange(MobileMenuState.MAIN)
                                        },
                                        onCreateBlankNote = {
                                            finishSlashLink()
                                            onNoteLinkCreateBlank()
                                            onMenuStateChange(MobileMenuState.MAIN)
                                        },
                                        autoFocusSearch = false,
                                        onBack = leaveLinkPanel
                                    )
                                }
                            }
                            MobileMenuState.LINK_TO_CANVAS -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    CanvasLinkMenuContent(
                                        loadCanvases = loadLinkableCanvases,
                                        onCanvasSelected = {
                                            finishSlashLink()
                                            onCanvasLinkSelected(it)
                                            onMenuStateChange(MobileMenuState.MAIN)
                                        },
                                        autoFocusSearch = false,
                                        onBack = leaveLinkPanel
                                    )
                                }
                            }
                            MobileMenuState.LINK_TO_DATABASE -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    MenuDragHandle(onClose = { onMenuStateChange(MobileMenuState.MAIN) }, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                                    DatabaseLinkMenuContent(
                                        loadDatabases = loadLinkableDatabases,
                                        onDatabaseSelected = {
                                            finishSlashLink()
                                            onDatabaseLinkSelected(it)
                                            onMenuStateChange(MobileMenuState.MAIN)
                                        },
                                        autoFocusSearch = false,
                                        onBack = leaveLinkPanel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class HeightWithoutKeyboard {
    var px = 0
}

internal fun Modifier.keepsHeightWhileKeyboardShows(keyboardInsets: WindowInsets, heightWithoutKeyboard: HeightWithoutKeyboard): Modifier =
    layout { measurable, constraints ->
        val isKeyboardShowing = keyboardInsets.getBottom(this) > 0
        if (!isKeyboardShowing || heightWithoutKeyboard.px == 0) heightWithoutKeyboard.px = constraints.maxHeight
        val height = maxOf(heightWithoutKeyboard.px, constraints.maxHeight)
        val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
        layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
    }

private fun mobileMenuStateAfterInserting(type: String): MobileMenuState = when (type) {
    "linked_note" -> MobileMenuState.LINK_TO_NOTE
    "linked_canvas" -> MobileMenuState.LINK_TO_CANVAS
    "linked_database" -> MobileMenuState.LINK_TO_DATABASE
    else -> MobileMenuState.MAIN
}

private val MenuHandleTouchWidth = 80.dp

@Composable
internal fun MenuDragHandle(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(MenuHandleTouchWidth)
            .padding(top = 10.dp, bottom = 8.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 15f) {
                        onClose()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        )
    }
}
