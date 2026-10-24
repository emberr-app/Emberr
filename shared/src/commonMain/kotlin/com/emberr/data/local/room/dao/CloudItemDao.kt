package com.emberr.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.emberr.data.local.room.entity.CloudItemEntity

@Dao
interface CloudItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: CloudItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<CloudItemEntity>)

    @Query("SELECT * FROM cloud_items WHERE spaceRef = :spaceRef AND itemId = :itemId LIMIT 1")
    suspend fun getItem(spaceRef: Long, itemId: String): CloudItemEntity?

    @Query("SELECT * FROM cloud_items WHERE localId = :localId")
    suspend fun getItemsByLocalId(localId: String): List<CloudItemEntity>

    @Query("SELECT * FROM cloud_items WHERE spaceRef = :spaceRef")
    suspend fun getItemsInSpace(spaceRef: Long): List<CloudItemEntity>

    @Query("DELETE FROM cloud_items")
    suspend fun deleteAllItems()
}
