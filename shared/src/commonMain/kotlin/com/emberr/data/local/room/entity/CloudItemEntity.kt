package com.emberr.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cloud_items",
    primaryKeys = ["spaceRef", "itemId"],
    indices = [Index(value = ["localId"])]
)
data class CloudItemEntity(
    val spaceRef: Long,
    val itemId: String,
    val itemType: String,
    val localId: String,
    val parentId: String?,
    val encryptedKey: String?,
    val encryptedKeyNonce: String?,
    val revision: Long,
    val syncedUpdatedAt: Long,
    val isDeleted: Boolean
)
