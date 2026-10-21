package com.emberr.domain.backup.manual

import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.inOneTransaction
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.model.NoteContent
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.sync.AutoSyncTrigger
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.sync.SyncCoordinator
import com.emberr.domain.util.sync.SyncEventBus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class BackupMediaCopyOutcome { COPIED, ALREADY_ON_DEVICE, NOT_IN_BACKUP }

interface EmberrBackupFile : AutoCloseable {
    val pickedName: String
    val backupData: EmberrBackupData
    suspend fun copyMediaFileIfMissing(fileName: String): BackupMediaCopyOutcome
}

interface EmberrBackupFileReader {
    suspend fun readBackupFile(location: String): EmberrBackupFile
}

sealed interface EmberrImportProgress {
    data object NotStarted : EmberrImportProgress
    data class CopyingMedia(val copiedCount: Int, val totalCount: Int) : EmberrImportProgress
    data object Saving : EmberrImportProgress
    data object Stopping : EmberrImportProgress
    data object Stopped : EmberrImportProgress
    data class Finished(val result: EmberrImportResult) : EmberrImportProgress
    data class Failed(val reason: String) : EmberrImportProgress
}

data class EmberrImportResult(
    val importedSpaceNames: List<String>,
    val missingMediaCount: Int,
    val addedDailyNotes: Boolean = false,
    val addedCalendar: Boolean = false
)

class SavedBackupImport(
    val addedNotes: EmberrBackupData,
    val mergedAccountNotes: List<NoteMetadataEntity>
)

