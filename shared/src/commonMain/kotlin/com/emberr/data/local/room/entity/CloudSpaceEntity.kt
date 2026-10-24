package com.emberr.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_spaces")
data class CloudSpaceEntity(
    @PrimaryKey val appSpaceId: String,
    val spaceRef: Long,
    val encryptedKey: String,
    val encryptedKeyNonce: String,
    val lastRevisionSeen: Long = 0L
)
