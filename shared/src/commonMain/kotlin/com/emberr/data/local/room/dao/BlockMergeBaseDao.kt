package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.emberr.data.local.room.entity.BlockMergeBaseEntity

@Dao
interface BlockMergeBaseDao {

    @Query("SELECT * FROM block_merge_bases WHERE noteId = :noteId")
    suspend fun getBasesOfNote(noteId: String): List<BlockMergeBaseEntity>

    @Upsert
    suspend fun upsertBases(bases: List<BlockMergeBaseEntity>)

    @Query("DELETE FROM block_merge_bases WHERE noteId = :noteId AND blockId IN (:blockIds)")
    suspend fun deleteBasesOfBlocks(noteId: String, blockIds: List<String>)
}
