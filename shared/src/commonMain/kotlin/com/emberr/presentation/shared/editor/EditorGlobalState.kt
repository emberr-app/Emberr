package com.emberr.presentation.shared.editor

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.text.TextRange
import com.emberr.domain.model.NoteBlock
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.TimeSource

fun Modifier.hidesEditorToolbarWhileFocused(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onDispose { if (isFocused) GlobalEditorState.focusedFieldsHidingToolbar -= 1 }
    }
    onFocusChanged { focusState ->
        if (focusState.hasFocus != isFocused) {
            isFocused = focusState.hasFocus
            GlobalEditorState.focusedFieldsHidingToolbar += if (focusState.hasFocus) 1 else -1
        }
    }
}

object GlobalEditorState {
    var currentlyFocusedBlockId: String? = null

    var focusedFieldsHidingToolbar by mutableIntStateOf(0)

    var isPointerOverWatchlistTitle: Boolean = false

    var lastCopiedMarkdown: String? = null

    var lastCopiedBlocks: List<NoteBlock>? = null

    var clearTextSelectionAcrossBlocks by mutableStateOf<(() -> Unit)?>(null)

    var currentlyFocusedTableCellKey: String? = null

    var focusedCanvasBlockId: String? = null

    var openCredentialsFormCount by mutableIntStateOf(0)

    val newCredentialsBlocksAwaitingForm = mutableSetOf<String>()

    var pressStartedOnCanvasBlock: Boolean = false

    var pressStartedOnNewDatabaseRow: Boolean = false

    var isPointerOverDatabaseViewPill: Boolean = false

    var pressStartedOnViewPill: Boolean = false

    var tapsOutsideNewDatabaseRows by mutableIntStateOf(0)

    var undoOrRedoShortcutOfLastUsedEditor: ((KeyEvent) -> Boolean)? = null

    var lastBlockBoundaryEdit: TimeSource.Monotonic.ValueTimeMark? = null

    var hasTextSelection by mutableStateOf(false)
        private set

    var currentSelection: TextRange = TextRange.Zero
        set(value) {
            field = value
            hasTextSelection = !value.collapsed
        }
}

object EditorEventBus {
    val insertSlashEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val cleanupSlashEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refocusActiveBlockEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var insertMentionSignal by mutableStateOf<Pair<String, Long>?>(null)
        private set
    private var insertMentionNonce = 0L
    fun requestInsertMention(blockId: String) {
        insertMentionNonce++
        insertMentionSignal = blockId to insertMentionNonce
    }

    var insertTextSignal by mutableStateOf<InsertTextRequest?>(null)
        private set
    private var insertTextNonce = 0L
    fun requestInsertText(blockId: String, text: String) {
        insertTextNonce++
        insertTextSignal = InsertTextRequest(blockId, text, insertTextNonce)
    }

    val confirmMentionEvent = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)
    val confirmMentionAndOpenEvent = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 1)
    val cancelMentionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

data class InsertTextRequest(val blockId: String, val text: String, val nonce: Long)
