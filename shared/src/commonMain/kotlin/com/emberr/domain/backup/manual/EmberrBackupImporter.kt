package com.emberr.domain.backup.manual

import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.inOneTransaction
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.model.NoteContent
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.sync.AutoSyncTrigger
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.sync.SyncCoordinator
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
    val missingMediaCount: Int
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

    fun startImport(backupFile: EmberrBackupFile, spaceIds: Set<String>) {
        _progress.value = EmberrImportProgress.CopyingMedia(copiedCount = 0, totalCount = 0)
        importJob = appScope.launch {
            var savedData: EmberrBackupData? = null
            val finalProgress = try {
                val result = backupFile.use { importSpaces(it, spaceIds, onSaved = { saved -> savedData = saved }) }
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
                savedData?.let { indexForAi(it) }
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
        spaceIds: Set<String>,
        onSaved: (EmberrBackupData) -> Unit = {}
    ): EmberrImportResult {
        val prepared = prepareSpacesForImport(backupFile.backupData, spaceIds, System.currentTimeMillis()) { Uuid.random().toString() }
        val mediaFileNames = mediaFileNamesUsedBy(
            blocks = prepared.blocks.mapNotNull { decodeBlockOrNull(it.blockDataJson) },
            coverImagePaths = prepared.notes.mapNotNull { it.coverImagePath },
            canvasImagePaths = prepared.canvasNodes.mapNotNull { it.imagePath }
        ).sorted()

        val newlyCopiedFileNames = mutableListOf<String>()
        var missingMediaCount = 0
        try {
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
                appDatabase.inOneTransaction { save(prepared) }
            }
        } catch (error: Throwable) {
            withContext(NonCancellable) {
                newlyCopiedFileNames.forEach { fileName -> mediaStorageHelper.deleteMediaFile(fileName) }
            }
            throw error
        }

        AutoSyncTrigger.requestSync()
        onSaved(prepared)
        return EmberrImportResult(
            importedSpaceNames = prepared.spaces.map { it.displayName },
            missingMediaCount = missingMediaCount
        )
    }

    private fun showProgress(progress: EmberrImportProgress) {
        _progress.update { current -> if (current == EmberrImportProgress.Stopping) current else progress }
    }

    private suspend fun save(prepared: EmberrBackupData) {
        val dao = appDatabase.backupDao()
        dao.insertSpaces(prepared.spaces)
        dao.insertFolders(prepared.folders)
        dao.insertCategories(prepared.categories)
        dao.insertPropertyTags(prepared.propertyTags)
        dao.insertCustomProperties(prepared.customProperties)
        dao.insertNotes(prepared.notes)
        dao.insertBlocks(prepared.blocks)
        dao.insertCanvasNodes(prepared.canvasNodes)
        dao.insertCanvasEdges(prepared.canvasEdges)
        dao.insertCanvasStrokes(prepared.canvasStrokes)
        dao.insertCalendarEventExceptions(prepared.calendarEventExceptions)

        val blocksByNoteId = prepared.blocks.groupBy { it.noteId }
        prepared.notes.forEach { note ->
            val blocks = blocksByNoteId[note.noteId].orEmpty()
                .sortedBy { it.displayOrder }
                .mapNotNull { decodeBlockOrNull(it.blockDataJson) }
            repository.refreshProjectionsForNote(note, blocks)
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
