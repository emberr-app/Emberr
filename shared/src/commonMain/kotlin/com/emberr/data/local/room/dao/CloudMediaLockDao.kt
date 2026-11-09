package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.CloudMediaLockEntity

@Dao
interface CloudMediaLockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocks(locks: List<CloudMediaLockEntity>)

    @Query("SELECT * FROM cloud_media_locks WHERE spaceRef = :spaceRef AND noteId = :noteId")
    suspend fun getLocksOfNote(spaceRef: Long, noteId: String): List<CloudMediaLockEntity>

    @Query("DELETE FROM cloud_media_locks")
    suspend fun deleteAllLocks()
}
