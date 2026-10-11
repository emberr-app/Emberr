package com.emberr.domain.backup.manual

import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.inOneTransaction
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.selfhost.media.MediaReferenceScanner
import com.emberr.domain.util.media.mediaFileNameOnly
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private val exportBlockJson = Json { ignoreUnknownKeys = true }

suspend fun AppDatabase.keepOnlySpacesForExport(spaceIdsToKeep: Set<String>): Set<String> {
    val dao = backupDao()
    val spaceIds = spaceIdsToKeep.toList()

    inOneTransaction {
        dao.deleteNotesOutsideSpacesOrInTrash(spaceIds)
        dao.deleteBlocksThatAreDeletedOrHaveNoNote()
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
        dao.deleteCategoriesOutsideSpacesOrDeleted(spaceIds)
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
