package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerAlreadyHasMergedNoteTest {

    private fun groceries(updatedAt: Long, selfHostSyncedAt: Long = 0L) = NoteMetadataEntity(
        noteId = "groceries",
        title = "Groceries",
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 100,
        updatedAt = updatedAt,
        filePath = "",
        selfHostSyncedAt = selfHostSyncedAt
    )

    private fun block(blockId: String, updatedAt: Long, displayOrder: Int = 0) = NoteBlockEntity(
        blockId = blockId,
        noteId = "groceries",
        displayOrder = displayOrder,
        blockDataJson = "{}",
        updatedAt = updatedAt,
        isDeleted = false
    )

    @Test
    fun aNoteOnlyChangedOnTheServerIsNotUploadedBack() {
        val serverCopy = groceries(updatedAt = 2000)
        val serverBlocks = listOf(block("milk", updatedAt = 2000), block("eggs", updatedAt = 1500, displayOrder = 1))

        val alreadyOnServer = serverAlreadyHasMergedNote(
            localMetadata = groceries(updatedAt = 1500, selfHostSyncedAt = 1500),
            remoteMetadata = serverCopy,
            mergedMetadata = serverCopy,
            mergedBlocks = serverBlocks,
            remoteUpserts = serverBlocks,
            remoteDeletions = emptyList()
        )

        assertTrue(alreadyOnServer)
    }

    @Test
    fun aNoteWithAnUnsentEditOnThisDeviceIsStillUploaded() {
        val serverCopy = groceries(updatedAt = 2000)
        val serverBlocks = listOf(block("milk", updatedAt = 2000))
        val mergedBlocks = serverBlocks + block("bread", updatedAt = 1800, displayOrder = 1)

        val alreadyOnServer = serverAlreadyHasMergedNote(
            localMetadata = groceries(updatedAt = 1800, selfHostSyncedAt = 1500),
            remoteMetadata = serverCopy,
            mergedMetadata = serverCopy,
            mergedBlocks = mergedBlocks,
            remoteUpserts = serverBlocks,
            remoteDeletions = emptyList()
        )

        assertFalse(alreadyOnServer)
    }
}
