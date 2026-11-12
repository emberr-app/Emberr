package com.emberr.data.local.room.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "block_merge_bases",
    primaryKeys = ["noteId", "blockId"],
    foreignKeys = [
        ForeignKey(
            entity = NoteMetadataEntity::class,
            parentColumns = ["noteId"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BlockMergeBaseEntity(
    val noteId: String,
    val blockId: String,
    val blockDataJson: String,
    val updatedAt: Long
)
