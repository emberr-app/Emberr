package com.emberr.domain.backup.manual

import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.ACCOUNT_TASKS_INBOX_NOTE_ID
import com.emberr.data.local.room.entity.MediaReferenceEntity
import com.emberr.data.local.room.inOneTransaction
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.generateDailyNoteSnippet
import com.emberr.domain.selfhost.media.MediaReferenceScanner
import com.emberr.domain.util.media.mediaFileNameOnly
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val exportBlockJson = Json { ignoreUnknownKeys = true }

internal const val PINNED_BLOCKS_DATE_STRING = "global_pinned"

suspend fun AppDatabase.keepOnlySpacesForExport(
    spaceIdsToKeep: Set<String>,
    keepDailyNotes: Boolean,
    keepCalendar: Boolean
): Set<String> {
    val dao = backupDao()
    val spaceIds = spaceIdsToKeep.toList()
    val spaceIdsWithAccount = (spaceIdsToKeep + ACCOUNT_SPACE_ID).toList()
    val dailyNoteIdsThatLostBlocks = HashSet<String>()

    inOneTransaction {
        dao.deleteNotesOutsideSpacesOrInTrash(spaceIdsWithAccount)
        val peopleNoteIds = noteIdsOfPeopleAndTheirSubNotes(
            EmberrBackupData(
                exportTimestamp = 0L,
                notes = dao.getNotesInSpace(ACCOUNT_SPACE_ID),
                blocks = dao.getBlocksInSpace(ACCOUNT_SPACE_ID)
            )
        ).toList()
        if (!keepDailyNotes) {
            val noteIdsToKeep = listOf(ACCOUNT_TASKS_INBOX_NOTE_ID) + if (keepCalendar) peopleNoteIds else emptyList()
            dao.deleteNotesInSpaceThatAreNotDailyExcept(ACCOUNT_SPACE_ID, noteIdsToKeep)
            dao.deleteDailyNote(ACCOUNT_SPACE_ID, PINNED_BLOCKS_DATE_STRING)
        }
        if (!keepCalendar) {
            dao.deleteNote(ACCOUNT_TASKS_INBOX_NOTE_ID)
            dao.deleteNotes(peopleNoteIds)
        }
        if (!keepDailyNotes || !keepCalendar) {
            val dailyBlocksToDrop = dao.getBlocksOfDatedDailyNotes(ACCOUNT_SPACE_ID, PINNED_BLOCKS_DATE_STRING).filter { block ->
                val isCalendarBlock = decodeBlockOrNull(block.blockDataJson) is CheckboxBlock
                if (isCalendarBlock) !keepCalendar else !keepDailyNotes
            }
            dao.deleteBlocks(dailyBlocksToDrop)
            dailyBlocksToDrop.mapTo(dailyNoteIdsThatLostBlocks) { it.noteId }
        }
        dao.deleteBlocksThatAreDeletedOrHaveNoNote()
        dao.deleteDailyNotesWithoutBlocks(ACCOUNT_SPACE_ID)
        rebuildSnippetAndMediaReferencesOf(dailyNoteIdsThatLostBlocks)
        dao.deleteCanvasNodesThatAreDeletedOrHaveNoNote()
        dao.deleteCanvasEdgesThatAreDeletedOrHaveNoNote()
        dao.deleteCanvasStrokesThatAreDeletedOrHaveNoNote()
        dao.deleteCalendarTasksThatHaveNoBlock()
        dao.deleteCalendarEventExceptionsThatHaveNoBlock()
        dao.deleteImageBlocksThatHaveNoBlock()
        dao.deleteDocumentBlocksThatHaveNoBlock()
        dao.deleteBookmarkBlocksThatHaveNoBlock()
        dao.deleteMovieBlocksThatHaveNoBlock()
        dao.deleteMediaReferencesThatHaveNoNote()
        dao.deleteSpacesNotKept(spaceIds)
        dao.deleteFoldersOutsideSpacesOrDeleted(spaceIds)
        dao.deleteDeletedCategories()
        dao.deletePropertyTagsOutsideSpacesOrDeleted(spaceIds)
        dao.deleteCustomPropertiesOutsideSpacesOrDeleted(spaceIds)
        dao.deleteAllChatSessions()
        dao.deleteAllNoteTombstones()
        dao.deleteAllApiConfigTombstones()
        dao.deleteAllUnappliedSyncChanges()
        dao.deleteAllUnusedMediaFiles()
        dao.deleteAllCloudSpaces()
        dao.deleteAllCloudItems()
        dao.deleteAllCloudMediaLocks()
        dao.deleteAllBlockMergeBases()
    }
    useWriterConnection { connection -> connection.execSQL("VACUUM") }

    return mediaFileNamesStillInUse()
}

private suspend fun AppDatabase.rebuildSnippetAndMediaReferencesOf(noteIds: Set<String>) {
    for (noteId in noteIds) {
        val blocks = backupDao().getBlocksOfNote(noteId).mapNotNull { decodeBlockOrNull(it.blockDataJson) }
        backupDao().updateSnippet(noteId, generateDailyNoteSnippet(blocks))
        mediaReferenceDao().deleteByNoteId(noteId)
        mediaReferenceDao().upsertReferences(
            MediaReferenceScanner.extractMediaFileNames(blocks).map { fileName -> MediaReferenceEntity(noteId = noteId, fileName = fileName) }
        )
    }
}

private suspend fun AppDatabase.mediaFileNamesStillInUse(): Set<String> =
    mediaFileNamesUsedBy(
        blocks = backupDao().getAllBlockDataJson().mapNotNull(::decodeBlockOrNull),
        coverImagePaths = noteDao().getAllCoverImagePaths().filterNotNull(),
        canvasImagePaths = canvasDao().getAllLiveImagePaths()
    )

internal fun mediaFileNamesUsedBy(
    blocks: List<NoteBlock>,
    coverImagePaths: List<String>,
    canvasImagePaths: List<String>
): Set<String> {
    val mediaPaths = MediaReferenceScanner.extractMediaFileNames(blocks) + coverImagePaths + canvasImagePaths
    return mediaPaths.map(::mediaFileNameOnly).filter { it.isNotBlank() }.toSet()
}

internal fun decodeBlockOrNull(blockDataJson: String): NoteBlock? =
    try {
        exportBlockJson.decodeFromString(NoteBlockSerializer, blockDataJson)
    } catch (_: SerializationException) {
        null
    }
