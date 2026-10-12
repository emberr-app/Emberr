package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.selfhost.translation.BlockTombstone

internal fun serverAlreadyHasMergedNote(
    localMetadata: NoteMetadataEntity?,
    remoteMetadata: NoteMetadataEntity?,
    mergedMetadata: NoteMetadataEntity,
    mergedBlocks: List<NoteBlockEntity>,
    remoteUpserts: List<NoteBlockEntity>,
    remoteDeletions: List<BlockTombstone>
): Boolean {
    if (remoteMetadata == null) return false
    val localHasUnsentChanges = localMetadata != null && localMetadata.updatedAt > localMetadata.selfHostSyncedAt
    if (localHasUnsentChanges) return false
    if (!mergedMetadata.isSameVersionAs(remoteMetadata)) return false

    val remoteBlocksById = remoteUpserts.associateBy { it.blockId }
    val remoteDeletedAtById = remoteDeletions.associate { it.blockId to it.deletedAt }
    return mergedBlocks.all { block ->
        if (block.isDeleted) {
            remoteDeletedAtById[block.blockId] == block.updatedAt
        } else {
            val remoteBlock = remoteBlocksById[block.blockId]
            remoteBlock != null && remoteBlock.updatedAt == block.updatedAt && remoteBlock.displayOrder == block.displayOrder
        }
    }
}

private fun NoteMetadataEntity.isSameVersionAs(other: NoteMetadataEntity): Boolean =
    updatedAt == other.updatedAt &&
        spaceId == other.spaceId &&
        kind == other.kind &&
        titleUpdatedAt == other.titleUpdatedAt &&
        folderUpdatedAt == other.folderUpdatedAt &&
        iconUpdatedAt == other.iconUpdatedAt &&
        favoriteUpdatedAt == other.favoriteUpdatedAt &&
        coverImageUpdatedAt == other.coverImageUpdatedAt &&
        wordCountUpdatedAt == other.wordCountUpdatedAt
