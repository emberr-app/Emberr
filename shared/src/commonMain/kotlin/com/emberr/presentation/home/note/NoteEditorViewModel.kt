package com.emberr.presentation.home.note

import androidx.lifecycle.viewModelScope
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.personNamesLinkedIn
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.withDatabasesAsTables
import com.emberr.domain.model.*
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.movie.withWatchlistsExpanded
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.table.DeviceTableLayoutStore
import com.emberr.domain.util.export.MarkdownAttachment
import com.emberr.domain.util.export.ExportedCanvas
import com.emberr.domain.util.export.MarkdownAttachments
import com.emberr.domain.util.export.MarkdownCanvasFile
import com.emberr.domain.util.export.MarkdownExport
import com.emberr.domain.util.voice.AudioRecorder
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.WebImageFetcher
import com.emberr.domain.util.sync.SyncCoordinator
import com.emberr.presentation.shared.components.couldNotMoveToTrashMessage
import com.emberr.presentation.shared.components.movedToTrashMessage
import com.emberr.domain.util.system.showNativeToast
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.presentation.shared.editor.BaseEditorViewModel
import com.emberr.presentation.shared.editor.ImageLinkFetch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.notifications_note
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModel(
    repository: NoteRepository,
    mediaStorageHelper: MediaStorageHelper,
    webImageFetcher: WebImageFetcher,
    reminderScheduler: ReminderScheduler,
    audioRecorder: AudioRecorder,
    appScope: CoroutineScope,
    deviceTableLayoutStore: DeviceTableLayoutStore,
    movieTitleSource: MovieTitleSource,
    peopleRepository: PeopleRepository,
    private val canvasRepository: CanvasRepository
) : BaseEditorViewModel(repository, mediaStorageHelper, webImageFetcher, reminderScheduler, audioRecorder, appScope, deviceTableLayoutStore, movieTitleSource, peopleRepository) {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val wordCount: StateFlow<Int> = _blocks.map { blocks ->
        blocks.sumOf { block ->
            val text = extractTextFromBlock(block) ?: ""
            if (text.isBlank()) 0 else text.trim().split("\\s+".toRegex()).size
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _noteTitle = MutableStateFlow("")
    val noteTitle: StateFlow<String> = _noteTitle.asStateFlow()

    private val _noteIcon = MutableStateFlow<String?>(null)
    val noteIcon: StateFlow<String?> = _noteIcon.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _coverImagePath = MutableStateFlow<String?>(null)
    val coverImagePath: StateFlow<String?> = _coverImagePath.asStateFlow()

    private val _coverLinkFetch = MutableStateFlow<ImageLinkFetch?>(null)
    val coverLinkFetch: StateFlow<ImageLinkFetch?> = _coverLinkFetch.asStateFlow()

    // True when the currently loaded note is a template - drives the "Editing Template" pill
    // in NoteScreen's top bar so it's obvious this note won't show up in the regular notes list.
    private val _isTemplate = MutableStateFlow(false)
    val isTemplate: StateFlow<Boolean> = _isTemplate.asStateFlow()

    private val _noteUpdatedAt = MutableStateFlow(0L)

    private var currentMetadata: NoteMetadataEntity? = null
    private val _currentlyLoadedNoteId = MutableStateFlow<String?>(null)
    private var currentlyLoadedNoteId: String?
        get() = _currentlyLoadedNoteId.value
        set(value) { _currentlyLoadedNoteId.value = value }

    private val _showWordCount = MutableStateFlow(false)
    val showWordCount: StateFlow<Boolean> = _showWordCount.asStateFlow()

    init {
        viewModelScope.launch {
            com.emberr.domain.util.sync.SyncEventBus.events.collect { event ->
                if (event !is com.emberr.domain.util.sync.NoteSyncEvent.NoteChanged) return@collect
                val syncedEntityId = event.entityId
                val currentId = currentMetadata?.noteId
                if (currentId != null && syncedEntityId == currentId) {
                    if (autosaveJob?.isActive == true || isWithinLocalMutationCooldown()) {
                        refreshFromDiskOncePaused { reloadNoteFromDisk(currentId) }
                        return@collect
                    }
                    reloadNoteFromDisk(currentId)
                }
            }
        }
        viewModelScope.launch {
            _currentlyLoadedNoteId
                .filterNotNull()
                .flatMapLatest { noteId -> repository.observeNoteContent(noteId) }
                .filterNotNull()
                .collect { freshContent ->
                    if (_isLoading.value) return@collect
                    if (autosaveJob?.isActive == true || isWithinLocalMutationCooldown()) {
                        currentlyLoadedNoteId?.let { noteId -> refreshFromDiskOncePaused { reloadNoteFromDisk(noteId) } }
                        return@collect
                    }

                    val final = recalculateNumberedLists(
                        preserveNewerLocalBlocks(freshContent.blocks).withSomewhereToType()
                    )
                    if (final != _blocks.value) {
                        _blocks.value = final
                    }
                }
        }
        viewModelScope.launch {
            _currentlyLoadedNoteId
                .filterNotNull()
                .flatMapLatest { noteId -> repository.observeNoteMetadata(noteId) }
                .filterNotNull()
                .collect { storedMetadata ->
                    if (_isLoading.value) return@collect
                    adoptMetadataChangedOutsideEditor(storedMetadata)
                }
        }
    }

    private fun List<NoteBlock>.withSomewhereToType(): List<NoteBlock> =
        if (any { block -> block !is PersonDetailBlock }) this
        else this + (
            _blocks.value.firstOrNull { block -> block is TextBlock && block.text.isEmpty() }
                ?: TextBlock(id = java.util.UUID.randomUUID().toString(), text = "")
        )

    private fun adoptMetadataChangedOutsideEditor(storedMetadata: NoteMetadataEntity) {
        val knownMetadata = currentMetadata ?: return
        if (storedMetadata.noteId != knownMetadata.noteId) return
        if (storedMetadata.updatedAt <= knownMetadata.updatedAt) return

        if (_noteTitle.value == knownMetadata.title) _noteTitle.value = storedMetadata.title
        if (_noteIcon.value == knownMetadata.icon) _noteIcon.value = storedMetadata.icon
        if (_isFavorite.value == knownMetadata.isFavorite) _isFavorite.value = storedMetadata.isFavorite
        if (_coverImagePath.value == knownMetadata.coverImagePath) _coverImagePath.value = storedMetadata.coverImagePath
        if (_showWordCount.value == knownMetadata.showWordCount) _showWordCount.value = storedMetadata.showWordCount
        _isTemplate.value = storedMetadata.isTemplate
        _noteUpdatedAt.value = storedMetadata.updatedAt
        currentMetadata = storedMetadata
    }

    private suspend fun reloadNoteFromDisk(noteId: String) {
        if (currentMetadata?.noteId != noteId) return
        val updatedMeta = withContext(Dispatchers.IO) { repository.getNoteById(noteId) }
        if (updatedMeta != null) {
            currentMetadata = updatedMeta
            _noteTitle.value = updatedMeta.title
            _noteIcon.value = updatedMeta.icon
            _isFavorite.value = updatedMeta.isFavorite
            _coverImagePath.value = updatedMeta.coverImagePath
            _showWordCount.value = updatedMeta.showWordCount
            _noteUpdatedAt.value = updatedMeta.updatedAt
            _isTemplate.value = updatedMeta.isTemplate
        }

        val content = withContext(Dispatchers.IO) { repository.getNoteContent(noteId) }
        val newBlocks = preserveNewerLocalBlocks(content?.blocks ?: emptyList())
        val resolved = recalculateNumberedLists(
            newBlocks.withSomewhereToType()
        )
        if (resolved != _blocks.value) {
            _blocks.value = resolved
        }
    }

    private suspend fun reconcileWithDisk(noteId: String, snapshot: List<NoteBlock>): List<NoteBlock> =
        editorDiskReconciler.reconcile(snapshot, diskBlocksOf(noteId))

    private suspend fun diskBlocksOf(noteId: String): List<NoteBlock> =
        repository.getNoteContent(noteId)?.blocks ?: emptyList()

    private suspend fun theNoteAlreadyHoldsThis(
        noteId: String,
        metadataToSave: NoteMetadataEntity,
        blocksToSave: List<NoteBlock>
    ): Boolean {
        val storedMetadata = repository.getNoteById(noteId) ?: return false
        if (blocksToSave != repository.getNoteContent(noteId)?.blocks.orEmpty()) return false

        return metadataToSave.copy(
            updatedAt = storedMetadata.updatedAt,
            selfHostSyncedAt = storedMetadata.selfHostSyncedAt,
            cloudSyncedAt = storedMetadata.cloudSyncedAt
        ) == storedMetadata
    }

    override suspend fun performSave(): Boolean {
        if (_isLoading.value) return false
        val meta = currentMetadata ?: return false

        return try {
            withContext(Dispatchers.IO) {
                SyncCoordinator.mutex.withLock {
                    // Re-validate now that the lock is actually held - acquiring it can be delayed for
                    // as long as a self-host sync pass holds the same mutex, and _blocks always tracks
                    // whichever note is CURRENTLY loaded, not meta.noteId. Without this check, a save
                    // queued behind a long lock wait would write whatever note the user has since
                    // switched to into meta's note instead of its own - cross-note contamination.
                    if (currentMetadata?.noteId != meta.noteId) return@withLock false
                    if (repository.getNoteById(meta.noteId) == null) return@withLock false

                    val diskBlocks = diskBlocksOf(meta.noteId)
                    val reconciled = _blocks.updateAndGet { editorBlocks -> editorDiskReconciler.reconcile(editorBlocks, diskBlocks) }

                    val updatedMeta = meta.withDetailsFromEditor().copy(
                        snippet = generateSnippet(reconciled),
                        updatedAt = System.currentTimeMillis()
                    )

                    if (theNoteAlreadyHoldsThis(meta.noteId, updatedMeta, reconciled)) return@withLock true

                    currentMetadata = updatedMeta
                    _noteUpdatedAt.value = updatedMeta.updatedAt

                    repository.saveNote(updatedMeta, NoteContent(blocks = reconciled))
                    true
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun performIndexing() {
        if (_isLoading.value) return
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        withContext(Dispatchers.IO) {
            repository.indexNote(
                meta,
                NoteContent(blocks = snapshot)
            )
        }
    }

    override suspend fun getNoteTitleForReminder(): String {
        return _noteTitle.value.ifBlank { getString(Res.string.notifications_note) }
    }

    override suspend fun spaceIdOfThisNote(): String? {
        val noteId = currentMetadata?.noteId ?: return null
        return repository.getNoteById(noteId)?.spaceId
    }

    fun toggleWordCount() {
        val previous = _showWordCount.value
        _showWordCount.value = !previous
        viewModelScope.launch { if (!performSave()) _showWordCount.value = previous }
    }

    fun loadNote(noteId: String) {
        if (currentlyLoadedNoteId == noteId) return
        currentlyLoadedNoteId = noteId
        clearUndoHistory()

        com.emberr.domain.util.eventbus.AiEventBus.activeNoteId = noteId

        autosaveJob?.cancel()
        indexingJob?.cancel()

        val previousMeta = currentMetadata
        val snapshot = _blocks.value.toList()

        val flushedMeta = previousMeta?.withDetailsFromEditor()?.copy(
            snippet = generateSnippet(snapshot),
            updatedAt = System.currentTimeMillis()
        )

        clearSelection()

        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            // Any exception below (a repository/network call throwing) must never leave _isLoading
            // stuck true - the reactive observeNoteContent collector above drops every emission while
            // it's true, so a stuck flag silently stops this note from ever picking up a background
            // sync update again until the ViewModel itself is recreated (a full app restart).
            try {
                if (flushedMeta != null && repository.getNoteById(previousMeta.noteId) != null) {
                    SyncCoordinator.mutex.withLock {
                        val reconciled = reconcileWithDisk(previousMeta.noteId, snapshot)
                        if (!theNoteAlreadyHoldsThis(previousMeta.noteId, flushedMeta, reconciled)) {
                            val contentToSave = NoteContent(blocks = reconciled)
                            repository.saveNote(flushedMeta, contentToSave)
                            repository.indexNote(flushedMeta, contentToSave)
                        }
                    }
                }
                editorDiskReconciler.forgetBlocksShown()

                currentMetadata = repository.getNoteById(noteId)

                if (currentMetadata != null) {
                    _noteTitle.value = currentMetadata?.title ?: ""
                    _noteIcon.value = currentMetadata?.icon
                    _isFavorite.value = currentMetadata?.isFavorite ?: false
                    _coverImagePath.value = currentMetadata?.coverImagePath
                    _showWordCount.value = currentMetadata?.showWordCount ?: false
                    _noteUpdatedAt.value = currentMetadata?.updatedAt ?: 0L
                    _isTemplate.value = currentMetadata?.isTemplate ?: false

                    val content = repository.getNoteContent(noteId)
                    val existingBlocks = content?.blocks ?: emptyList()

                    _blocks.value = recalculateNumberedLists(
                        existingBlocks.withSomewhereToType()
                    )
                }
                isAiIndexDirty = false
                lastIndexedContentHash = 0
            } finally {
                _isLoading.value = false
            }

            // The reactive observeNoteContent collector drops every cache emission while _isLoading
            // was true above - if a background sync refreshed this exact note during that window, that
            // update is gone for good (StateFlow doesn't redeliver a value a collector already saw).
            // Re-read once now that the guard is open, so a sync landing mid-load isn't silently lost.
            val latestContent = repository.getNoteContent(noteId)
            if (latestContent != null) {
                val resolved = recalculateNumberedLists(
                    latestContent.blocks.withSomewhereToType()
                )
                if (resolved != _blocks.value) {
                    _blocks.value = resolved
                }
            }
        }
    }

    private fun NoteMetadataEntity.withDetailsFromEditor(): NoteMetadataEntity {
        val now = System.currentTimeMillis()
        val withTitle = if (_noteTitle.value == title) this else copy(title = _noteTitle.value, titleUpdatedAt = now)
        val withIcon = if (_noteIcon.value == icon) withTitle else withTitle.copy(icon = _noteIcon.value, iconUpdatedAt = now)
        val withFavorite = if (_isFavorite.value == isFavorite) withIcon else withIcon.copy(isFavorite = _isFavorite.value, favoriteUpdatedAt = now)
        val withCoverImage = if (_coverImagePath.value == coverImagePath) withFavorite else {
            withFavorite.copy(coverImagePath = _coverImagePath.value, coverImageUpdatedAt = now)
        }
        return if (_showWordCount.value == showWordCount) withCoverImage else {
            withCoverImage.copy(showWordCount = _showWordCount.value, wordCountUpdatedAt = now)
        }
    }

    fun updateTitle(newTitle: String) {
        _noteTitle.value = newTitle
        scheduleAutosave()
    }

    fun updateIcon(newIcon: String?) {
        val previous = _noteIcon.value
        _noteIcon.value = newIcon
        viewModelScope.launch { if (!performSave()) _noteIcon.value = previous }
    }

    fun toggleFavorite() {
        val previous = _isFavorite.value
        _isFavorite.value = !previous
        viewModelScope.launch { if (!performSave()) _isFavorite.value = previous }
    }

    fun handleCoverImagePicked(uriString: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaInfo = mediaStorageHelper.copyUriToInternalStorage(uriString)
            if (mediaInfo != null) {
                val previous = _coverImagePath.value
                _coverImagePath.value = mediaInfo.localFileName
                if (!performSave()) _coverImagePath.value = previous
            }
        }
    }

    fun handleCoverLinkSubmitted(address: String) {
        if (_coverLinkFetch.value?.hasFailed == false) return
        _coverLinkFetch.value = ImageLinkFetch(address, hasFailed = false)
        viewModelScope.launch {
            val mediaInfo = webImageFetcher.fetchIntoMedia(address)
            if (mediaInfo == null) {
                _coverLinkFetch.value = ImageLinkFetch(address, hasFailed = true)
                return@launch
            }
            val previous = _coverImagePath.value
            _coverImagePath.value = mediaInfo.localFileName
            if (!performSave()) _coverImagePath.value = previous
            _coverLinkFetch.value = null
        }
    }

    fun removeCoverImage() {
        val previous = _coverImagePath.value
        _coverImagePath.value = null
        viewModelScope.launch { if (!performSave()) _coverImagePath.value = previous }
    }

    fun moveToTrash(onMoved: () -> Unit) {
        val meta = currentMetadata ?: return
        val snapshot = _blocks.value.toList()

        viewModelScope.launch(Dispatchers.IO) {
            val trashedTitle = try {
                SyncCoordinator.mutex.withLock {
                    val reconciled = reconcileWithDisk(meta.noteId, snapshot)
                    val trashedMeta = meta.withDetailsFromEditor().copy(
                        snippet = generateSnippet(reconciled),
                        trashedAt = System.currentTimeMillis()
                    )

                    val content = NoteContent(blocks = reconciled)

                    repository.saveNote(trashedMeta, content)
                    repository.indexNote(trashedMeta, content)

                    currentMetadata = trashedMeta
                    trashedMeta.title
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            withContext(Dispatchers.Main) {
                if (trashedTitle == null) {
                    showNativeToast(couldNotMoveToTrashMessage())
                } else {
                    showNativeToast(movedToTrashMessage(trashedTitle))
                    onMoved()
                }
            }
        }
    }

    suspend fun generatePlainTextExport(): String {
        val title = _noteTitle.value.ifBlank { "Untitled Note" }
        val blocks = blocksForExport()
        val body = com.emberr.domain.util.export.ExportEngine.generatePlainText(
            blocks = blocks,
            personNamesById = personNamesLinkedIn(blocks, repository::getNoteById)
        )
        return "$title\n\n$body"
    }

    suspend fun blocksForExport(): List<NoteBlock> = _blocks.value.withDatabasesAsTables(repository).withWatchlistsExpanded(repository)

    suspend fun generateMarkdownExport(): String = markdownOf(blocksForExport(), attachments = emptyList(), canvasFiles = emptyList())

    suspend fun generateMarkdownFileExport(baseFileName: String): MarkdownExport {
        val blocks = blocksForExport()
        val canvases = canvasesIn(blocks)
        val attachments = MarkdownAttachments.collect(blocks, canvases)
        val canvasFiles = MarkdownAttachments.canvasFilesFor(canvases, attachments)
        return MarkdownExport(baseFileName, markdownOf(blocks, attachments, canvasFiles), attachments, canvasFiles)
    }

    private suspend fun canvasesIn(blocks: List<NoteBlock>): List<ExportedCanvas> =
        blocks.filterIsInstance<CanvasBlock>()
            .filter { !it.isDeleted }
            .map { it.canvasNoteId }
            .distinct()
            .mapNotNull { canvasNoteId ->
                val canvasNote = repository.getNoteById(canvasNoteId) ?: return@mapNotNull null
                ExportedCanvas(canvasNoteId, canvasNote.title, canvasRepository.loadCanvasIncludingDeleted(canvasNoteId))
            }

    private suspend fun markdownOf(
        blocks: List<NoteBlock>,
        attachments: List<MarkdownAttachment>,
        canvasFiles: List<MarkdownCanvasFile>
    ): String {
        val title = _noteTitle.value.ifBlank { "Untitled Note" }
        val categoryNamesById = repository.getAllCategories().first()
            .filter { !it.isDeleted }
            .associate { it.categoryId to it.name }
        val body = com.emberr.domain.util.export.ExportEngine.generateMarkdown(
            blocks = blocks,
            noteTitlesById = linkedNoteTitlesIn(blocks),
            categoryNamesById = categoryNamesById,
            attachmentPathsByStoredName = attachments.associate { it.storedFileName to it.pathInExport },
            personNamesById = personNamesLinkedIn(blocks, repository::getNoteById),
            canvasPathsByNoteId = canvasFiles.associate { it.canvasNoteId to it.pathInExport }
        )
        return "# $title\n\n$body"
    }

    private suspend fun linkedNoteTitlesIn(blocks: List<NoteBlock>): Map<String, String> =
        blocks.filterIsInstance<LinkedNoteBlock>()
            .map { it.linkedNoteId }
            .distinct()
            .mapNotNull { noteId -> repository.getNoteById(noteId)?.let { note -> noteId to note.title.ifBlank { "Untitled" } } }
            .toMap()
}

