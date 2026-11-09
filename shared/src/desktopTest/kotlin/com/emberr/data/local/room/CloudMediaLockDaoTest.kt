package com.emberr.data.local.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.entity.CloudMediaLockEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CloudMediaLockDaoTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val cloudMediaLockDao = database.cloudMediaLockDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun lock(revision: Long, isDeleted: Boolean) = CloudMediaLockEntity(
        spaceRef = 7L,
        mediaId = "media_1",
        noteId = "note-1",
        encryptedKey = if (isDeleted) null else "key-$revision",
        encryptedKeyNonce = if (isDeleted) null else "nonce-$revision",
        revision = revision,
        isDeleted = isDeleted
    )

    @Test
    fun anOlderLockNeverReplacesANewerOneButANewerOneDoes() = runTest {
        cloudMediaLockDao.upsertLocks(listOf(lock(revision = 5, isDeleted = true)))

        cloudMediaLockDao.upsertLocks(listOf(lock(revision = 3, isDeleted = false)))
        assertEquals(listOf(lock(revision = 5, isDeleted = true)), cloudMediaLockDao.getLocksOfNote(7L, "note-1"))

        cloudMediaLockDao.upsertLocks(listOf(lock(revision = 8, isDeleted = false)))
        assertEquals(listOf(lock(revision = 8, isDeleted = false)), cloudMediaLockDao.getLocksOfNote(7L, "note-1"))
    }
}
