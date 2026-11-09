package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.emberr.data.local.room.entity.CloudMediaLockEntity

@Dao
interface CloudMediaLockDao {

    @Transaction
    suspend fun upsertLocks(locks: List<CloudMediaLockEntity>) {
        locks.forEach { lock ->
            saveLockUnlessStoredRevisionIsAsHigh(
                spaceRef = lock.spaceRef,
                mediaId = lock.mediaId,
                noteId = lock.noteId,
                encryptedKey = lock.encryptedKey,
                encryptedKeyNonce = lock.encryptedKeyNonce,
                revision = lock.revision,
                isDeleted = lock.isDeleted
            )
        }
    }

    @Query(
        """
        INSERT OR REPLACE INTO cloud_media_locks (spaceRef, mediaId, noteId, encryptedKey, encryptedKeyNonce, revision, isDeleted)
        SELECT :spaceRef, :mediaId, :noteId, :encryptedKey, :encryptedKeyNonce, :revision, :isDeleted
        WHERE NOT EXISTS (
            SELECT 1 FROM cloud_media_locks
            WHERE spaceRef = :spaceRef AND mediaId = :mediaId AND noteId = :noteId AND revision >= :revision
        )
        """
    )
    suspend fun saveLockUnlessStoredRevisionIsAsHigh(
        spaceRef: Long,
        mediaId: String,
        noteId: String,
        encryptedKey: String?,
        encryptedKeyNonce: String?,
        revision: Long,
        isDeleted: Boolean
    )

    @Query("SELECT * FROM cloud_media_locks WHERE spaceRef = :spaceRef AND noteId = :noteId")
    suspend fun getLocksOfNote(spaceRef: Long, noteId: String): List<CloudMediaLockEntity>

    @Query("SELECT * FROM cloud_media_locks WHERE isDeleted = 0")
    suspend fun getLiveLocks(): List<CloudMediaLockEntity>

    @Query("DELETE FROM cloud_media_locks")
    suspend fun deleteAllLocks()
}