class EmberrBackupImporter(
    private val appDatabase: AppDatabase,
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val reminderRescheduler: ReminderRescheduler,
    private val appScope: CoroutineScope
) {

    private val _progress = MutableStateFlow<EmberrImportProgress>(EmberrImportProgress.NotStarted)
    val progress: StateFlow<EmberrImportProgress> = _progress.asStateFlow()

    private var importJob: Job? = null

    fun startImport(backupFile: EmberrBackupFile, choice: BackupContentChoice) {
        _progress.value = EmberrImportProgress.CopyingMedia(copiedCount = 0, totalCount = 0)
        importJob = appScope.launch {
            var savedData: SavedBackupImport? = null
            val finalProgress = try {
                val result = backupFile.use { importSpaces(it, choice, onSaved = { saved -> savedData = saved }) }
                EmberrImportProgress.Finished(result)
            } catch (cancellation: CancellationException) {
                _progress.value = EmberrImportProgress.Stopped
                throw cancellation
            } catch (error: Throwable) {
                EmberrImportProgress.Failed(error.message ?: "Unknown error")
            }
            withContext(NonCancellable) {
                if (savedData != null) reminderRescheduler.rescheduleUpcomingReminders()
                _progress.value = finalProgress
                savedData?.let { saved ->
                    indexForAi(saved.addedNotes)
                    indexMergedAccountNotesForAi(saved.mergedAccountNotes)
                }
            }
        }
    }

    fun stopImport() {
        if (_progress.value !is EmberrImportProgress.CopyingMedia) return
        _progress.value = EmberrImportProgress.Stopping
        importJob?.cancel()
    }

    fun clearFinishedImport() {
        val progress = _progress.value
        if (progress is EmberrImportProgress.Finished || progress is EmberrImportProgress.Failed || progress == EmberrImportProgress.Stopped) {
            _progress.value = EmberrImportProgress.NotStarted
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun importSpaces(
        backupFile: EmberrBackupFile,
        choice: BackupContentChoice,
        onSaved: (SavedBackupImport) -> Unit = {}
    ): EmberrImportResult {
        val now = System.currentTimeMillis()
        val newId = { Uuid.random().toString() }
        val backup = backupFile.backupData
        val categories = resolveCategoriesForImport(
            backupCategories = backup.categories,
            localCategoriesIncludingDeleted = appDatabase.backupDao().getAllCategoriesIncludingDeleted(),
            now = now,
            newId = newId
        )
        val preparedBackup = prepareBackupForImport(backup, choice, categories.localCategoryIdByBackupId, now, newId)
        val prepared = preparedBackup.spaces
        val accountData = preparedBackup.accountData
        val mediaFileNames = mediaFileNamesUsedBy(
            blocks = (prepared.blocks + accountData.blocksToMerge + accountData.subNoteBlocks).mapNotNull { decodeBlockOrNull(it.blockDataJson) },
            coverImagePaths = (prepared.notes + accountData.notesToMerge + accountData.subNotes).mapNotNull { it.coverImagePath },
            canvasImagePaths = (prepared.canvasNodes + accountData.canvasNodes).mapNotNull { it.imagePath }
        ).sorted()

        val newlyCopiedFileNames = mutableListOf<String>()
        var missingMediaCount = 0
        val saved = try {
            mediaFileNames.forEachIndexed { copiedCount, fileName ->
                currentCoroutineContext().ensureActive()
                showProgress(EmberrImportProgress.CopyingMedia(copiedCount, mediaFileNames.size))
                when (backupFile.copyMediaFileIfMissing(fileName)) {
                    BackupMediaCopyOutcome.COPIED -> newlyCopiedFileNames += fileName
                    BackupMediaCopyOutcome.NOT_IN_BACKUP -> missingMediaCount++
                    BackupMediaCopyOutcome.ALREADY_ON_DEVICE -> Unit
                }
            }
            showProgress(EmberrImportProgress.Saving)
            SyncCoordinator.mutex.withLock {
                appDatabase.inOneTransaction {
                    appDatabase.backupDao().insertCategories(categories.newCategories)
                    save(prepared)
                    val addedSubNotes = saveAccountSubNotes(accountData)
                    val mergedAccountNotes = repository.mergeBackupNotesIntoAccountWhileSyncLockHeld(
                        accountData.notesToMerge,
                        accountData.blocksToMerge
                    )
                    appDatabase.backupDao().insertCalendarEventExceptions(accountData.calendarEventExceptions)
                    SavedBackupImport(
                        addedNotes = prepared.copy(
                            notes = prepared.notes + addedSubNotes.notes,
                            blocks = prepared.blocks + addedSubNotes.blocks,
                            canvasNodes = prepared.canvasNodes + addedSubNotes.canvasNodes,
                            canvasEdges = prepared.canvasEdges + addedSubNotes.canvasEdges,
                            canvasStrokes = prepared.canvasStrokes + addedSubNotes.canvasStrokes
                        ),
                        mergedAccountNotes = mergedAccountNotes
                    )
                }
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                repository.clearCaches()
                newlyCopiedFileNames.forEach { fileName -> mediaStorageHelper.deleteMediaFile(fileName) }
            }
            throw error
        }

        saved.mergedAccountNotes.forEach { note ->
            SyncEventBus.emitSyncCompleted(note.dateString?.takeIf { note.isDaily } ?: note.noteId, ACCOUNT_SPACE_ID)
        }
        AutoSyncTrigger.requestSync()
        onSaved(saved)
        return EmberrImportResult(
            importedSpaceNames = prepared.spaces.map { it.displayName },
            missingMediaCount = missingMediaCount,
            addedDailyNotes = choice.includesDailyNotes,
            addedCalendar = choice.includesCalendar
        )
    }

    private suspend fun saveAccountSubNotes(accountData: AccountDataToImport): EmberrBackupData {
        val addedSubNotes = EmberrBackupData(
            exportTimestamp = 0L,
            notes = accountData.subNotes,
            blocks = accountData.subNoteBlocks,
            canvasNodes = accountData.canvasNodes,
            canvasEdges = accountData.canvasEdges,
            canvasStrokes = accountData.canvasStrokes
        )
        val dao = appDatabase.backupDao()
        dao.insertNotes(addedSubNotes.notes)
        dao.insertBlocks(addedSubNotes.blocks)
        dao.insertCanvasNodes(addedSubNotes.canvasNodes)
        dao.insertCanvasEdges(addedSubNotes.canvasEdges)
        dao.insertCanvasStrokes(addedSubNotes.canvasStrokes)
        refreshProjectionsOf(addedSubNotes)
        return addedSubNotes
    }

    private fun showProgress(progress: EmberrImportProgress) {
        _progress.update { current -> if (current == EmberrImportProgress.Stopping) current else progress }
    }

    private suspend fun save(prepared: EmberrBackupData) {
        val dao = appDatabase.backupDao()
        dao.insertSpaces(prepared.spaces)
        dao.insertFolders(prepared.folders)
        dao.insertPropertyTags(prepared.propertyTags)
        dao.insertCustomProperties(prepared.customProperties)
        dao.insertNotes(prepared.notes)
        dao.insertBlocks(prepared.blocks)
        dao.insertCanvasNodes(prepared.canvasNodes)
        dao.insertCanvasEdges(prepared.canvasEdges)
        dao.insertCanvasStrokes(prepared.canvasStrokes)
        dao.insertCalendarEventExceptions(prepared.calendarEventExceptions)
        refreshProjectionsOf(prepared)
    }

    private suspend fun refreshProjectionsOf(prepared: EmberrBackupData) {
        val blocksByNoteId = prepared.blocks.groupBy { it.noteId }
        prepared.notes.forEach { note ->
            val blocks = blocksByNoteId[note.noteId].orEmpty()
                .sortedBy { it.displayOrder }
                .mapNotNull { decodeBlockOrNull(it.blockDataJson) }
            repository.refreshProjectionsForNote(note, blocks)
        }
    }

    private suspend fun indexMergedAccountNotesForAi(mergedNotes: List<NoteMetadataEntity>) {
        mergedNotes.forEach { note ->
            val dateString = note.dateString?.takeIf { note.isDaily }
            if (dateString == PINNED_BLOCKS_DATE_STRING) return@forEach
            if (dateString != null) {
                repository.getDailyNote(dateString)?.let { content -> repository.indexDailyNote(dateString, content, note) }
            } else {
                repository.getNoteContent(note.noteId)?.let { content -> repository.indexNote(note, content) }
            }
        }
    }

    private suspend fun indexForAi(saved: EmberrBackupData) {
        val blocksByNoteId = saved.blocks.groupBy { it.noteId }
        val canvasNodesByNoteId = saved.canvasNodes.groupBy { it.noteId }
        val canvasEdgesByNoteId = saved.canvasEdges.groupBy { it.noteId }
        val canvasStrokesByNoteId = saved.canvasStrokes.groupBy { it.noteId }
        saved.notes.forEach { note ->
            if (note.kind == NoteKind.CANVAS) {
                repository.indexCanvas(
                    note.noteId,
                    CanvasContent(
                        nodes = canvasNodesByNoteId[note.noteId].orEmpty(),
                        edges = canvasEdgesByNoteId[note.noteId].orEmpty(),
                        strokes = canvasStrokesByNoteId[note.noteId].orEmpty()
                    )
                )
            } else {
                val blocks = blocksByNoteId[note.noteId].orEmpty()
                    .sortedBy { it.displayOrder }
                    .mapNotNull { decodeBlockOrNull(it.blockDataJson) }
                repository.indexNote(note, NoteContent(blocks = blocks))
            }
        }
    }
}
