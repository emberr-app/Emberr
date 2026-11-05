package com.emberr.presentation.shared.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.emberr.domain.model.PersonDetailBlock
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
import com.emberr.presentation.shared.editor.blockViews.media.AudioBlockView
import com.emberr.presentation.shared.editor.blockViews.media.BookmarkBlockView
import com.emberr.presentation.shared.editor.blockViews.CanvasBlockView
import com.emberr.presentation.shared.editor.blockViews.media.DocumentBlockView
import com.emberr.presentation.shared.editor.blockViews.media.ImageBlockView
import com.emberr.presentation.shared.editor.blockViews.LinkedNoteBlockView
import com.emberr.presentation.shared.editor.blockViews.TableBlockView
import com.emberr.presentation.shared.editor.blockViews.UnknownBlockView
import com.emberr.presentation.shared.editor.blockViews.credentials.CredentialsBlockView
import com.emberr.presentation.shared.editor.blockViews.database.DatabaseBlockView
import com.emberr.presentation.shared.editor.blockViews.movie.MovieBlockView
import com.emberr.presentation.shared.editor.blockViews.movie.WatchlistBlockView
import com.emberr.presentation.shared.editor.blockViews.property.PropertyBlockView
import com.emberr.presentation.shared.editor.formatting.TextFormatContextMenu
import com.emberr.presentation.shared.editor.blockViews.SolidDividerBlockView
import com.emberr.presentation.shared.editor.blockViews.ThreeDotDividerBlockView

@Composable
internal fun NonTextBlockContent(
    block: NoteBlock,
    actions: EditorActions,
    inSelectionMode: Boolean,
    focusRequest: FocusRequest?,
    afterKeyboardCloses: (action: () -> Unit) -> Unit
) {
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
            onUpdatePeople = { actions.onUpdatePropertyPeople(block.id, it) },
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
        is SolidDividerBlock -> SolidDividerBlockView()
        is ThreeDotDividerBlock -> ThreeDotDividerBlockView()
        is MovieBlock -> MovieBlockView(
            block = block,
            editor = actions.movieBlockEditor,
            inSelectionMode = inSelectionMode,
            onToggleSelection = { actions.onToggleSelection(block.id) },
            onDelete = { actions.onDeleteBlock(block.id) },
            focusRequestNonce = focusRequest?.takeIf { it.id == block.id }?.nonce,
            runAfterKeyboardCloses = afterKeyboardCloses
        )
        is WatchlistBlock -> WatchlistBlockView(
            block = block,
            editor = actions.watchlistBlockEditor,
            inSelectionMode = inSelectionMode,
            onToggleSelection = { actions.onToggleSelection(block.id) },
            onDelete = { actions.onDeleteBlock(block.id) },
            runAfterKeyboardCloses = afterKeyboardCloses
        )
        is CredentialsBlock -> CredentialsBlockView(
            block = block,
            inSelectionMode = inSelectionMode,
            onToggleSelection = { actions.onToggleSelection(block.id) },
            onSave = { edit -> actions.onUpdateCredentials(block.id, edit) },
            runAfterKeyboardCloses = afterKeyboardCloses
        )
        is PersonDetailBlock -> Unit
        is UnknownBlock -> UnknownBlockView()
        is TextBlock, is HeadingBlock, is QuoteBlock, is CheckboxBlock, is BulletedListBlock,
        is NumberedListBlock, is ToggleBlock, is CalloutBlock, is CodeBlock -> Unit
    }
}
