package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.CloudSpaceEntity

@Dao
interface CloudSpaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpace(space: CloudSpaceEntity)

    @Query("SELECT * FROM cloud_spaces")
    suspend fun getAllSpaces(): List<CloudSpaceEntity>

    @Query("SELECT * FROM cloud_spaces WHERE appSpaceId = :appSpaceId LIMIT 1")
    suspend fun getSpaceByAppSpaceId(appSpaceId: String): CloudSpaceEntity?

    @Query("UPDATE cloud_spaces SET lastRevisionSeen = :revision WHERE appSpaceId = :appSpaceId")
    suspend fun updateLastRevisionSeen(appSpaceId: String, revision: Long)

    @Query("DELETE FROM cloud_spaces")
    suspend fun deleteAllSpaces()
}
