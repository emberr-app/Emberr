package com.emberr.presentation.shared.editor.text

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.markdown.MarkdownClipboardActions
import com.emberr.presentation.shared.editor.markdown.MarkdownShortcut
import com.emberr.presentation.shared.editor.markdown.PastedText
import com.emberr.presentation.shared.editor.markdown.markdownShortcutAfterTyping
import com.emberr.presentation.shared.editor.markdown.pastedTextToReadAsMarkdown
import com.emberr.presentation.shared.editor.selection.BlockTextPosition
import com.emberr.presentation.shared.editor.selection.LocalCrossBlockSelection
import com.emberr.presentation.shared.editor.selection.SelectableTextField
import com.emberr.presentation.shared.editor.selection.SelectionStep
import com.emberr.presentation.shared.editor.selection.selectionStepFor
import com.emberr.presentation.shared.editor.selection.textInsertedBetween
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource
import com.emberr.presentation.shared.editor.EditorEventBus
import com.emberr.presentation.shared.editor.FocusRequest
import com.emberr.presentation.shared.editor.GlobalEditorState
import com.emberr.presentation.shared.editor.SelectionRequest
import com.emberr.presentation.shared.editor.mention.DesktopMentionMenu
import com.emberr.presentation.shared.editor.mention.rememberMentionMenuSections

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

    LaunchedEffect(EditorEventBus.insertMentionSignal) {
        val (targetId, _) = EditorEventBus.insertMentionSignal ?: return@LaunchedEffect
        if (targetId != blockId || !isFieldFocused) return@LaunchedEffect

        val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
        val needsSpace = cursor > 0 && !tfv.text[cursor - 1].isWhitespace()
        val insert = if (needsSpace) " @" else "@"

        val newText = tfv.text.substring(0, cursor) + insert + tfv.text.substring(cursor)
        val newCursor = cursor + insert.length

        tfv = tfv.copy(text = newText, selection = TextRange(newCursor), composition = null)
        lastSentText = newText
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

    val mentionSections = rememberMentionMenuSections(
        mentionQuery = mentionQuery,
        allLinkableNotes = allLinkableNotes,
        onCreateBlankNote = {
            val newNoteId = onCreateLinkedNote("Untitled")
            insertNoteLink("Untitled", newNoteId)
            onOpenNote(newNoteId)
        },
        onCreateNote = { query ->
            val safeTitle = query.replace("[", "").replace("]", "").trim().ifEmpty { "Untitled" }
            val newNoteId = onCreateLinkedNote(safeTitle)
            insertNoteLink(safeTitle, newNoteId)
        },
        onPick = { note ->
            val safeTitle = note.title.replace("[", "").replace("]", "").ifEmpty { "Untitled" }
            insertNoteLink(safeTitle, note.noteId)
        }
    )
    val mentionMenuEntries = remember(mentionSections) { mentionSections.flatMap { section -> section.items } }

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
            DesktopMentionMenu(
                anchorRect = mentionRect,
                sections = mentionSections,
                selectedIndex = mentionSelectedIndex,
                onDismiss = { mentionQuery = null; mentionStartIndex = -1 }
            )
        }
    }
}

internal fun displayedCharacterRect(
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
