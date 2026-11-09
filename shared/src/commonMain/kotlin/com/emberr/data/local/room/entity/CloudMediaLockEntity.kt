package com.emberr.data.local.room.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cloud_media_locks",
    primaryKeys = ["spaceRef", "mediaId", "noteId"],
    indices = [Index(value = ["spaceRef", "noteId"])]
)
data class CloudMediaLockEntity(
    val spaceRef: Long,
    val mediaId: String,
    val noteId: String,
    val encryptedKey: String?,
    val encryptedKeyNonce: String?,
    val revision: Long,
    val isDeleted: Boolean
)
