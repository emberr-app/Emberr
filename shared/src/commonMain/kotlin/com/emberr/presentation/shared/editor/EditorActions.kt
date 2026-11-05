package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Stable
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.LinkableDatabase
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.CalloutType
import com.emberr.domain.model.CredentialsEdit
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.PropertyDateRange
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.shared.editor.blockViews.database.DatabaseBlockEditor
import com.emberr.presentation.shared.editor.blockViews.movie.MovieBlockEditor
import com.emberr.presentation.shared.editor.blockViews.movie.WatchlistBlockEditor
import com.emberr.presentation.shared.editor.markdown.MarkdownShortcut
import com.emberr.presentation.shared.editor.selection.CrossBlockTextRange
import com.emberr.presentation.shared.editor.selection.TextCaret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ImageLinkFetch(val address: String, val hasFailed: Boolean)

private val noImageLinkFetches: StateFlow<Map<String, ImageLinkFetch>> = MutableStateFlow<Map<String, ImageLinkFetch>>(emptyMap()).asStateFlow()

@Stable
interface EditorActions {

    fun onClearSlashQuery()
    fun onReplaceSlashQuery(replacement: String)
    fun onClearFocusRequest()
    fun onUpdateText(id: String, text: String)
    fun onToggleCheckbox(id: String, checked: Boolean)
    fun onToggleExpand(id: String)
    fun onFocusBlock(id: String)
    fun onChangeBlockType(type: String)
    fun onToggleFormat(format: String)
    fun onAdjustIndentation(increase: Boolean)
    fun onSetBlockAlignment(alignment: TextAlignment)
    fun onEnterPressed(id: String, before: String, after: String)
    fun onShiftEnterPressed(id: String, before: String, after: String) = onEnterPressed(id, before, after)
    fun onBackspaceOnEmpty(id: String)
    fun onBackspaceAtStartOfText(id: String): TextCaret? = null
    fun onDeleteAtEndOfText(id: String): TextCaret? = null
    fun backspaceLeavesTopOfNote(id: String): Boolean = false
    val canEditTextAcrossBlocks: Boolean get() = false
    fun onBlocksToCopy(range: CrossBlockTextRange, visibleBlocksInRange: List<NoteBlock>): List<NoteBlock> = visibleBlocksInRange
    fun onReplaceTextAcrossBlocks(range: CrossBlockTextRange, blockIdsInRange: List<String>, replacement: String): TextCaret? = null
    fun onPasteMarkdown(blockId: String, replacedStart: Int, replacedEnd: Int, markdown: String): TextCaret? = null
    fun onMarkdownShortcut(blockId: String, literalText: String, shortcut: MarkdownShortcut): Boolean = false
    fun onPasteOverTextAcrossBlocks(range: CrossBlockTextRange, blockIdsInRange: List<String>, pastedText: String): TextCaret? = null
    fun onSplitAtTextAcrossBlocks(range: CrossBlockTextRange, blockIdsInRange: List<String>): TextCaret? = null
    fun onAdjustIndentationOfBlocks(blockIds: Set<String>, increase: Boolean) {}
    fun onToggleFormatAcrossBlocks(range: CrossBlockTextRange, blockIdsInRange: List<String>, format: String) {}
    fun onToggleSelection(id: String)
    fun onUpdateReminder(id: String, timestamp: Long?, allDay: AllDaySpan?)
    fun onOpenEventOptions(blockId: String, occurrenceDate: String?) {}
    fun onUrlSubmit(id: String, url: String)
    fun onImagePicked(id: String, uri: String)
    fun onDocumentPicked(id: String, uri: String)
    fun onAddBlankBlock()
    fun onInsertMediaBlock(type: String)
    fun onOutsideTap()
    fun onVoiceRecorded(id: String, filePath: String, duration: Int)
    fun onRemoveVoice(id: String)
    fun onStartRecording()
    fun onStopRecording(blockId: String, cancel: Boolean)
    fun onPlayAudio(filePath: String, onComplete: () -> Unit)
    fun onStopAudio()
    fun onDeleteBlock(id: String)
    fun onRequestImagePicker(blockId: String)
    fun onImageLinkSubmitted(blockId: String, address: String) {}
    val imageLinkFetches: StateFlow<Map<String, ImageLinkFetch>> get() = noImageLinkFetches
    fun onRequestDocumentPicker(blockId: String)
    fun onOpenFile(filePath: String, mimeType: String)
    fun onTogglePin()
    fun onUpdateTable(id: String, rows: List<List<String>>)
    fun onUpdateCredentials(id: String, edit: CredentialsEdit)
    fun onUpdateTableStyle(
        id: String,
        cellStyles: Map<String, com.emberr.domain.model.TableCellStyle>,
        rowStyles: Map<String, com.emberr.domain.model.TableCellStyle>,
        columnStyles: Map<String, com.emberr.domain.model.TableCellStyle>
    )
    fun onAddBlockAbove(id: String)
    fun onAddBlockBelow(id: String)
    fun onNoteLinkClick(noteId: String)
    fun onCreateLinkedNote(title: String): String
    fun onInsertLinkedNoteBlock(noteId: String) {}
    fun onInsertCanvasBlock(canvasNoteId: String) {}
    suspend fun getLinkableCanvases(): List<NoteMetadataEntity> = emptyList()
    fun onInsertLinkedDatabaseBlock(databaseId: String) {}
    suspend fun getLinkableDatabases(): List<LinkableDatabase> = emptyList()
    fun onRequestCamera(blockId: String)
    suspend fun getNoteMetadata(noteId: String): NoteMetadataEntity?
    fun onUpdateLinkedNoteOptions(id: String, showIcon: Boolean, showCoverImage: Boolean)
    fun onUpdateCalloutStyle(id: String, calloutType: CalloutType, isFoldable: Boolean)
    fun onUpdatePropertyText(id: String, text: String)
    fun onUpdatePropertyDate(id: String, range: PropertyDateRange)
    fun onUpdatePropertyTags(id: String, tags: List<String>)
    fun onUpdatePropertyChecked(id: String, isChecked: Boolean)
    fun onUpdatePropertyPeople(id: String, personNoteIds: List<String>) {}
    val databaseBlockEditor: DatabaseBlockEditor?
    val movieBlockEditor: MovieBlockEditor? get() = null
    val watchlistBlockEditor: WatchlistBlockEditor? get() = null
}
