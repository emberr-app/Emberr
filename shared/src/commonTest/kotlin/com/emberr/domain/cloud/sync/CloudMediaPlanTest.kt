package com.emberr.domain.cloud.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class CloudMediaPlanTest {

    private val mediaIdsByFileName = mapOf(
        "photo.jpg" to "media_photo",
        "old.pdf" to "media_old",
        "voice.m4a" to "media_voice"
    )

    @Test
    fun addsMissingLocksRemovesUnusedOnesOnlyForSentNotesAndUploadsASharedFileOnce() {
        val sentNote = CloudNoteMedia(spaceRef = 1L, noteId = "note-sent", fileNames = setOf("photo.jpg"), hasUnsentChanges = false)
        val editedNote = CloudNoteMedia(spaceRef = 2L, noteId = "note-edited", fileNames = setOf("photo.jpg"), hasUnsentChanges = true)
        val liveLocks = setOf(
            CloudMediaLockPlace(1L, "media_old", "note-sent"),
            CloudMediaLockPlace(2L, "media_voice", "note-edited")
        )

        val plan = planCloudMedia(
            notes = listOf(sentNote, editedNote),
            mediaIdsByFileName = mediaIdsByFileName,
            liveLocks = liveLocks,
            fileNamesOnDisk = setOf("photo.jpg")
        )

        assertEquals(listOf("photo.jpg"), plan.fileNamesToUpload)
        assertEquals(
            listOf(CloudMediaLockPlace(1L, "media_photo", "note-sent"), CloudMediaLockPlace(2L, "media_photo", "note-edited")),
            plan.locksToAdd.map { it.place }
        )
        assertEquals(listOf(CloudMediaLockPlace(1L, "media_old", "note-sent")), plan.locksToRemove)
    }

    @Test
    fun aFileLockedInAnySpaceIsNotUploadedAndAFileMissingOnDiskIsDownloaded() {
        val note = CloudNoteMedia(spaceRef = 1L, noteId = "note-1", fileNames = setOf("photo.jpg", "voice.m4a"), hasUnsentChanges = false)

        val plan = planCloudMedia(
            notes = listOf(note),
            mediaIdsByFileName = mediaIdsByFileName,
            liveLocks = setOf(CloudMediaLockPlace(9L, "media_photo", "note-elsewhere")),
            fileNamesOnDisk = setOf("photo.jpg")
        )

        assertEquals(emptyList<String>(), plan.fileNamesToUpload)
        assertEquals(listOf("voice.m4a"), plan.fileNamesToDownload)
    }
}
