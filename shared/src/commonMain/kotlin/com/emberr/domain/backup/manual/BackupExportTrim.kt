package com.emberr.domain.backup.manual

import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.ACCOUNT_TASKS_INBOX_NOTE_ID
import com.emberr.data.local.room.inOneTransaction
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
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

    inOneTransaction {
        dao.deleteNotesOutsideSpacesOrInTrash(spaceIdsWithAccount)
        if (!keepDailyNotes) {
            dao.deleteNotesInSpaceThatAreNotDailyExcept(ACCOUNT_SPACE_ID, noteIdToKeep = ACCOUNT_TASKS_INBOX_NOTE_ID)
            dao.deleteDailyNote(ACCOUNT_SPACE_ID, PINNED_BLOCKS_DATE_STRING)
        }
        if (!keepCalendar) dao.deleteNote(ACCOUNT_TASKS_INBOX_NOTE_ID)
        if (!keepDailyNotes || !keepCalendar) {
            val dailyBlocksToDrop = dao.getBlocksOfDatedDailyNotes(ACCOUNT_SPACE_ID, PINNED_BLOCKS_DATE_STRING).filter { block ->
                val isCalendarBlock = decodeBlockOrNull(block.blockDataJson) is CheckboxBlock
                if (isCalendarBlock) !keepCalendar else !keepDailyNotes
            }
            dao.deleteBlocks(dailyBlocksToDrop)
        }
        dao.deleteBlocksThatAreDeletedOrHaveNoNote()
        dao.deleteDailyNotesWithoutBlocks(ACCOUNT_SPACE_ID)
        dao.deleteCanvasNodesThatAreDeletedOrHaveNoNote()
        dao.deleteCanvasEdgesThatAreDeletedOrHaveNoNote()
        dao.deleteCanvasStrokesThatAreDeletedOrHaveNoNote()
        dao.deleteCalendarTasksThatHaveNoBlock()
        dao.deleteCalendarEventExceptionsThatHaveNoBlock()
        dao.deleteImageBlocksThatHaveNoNote()
        dao.deleteDocumentBlocksThatHaveNoNote()
        dao.deleteBookmarkBlocksThatHaveNoNote()
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
    }
    useWriterConnection { connection -> connection.execSQL("VACUUM") }

    return mediaFileNamesStillInUse()
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
