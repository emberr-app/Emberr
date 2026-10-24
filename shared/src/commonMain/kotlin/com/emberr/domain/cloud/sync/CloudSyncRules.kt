package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.selfhost.sync.serverAlreadyHasMergedNote
import com.emberr.domain.selfhost.translation.BlockTombstone

private const val MICROSECONDS_PER_MILLISECOND = 1_000L

fun localEditIsNewerThanTombstone(localUpdatedAtMillis: Long, tombstoneUpdatedAtMicroseconds: Long): Boolean =
    localUpdatedAtMillis * MICROSECONDS_PER_MILLISECOND > tombstoneUpdatedAtMicroseconds

fun tombstoneTimeInMillis(tombstoneUpdatedAtMicroseconds: Long): Long =
    tombstoneUpdatedAtMicroseconds / MICROSECONDS_PER_MILLISECOND

fun <Item> parentsBeforeChildren(
    items: List<Item>,
    idOf: (Item) -> String,
    parentIdOf: (Item) -> String?
): List<Item> {
    val itemsById = items.associateBy(idOf)
    val ordered = LinkedHashMap<String, Item>()

    fun addAfterItsParents(item: Item, idsOnThisPath: MutableSet<String>) {
        val id = idOf(item)
        if (id in ordered || !idsOnThisPath.add(id)) return
        parentIdOf(item)?.let(itemsById::get)?.let { parent -> addAfterItsParents(parent, idsOnThisPath) }
        ordered[id] = item
    }

    items.forEach { item -> addAfterItsParents(item, mutableSetOf()) }
    return ordered.values.toList()
}

fun streamSpaceIdOf(note: NoteMetadataEntity): String =
    if (note.isDaily) ACCOUNT_SPACE_ID else note.spaceId

fun noteKeyParentCandidateOf(note: NoteMetadataEntity): String? {
    val databaseId = note.databaseId
    return when {
        note.isDaily -> null
        databaseId != null && note.noteId != databaseNoteId(databaseId) -> databaseNoteId(databaseId)
        else -> note.folderId
    }
}

inline fun noteKeyParentId(note: NoteMetadataEntity, parentCanHoldKey: (parentId: String) -> Boolean): String {
    val candidateId = noteKeyParentCandidateOf(note)
    return if (candidateId != null && parentCanHoldKey(candidateId)) candidateId else streamSpaceIdOf(note)
}

internal fun cloudSyncedAtAfterPull(
    localMetadata: NoteMetadataEntity?,
    remoteMetadata: NoteMetadataEntity,
    mergedMetadata: NoteMetadataEntity,
    mergedBlocks: List<NoteBlockEntity>,
    remoteUpserts: List<NoteBlockEntity>,
    remoteDeletions: List<BlockTombstone>
): Long {
    val cloudAlreadyHasMergedNote = serverAlreadyHasMergedNote(
        localMetadata = localMetadata?.copy(selfHostSyncedAt = localMetadata.cloudSyncedAt),
        remoteMetadata = remoteMetadata,
        mergedMetadata = mergedMetadata,
        mergedBlocks = mergedBlocks,
        remoteUpserts = remoteUpserts,
        remoteDeletions = remoteDeletions
    )
    val keptCloudSyncedAt = localMetadata?.cloudSyncedAt ?: 0L
    return when {
        cloudAlreadyHasMergedNote -> mergedMetadata.updatedAt
        mergedMetadata.updatedAt > keptCloudSyncedAt -> keptCloudSyncedAt
        else -> 0L
    }
}
