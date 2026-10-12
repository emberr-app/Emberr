package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.NoteMetadataEntity
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerEntriesToPullTest {

    private fun localNote(noteId: String, updatedAt: Long, selfHostSyncedAt: Long) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 100,
        updatedAt = updatedAt,
        filePath = "",
        selfHostSyncedAt = selfHostSyncedAt
    )

    @Test
    fun aDeletionOfANoteThatIsAlreadyGoneHereIsNotPulledAgain() {
        val oldDeletion = SelfHostManifestEntry(entryId = "groceries", entryType = SelfHostEntryType.NOTE, updatedAt = 2000, isDeleted = true)

        val toPull = serverEntriesToPull(listOf(oldDeletion), localRowForEntry = { null }, localDeletedAtByEntryId = emptyMap())

        assertEquals(emptyList(), toPull)
    }

    @Test
    fun aDeletionOfANoteStillOnThisDeviceIsPulled() {
        val deletion = SelfHostManifestEntry(entryId = "groceries", entryType = SelfHostEntryType.NOTE, updatedAt = 2000, isDeleted = true)
        val groceriesHere = localNote("groceries", updatedAt = 1500, selfHostSyncedAt = 1500)

        val toPull = serverEntriesToPull(listOf(deletion), localRowForEntry = { groceriesHere }, localDeletedAtByEntryId = emptyMap())

        assertEquals(listOf(deletion), toPull)
    }
}
