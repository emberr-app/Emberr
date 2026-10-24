package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.entity.FolderEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudSyncRulesTest {

    private fun folder(folderId: String, parentFolderId: String?) =
        FolderEntity(folderId = folderId, name = folderId, parentFolderId = parentFolderId, createdAt = 1)

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
}
