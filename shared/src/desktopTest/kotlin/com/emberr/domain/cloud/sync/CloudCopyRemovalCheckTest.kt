package com.emberr.domain.cloud.sync

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.CloudMediaLockEntity
import com.emberr.data.local.room.entity.CloudSpaceEntity
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.cloud.api.CloudSubscription
import com.emberr.domain.cloud.keys.CloudItemType
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CloudCopyRemovalCheckTest {

    private val appDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()

    @AfterTest
    fun closeDatabase() {
        appDatabase.close()
    }

    @Test
    fun aRemovedCloudCopyForgetsTheSyncRecordsOnceKeepsEveryNoteAndAnswersRemovedEveryTime() = runTest {
        val noteDao = appDatabase.noteDao()
        val cloudSpaceDao = appDatabase.cloudSpaceDao()
        val cloudItemDao = appDatabase.cloudItemDao()
        val cloudMediaLockDao = appDatabase.cloudMediaLockDao()
        noteDao.insertOrUpdateMetadata(
            NoteMetadataEntity(
                noteId = "kept",
                title = "Kept",
                folderId = null,
                isDaily = false,
                dateString = null,
                createdAt = 1L,
                updatedAt = 2L,
                filePath = "",
                cloudSyncedAt = 2L
            )
        )
        cloudSpaceDao.upsertSpace(CloudSpaceEntity(appSpaceId = DEFAULT_SPACE_ID, spaceRef = 2L, encryptedKey = "a", encryptedKeyNonce = "b"))
        cloudItemDao.upsertItem(
            CloudItemEntity(
                spaceRef = 2L,
                itemId = "kept",
                itemType = CloudItemType.NOTE.name,
                localId = "kept",
                parentId = DEFAULT_SPACE_ID,
                encryptedKey = null,
                encryptedKeyNonce = null,
                revision = 3L,
                syncedUpdatedAt = 2L,
                isDeleted = false
            )
        )
        cloudMediaLockDao.upsertLocks(
            listOf(CloudMediaLockEntity(2L, "media_a", "kept", "c", "d", revision = 4L, isDeleted = false))
        )
        var subscription: CloudSubscription? = null
        var timesForgotten = 0
        val removalCheck = CloudCopyRemovalCheck(
            latestSubscription = { subscription },
            forgetCloudSyncRecords = {
                timesForgotten++
                forgetCloudSyncRecords(cloudSpaceDao, cloudItemDao, cloudMediaLockDao, noteDao)
            }
        )

        assertNull(removalCheck.forgetRecordsIfCloudCopyRemoved())

        subscription = CloudSubscription(
            plan = CloudSubscription.TRIAL_PLAN,
            storageLimitBytes = 104_857_600L,
            expiresAtMicroseconds = 1_000_000L,
            cloudCopyRemovedAtMicroseconds = 5_000_000L
        )
        val firstPass = removalCheck.forgetRecordsIfCloudCopyRemoved()
        val nextPass = removalCheck.forgetRecordsIfCloudCopyRemoved()

        assertEquals(CloudSyncResult.CloudCopyRemoved(5_000_000L), firstPass)
        assertEquals(CloudSyncResult.CloudCopyRemoved(5_000_000L), nextPass)
        assertEquals(1, timesForgotten)
        assertTrue(cloudSpaceDao.getAllSpaces().isEmpty())
        assertTrue(cloudItemDao.getItemsInSpace(2L).isEmpty())
        assertTrue(cloudMediaLockDao.getLiveLocks().isEmpty())
        assertEquals(0L, noteDao.getNoteById("kept")?.cloudSyncedAt)
    }
}
