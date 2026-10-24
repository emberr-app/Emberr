package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.cloud.api.CloudRemoteItem
import com.emberr.domain.cloud.keys.CloudItemType
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

fun listNeedsPush(
    listToSend: CloudListsPayload,
    pulledCopy: CloudListsPayload?,
    row: CloudItemEntity?,
    mustSendChanges: Boolean
): Boolean = when {
    pulledCopy != null -> !listToSend.hasSameEntriesAs(pulledCopy)
    row == null -> listToSend.hasEntries()
    else -> mustSendChanges || listToSend.newestEntryTime() > row.syncedUpdatedAt
}

inline fun eventExceptionsByStream(
    exceptions: List<CalendarEventExceptionEntity>,
    noteIdOfBlock: (blockId: String) -> String?,
    noteOf: (noteId: String) -> NoteMetadataEntity?
): Map<String, List<CalendarEventExceptionEntity>> =
    exceptions
        .mapNotNull { exception ->
            val note = noteIdOfBlock(exception.blockId)?.let { noteId -> noteOf(noteId) } ?: return@mapNotNull null
            streamSpaceIdOf(note) to exception
        }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

internal fun tombstonesPrunedFromFullList(
    rows: Collection<CloudItemEntity>,
    fullList: List<CloudRemoteItem>
): List<CloudRemoteItem> {
    val itemIdsOnServer = fullList.mapTo(HashSet()) { it.itemId }
    return rows.filter { row -> !row.isDeleted && row.itemId !in itemIdsOnServer }.map { row ->
        CloudRemoteItem(
            itemId = row.itemId,
            itemType = CloudItemType.entries.firstOrNull { it.name == row.itemType },
            payloadVersion = 0,
            parentId = null,
            lockedKey = null,
            inlinePayload = null,
            blobNonce = null,
            downloadUrl = null,
            revision = 0L,
            sizeBytes = 0L,
            deleted = true,
            updatedAtMicroseconds = row.syncedUpdatedAt * MICROSECONDS_PER_MILLISECOND
        )
    }
}

fun cloudSyncHasWork(
    latestRevisionsBySpaceId: Map<String, Long>,
    lastRevisionsSeenBySpaceId: Map<String, Long>,
    localChangesAreWaiting: Boolean
): Boolean =
    localChangesAreWaiting ||
        latestRevisionsBySpaceId.any { (spaceId, latestRevision) -> lastRevisionsSeenBySpaceId[spaceId] != latestRevision }

fun shouldSyncInBackground(signedInSessionCount: Int): Boolean = signedInSessionCount >= 2
