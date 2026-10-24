package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.PropertyTagEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudSyncRulesTest {

    private fun folder(folderId: String, parentFolderId: String?) =
        FolderEntity(folderId = folderId, name = folderId, parentFolderId = parentFolderId, createdAt = 1)

    private fun note(
        noteId: String,
        spaceId: String = "work",
        folderId: String? = null,
        databaseId: String? = null,
        isDaily: Boolean = false,
        updatedAt: Long = 1,
        selfHostSyncedAt: Long = 0,
        cloudSyncedAt: Long = 0
    ) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = folderId,
        isDaily = isDaily,
        dateString = if (isDaily) "2026-10-24" else null,
        createdAt = 1,
        updatedAt = updatedAt,
        filePath = "",
        selfHostSyncedAt = selfHostSyncedAt,
        cloudSyncedAt = cloudSyncedAt,
        spaceId = spaceId,
        databaseId = databaseId
    )

    @Test
    fun anEditAfterTheEraseTimeSurvivesAndAnEditBeforeItIsDeleted() {
        val eraseTimeMicroseconds = 1_700_000_000_000_500L

        assertTrue(localEditIsNewerThanTombstone(localUpdatedAtMillis = 1_700_000_000_001L, eraseTimeMicroseconds))
        assertFalse(localEditIsNewerThanTombstone(localUpdatedAtMillis = 1_700_000_000_000L, eraseTimeMicroseconds))
    }

    @Test
    fun aChildFolderListedBeforeItsParentIsOrderedAfterIt() {
        val ordered = parentsBeforeChildren(
            items = listOf(folder("B", parentFolderId = "A"), folder("C", parentFolderId = "B"), folder("A", parentFolderId = null)),
            idOf = { it.folderId },
            parentIdOf = { it.parentFolderId }
        )

        assertEquals(listOf("A", "B", "C"), ordered.map { it.folderId })
    }

    @Test
    fun aNoteKeyIsLockedByItsDatabaseNoteThenItsFolderThenItsStream() {
        val rowInAFolder = note("row", folderId = "projects", databaseId = "tasks")

        assertEquals("database-tasks", noteKeyParentId(rowInAFolder) { true })
        assertEquals("work", noteKeyParentId(rowInAFolder) { false })
        assertEquals("projects", noteKeyParentId(note("plan", folderId = "projects")) { true })
        assertEquals(ACCOUNT_SPACE_ID, noteKeyParentId(note("today", folderId = "projects", isDaily = true)) { true })
        assertEquals(ACCOUNT_SPACE_ID, noteKeyParentId(note("inbox", spaceId = ACCOUNT_SPACE_ID)) { true })
    }

    @Test
    fun aPulledNoteIsMarkedSentOnlyWhenThisDeviceHasNoUnsentCloudEdit() {
        val serverCopy = note("groceries", updatedAt = 2000)
        val serverBlocks = listOf(
            NoteBlockEntity(blockId = "milk", noteId = "groceries", displayOrder = 0, blockDataJson = "{}", updatedAt = 2000, isDeleted = false)
        )
        fun cloudSyncedAtAfterPullingOnto(localCopy: NoteMetadataEntity) = cloudSyncedAtAfterPull(
            localMetadata = localCopy,
            remoteMetadata = serverCopy,
            mergedMetadata = serverCopy.copy(cloudSyncedAt = localCopy.cloudSyncedAt),
            mergedBlocks = serverBlocks,
            remoteUpserts = serverBlocks,
            remoteDeletions = emptyList()
        )

        val sentToSelfHostButNotToCloud = note("groceries", updatedAt = 1800, selfHostSyncedAt = 1800, cloudSyncedAt = 1500)
        val alreadySentToCloud = note("groceries", updatedAt = 1800, cloudSyncedAt = 1800)

        assertEquals(1500L, cloudSyncedAtAfterPullingOnto(sentToSelfHostButNotToCloud))
        assertEquals(2000L, cloudSyncedAtAfterPullingOnto(alreadySentToCloud))
    }

    @Test
    fun aListIsSentWhenTheCloudCopyLacksAnOlderEntryAndOtherwiseOnlyAfterANewerEdit() {
        fun tag(tagId: String, updatedAt: Long) =
            PropertyTagEntity(tagId = tagId, propertyKey = "status", name = tagId, createdAt = 1, updatedAt = updatedAt, spaceId = "work")
        val listRow = CloudItemEntity(
            spaceRef = 7, itemId = "space_lists", itemType = "SPACE_LISTS", localId = "space_lists", parentId = "work",
            encryptedKey = "a2V5", encryptedKeyNonce = "bm9uY2U=", revision = 3, syncedUpdatedAt = 500, isDeleted = false
        )
        val cloudCopy = CloudListsPayload(propertyTags = listOf(tag("from-other-device", updatedAt = 500)))
        val mergedWithAnOldTagNeverSent = CloudListsPayload(
            propertyTags = listOf(tag("old-tag-made-offline", updatedAt = 100), tag("from-other-device", updatedAt = 500))
        )

        assertTrue(listNeedsPush(mergedWithAnOldTagNeverSent, pulledCopy = cloudCopy, row = listRow, mustSendChanges = false))
        assertFalse(listNeedsPush(cloudCopy, pulledCopy = cloudCopy, row = listRow, mustSendChanges = false))
        assertFalse(listNeedsPush(cloudCopy, pulledCopy = null, row = listRow, mustSendChanges = false))
        assertTrue(listNeedsPush(cloudCopy, pulledCopy = null, row = listRow.copy(syncedUpdatedAt = 400), mustSendChanges = false))
    }

    @Test
    fun anEventExceptionGoesToTheStreamOfTheNoteHoldingItsBlockAndStaysHereWhenThatBlockIsUnknown() {
        fun exception(blockId: String) = CalendarEventExceptionEntity(blockId = blockId, occurrenceDate = "2026-10-24", isCancelled = true, updatedAt = 1)
        val notesById = mapOf("today" to note("today", isDaily = true), "plan" to note("plan"))
        val noteIdByBlockId = mapOf("gym" to "today", "standup" to "plan")

        val exceptionsByStream = eventExceptionsByStream(
            exceptions = listOf(exception("gym"), exception("standup"), exception("not-here-yet")),
            noteIdOfBlock = { blockId -> noteIdByBlockId[blockId] },
            noteOf = { noteId -> notesById[noteId] }
        )

        assertEquals(listOf("gym"), exceptionsByStream[ACCOUNT_SPACE_ID]?.map { it.blockId })
        assertEquals(listOf("standup"), exceptionsByStream["work"]?.map { it.blockId })
        assertEquals(2, exceptionsByStream.values.sumOf { it.size })
    }

    @Test
    fun syncHasWorkOnlyWhenARevisionMovedASpaceIsNewOrLocalChangesWait() {
        val seen = mapOf("work" to 12L, ACCOUNT_SPACE_ID to 7L)

        assertFalse(cloudSyncHasWork(mapOf("work" to 12L, ACCOUNT_SPACE_ID to 7L), seen, localChangesAreWaiting = false))
        assertTrue(cloudSyncHasWork(mapOf("work" to 13L, ACCOUNT_SPACE_ID to 7L), seen, localChangesAreWaiting = false))
        assertTrue(cloudSyncHasWork(seen + ("travel" to 1L), seen, localChangesAreWaiting = false))
        assertTrue(cloudSyncHasWork(seen, seen, localChangesAreWaiting = true))
    }
}
