package com.emberr.presentation.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PendingShare
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.network.HtmlMetadataFetcher
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.eventbus.ShareEventBus
import com.emberr.domain.util.sync.SyncCoordinator
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.UUID
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.share_failed_to_save
import emberr.shared.generated.resources.share_note_no_longer_available
import emberr.shared.generated.resources.share_saved_items_to_inbox
import emberr.shared.generated.resources.share_saved_to_inbox
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

class ShareViewModel(
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val _currentShare = MutableStateFlow<PendingShare?>(null)
    val currentShare: StateFlow<PendingShare?> = _currentShare.asStateFlow()

    private val _linkableNotes = MutableStateFlow<List<NoteMetadataEntity>>(emptyList())
    val linkableNotes: StateFlow<List<NoteMetadataEntity>> = _linkableNotes.asStateFlow()

    private val _navigateToNoteId = MutableStateFlow<String?>(null)
    val navigateToNoteId: StateFlow<String?> = _navigateToNoteId.asStateFlow()

    init {
        ShareEventBus.pendingShare.onEach {
            _currentShare.value = it
            ShareEventBus.consumePendingShare()
        }.launchIn(viewModelScope)
        repository.getAllLinkableNotes().onEach { _linkableNotes.value = it }.launchIn(viewModelScope)
    }

    private suspend fun notify(message: String) = withContext(Dispatchers.Main) {
        showNativeToast(message)
    }

    private suspend fun getOrCreateInbox(): Pair<NoteMetadataEntity, NoteContent> {
        val allNotes = repository.getAllNotes().first()
        var inboxNote = allNotes.find { it.title.equals("Inbox", ignoreCase = true) }
        val noteId: String
        val content: NoteContent

        if (inboxNote == null) {
            noteId = UUID.randomUUID().toString()
            inboxNote = NoteMetadataEntity(
                noteId = noteId, title = "Inbox", icon = "📥", folderId = null,
                isDaily = false, dateString = null, createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(), filePath = "note_$noteId.json", snippet = "Saved links and ideas."
            )
            content = NoteContent(blocks = emptyList())
        } else {
            noteId = inboxNote.noteId
            content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
        }
        return Pair(inboxNote, content)
    }

    private suspend fun buildSingleBlock(share: PendingShare): NoteBlock? = when (share) {
        is PendingShare.Link -> BookmarkBlock(
            id = UUID.randomUUID().toString(),
            indentationLevel = 0,
            url = share.url,
            title = "Loading preview...",
            description = null,
            previewImageUrl = null
        )
        is PendingShare.Image -> {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(share.uriString)
            if (mediaInfo == null) null else ImageBlock(
                id = UUID.randomUUID().toString(),
                indentationLevel = 0,
                localFilePath = mediaInfo.localFileName
            )
        }
        is PendingShare.Document -> {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(share.uriString)
            if (mediaInfo == null) null else DocumentBlock(
                id = UUID.randomUUID().toString(),
                indentationLevel = 0,
                localFilePath = mediaInfo.localFileName,
                fileName = mediaInfo.originalName,
                mimeType = mediaInfo.mimeType,
                fileSizeString = mediaInfo.formattedSize
            )
        }
        is PendingShare.Multiple -> null
    }

    private suspend fun buildBlocks(share: PendingShare): List<NoteBlock> = when (share) {
        is PendingShare.Multiple -> share.items.mapNotNull { buildSingleBlock(it) }
        else -> listOfNotNull(buildSingleBlock(share))
    }

    private fun scheduleLinkMetadataRefresh(noteId: String, block: BookmarkBlock) {
        appScope.launch(Dispatchers.IO) {
            try {
                val metadata = HtmlMetadataFetcher.fetchMetadata(block.url)
                if (metadata.description == "Could not load preview") return@launch

                SyncCoordinator.mutex.withLock {
                    val meta = repository.getNoteById(noteId) ?: return@withLock
                    val currentContent = repository.getNoteContent(noteId) ?: return@withLock
                    val finalizedBlocks = currentContent.blocks.map {
                        if (it.id == block.id && it is BookmarkBlock) {
                            it.copy(
                                title = metadata.title ?: "Unknown Link",
                                description = metadata.description,
                                previewImageUrl = metadata.imageUrl,
                                updatedAt = System.currentTimeMillis()
                            )
                        } else it
                    }
                    repository.saveNote(meta.copy(updatedAt = System.currentTimeMillis()), NoteContent(blocks = finalizedBlocks))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveToInbox() {
        val share = _currentShare.value ?: return
        _currentShare.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newBlocks = buildBlocks(share)
                if (newBlocks.isEmpty()) {
                    notify(getString(Res.string.share_failed_to_save))
                    return@launch
                }

                var inboxNoteId = ""
                SyncCoordinator.mutex.withLock {
                    val (inboxMeta, content) = getOrCreateInbox()
                    inboxNoteId = inboxMeta.noteId
                    repository.saveNote(
                        inboxMeta.copy(updatedAt = System.currentTimeMillis()),
                        NoteContent(blocks = content.blocks + newBlocks)
                    )
                }

                newBlocks.filterIsInstance<BookmarkBlock>().forEach { scheduleLinkMetadataRefresh(inboxNoteId, it) }
                notify(
                    if (newBlocks.size > 1) getPluralString(Res.plurals.share_saved_items_to_inbox, newBlocks.size, newBlocks.size)
                    else getString(Res.string.share_saved_to_inbox)
                )
            } catch (e: Exception) {
                e.printStackTrace()
                notify(getString(Res.string.share_failed_to_save))
            }
        }
    }

    fun saveToNote(noteId: String) {
        val share = _currentShare.value ?: return
        _currentShare.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newBlocks = buildBlocks(share)
                if (newBlocks.isEmpty()) {
                    notify(getString(Res.string.share_failed_to_save))
                    return@launch
                }

                var saved = false
                SyncCoordinator.mutex.withLock {
                    val meta = repository.getNoteById(noteId) ?: return@withLock
                    val content = repository.getNoteContent(noteId) ?: NoteContent(blocks = emptyList())
                    repository.saveNote(
                        meta.copy(updatedAt = System.currentTimeMillis()),
                        NoteContent(blocks = content.blocks + newBlocks)
                    )
                    saved = true
                }

                if (!saved) {
                    notify(getString(Res.string.share_note_no_longer_available))
                    return@launch
                }

                newBlocks.filterIsInstance<BookmarkBlock>().forEach { scheduleLinkMetadataRefresh(noteId, it) }
                _navigateToNoteId.value = noteId
            } catch (e: Exception) {
                e.printStackTrace()
                notify(getString(Res.string.share_failed_to_save))
            }
        }
    }

    fun createNoteAndSave(title: String) {
        val share = _currentShare.value ?: return
        _currentShare.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newBlocks = buildBlocks(share)
                if (newBlocks.isEmpty()) {
                    notify(getString(Res.string.share_failed_to_save))
                    return@launch
                }

                val newNoteId = UUID.randomUUID().toString()
                val metadata = NoteMetadataEntity(
                    noteId = newNoteId, title = title, icon = null, folderId = null,
                    isDaily = false, dateString = null, createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(), filePath = "note_$newNoteId.json", snippet = ""
                )
                SyncCoordinator.mutex.withLock {
                    repository.saveNote(metadata, NoteContent(blocks = newBlocks))
                }

                newBlocks.filterIsInstance<BookmarkBlock>().forEach { scheduleLinkMetadataRefresh(newNoteId, it) }
                _navigateToNoteId.value = newNoteId
            } catch (e: Exception) {
                e.printStackTrace()
                notify(getString(Res.string.share_failed_to_save))
            }
        }
    }

    fun dismiss() {
        _currentShare.value = null
    }

    fun clearNavigation() {
        _navigateToNoteId.value = null
    }
}
