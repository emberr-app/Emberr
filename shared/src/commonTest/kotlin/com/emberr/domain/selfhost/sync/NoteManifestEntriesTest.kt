package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteMetadataEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteManifestEntriesTest {

    private fun dailyEntry(spaceId: String, dateString: String, updatedAt: Long) = SelfHostManifestEntry(
        entryId = dailyManifestEntryId(spaceId, dateString),
        entryType = SelfHostEntryType.DAILY,
        spaceId = spaceId,
        updatedAt = updatedAt,
        dateString = dateString
    )

    private fun accountDailyNote(noteId: String, dateString: String, updatedAt: Long) = NoteMetadataEntity(
        noteId = noteId,
        title = "Daily: $dateString",
        folderId = null,
        isDaily = true,
        dateString = dateString,
        createdAt = 1L,
        updatedAt = updatedAt,
        filePath = "",
        selfHostSyncedAt = updatedAt,
        spaceId = ACCOUNT_SPACE_ID
    )

    @Test
    fun oldSpaceDailyEntriesMergedHereAreRetiredWithoutEverDeletingTheAccountDailyNote() {
        val mergedDate = "2026-10-20"
        val keptOldNoteIdDate = "2026-10-21"
        val editedAfterMergeDate = "2026-10-22"
        val serverEntries = listOf(
            dailyEntry("work", mergedDate, updatedAt = 100L),
            dailyEntry(ACCOUNT_SPACE_ID, mergedDate, updatedAt = 150L),
            dailyEntry("personal", keptOldNoteIdDate, updatedAt = 200L),
            dailyEntry("work", editedAfterMergeDate, updatedAt = 400L)
        )
        val localNotes = listOf(
            accountDailyNote("account-daily", mergedDate, updatedAt = 150L),
            accountDailyNote("personal-daily", keptOldNoteIdDate, updatedAt = 200L),
            accountDailyNote("work-daily", editedAfterMergeDate, updatedAt = 300L)
        )

        val entries = buildNoteManifestEntries(
            serverEntries = serverEntries,
            localNotes = localNotes,
            localTombstones = emptyList(),
            mediaFileNamesByNoteId = emptyMap(),
            noteIdsStillWaitingToUpload = emptySet(),
            oldSpaceDailyVersionsMergedHere = mapOf(
                dailyManifestEntryId("work", mergedDate) to 100L,
                dailyManifestEntryId("personal", keptOldNoteIdDate) to 200L,
                dailyManifestEntryId("work", editedAfterMergeDate) to 300L
            )
        ).entries
        val entriesById = entries.associateBy { it.entryId }

        assertTrue(entriesById.getValue(dailyManifestEntryId("work", mergedDate)).isDeleted)
        assertTrue(entriesById.getValue(dailyManifestEntryId("personal", keptOldNoteIdDate)).isDeleted)
        assertEquals(false, entriesById.getValue(dailyManifestEntryId("work", editedAfterMergeDate)).isDeleted)
        listOf(mergedDate, keptOldNoteIdDate, editedAfterMergeDate).forEach { dateString ->
            val accountEntries = entries.filter { it.entryId == dailyManifestEntryId(ACCOUNT_SPACE_ID, dateString) }
            assertEquals(listOf(false), accountEntries.map { it.isDeleted }, "account daily note for $dateString")
        }
    }
}
