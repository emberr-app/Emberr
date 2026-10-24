package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.FolderDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.SelfHostDeletedNoteDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SelfHostDeletedNoteEntity
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudBlobTransfer
import com.emberr.domain.cloud.api.CloudRemoteItem
import com.emberr.domain.cloud.api.CloudUploadUrl
import com.emberr.domain.cloud.api.CloudWriteResult
import com.emberr.domain.cloud.api.toBase64
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudDailyNoteIds
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.NoteContent
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.selfhost.merge.NoteMergeHelper
import com.emberr.domain.selfhost.sync.PreparedSyncOperations
import com.emberr.domain.selfhost.translation.NOTE_PAYLOAD_SCHEMA_VERSION
import com.emberr.domain.selfhost.translation.NoteJsonCompiler
import com.emberr.domain.selfhost.translation.NoteJsonParser
import com.emberr.domain.selfhost.translation.NotePayloadSyncException
import com.emberr.domain.space.SpaceRepository
import com.emberr.domain.sync.withNewerDetailsFrom
import com.emberr.domain.util.sync.SyncEventBus
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private class NotePass(
    val sessionToken: String,
    val streams: List<CloudStream>,
    val counts: SyncPassCounts
) {
    val streamsBySpaceId = streams.associateBy { it.appSpaceId }
    val accountStream = streams.first { it.isAccountStream }
    val foldersById = HashMap<String, FolderEntity?>()
    var localDailyNotesByItemId: Map<String, NoteMetadataEntity>? = null
}

private class NoteKeyToSend(val openKey: ByteArray, val lockedKey: CloudEncryptedBytes)

private class SavedNoteKey(val stream: CloudStream, val lockedKey: CloudEncryptedBytes, val parentId: String)

class CloudNoteSync(
    private val apiClient: CloudApiClient,
    private val blobTransfer: CloudBlobTransfer,
    private val itemKeyManager: CloudItemKeyManager,
    private val dailyNoteIds: CloudDailyNoteIds,
    cloudItemDao: CloudItemDao,
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val folderDao: FolderDao,
    private val selfHostDeletedNoteDao: SelfHostDeletedNoteDao,
    private val noteRepository: NoteRepository,
    private val spaceRepository: SpaceRepository,
    private val canvasRepository: CanvasRepository
) {

    private val blockJson = Json { ignoreUnknownKeys = true }
    private val streamRecords = CloudStreamRecords(itemKeyManager, cloudItemDao)

    internal suspend fun applyPulledNotes(
        sessionToken: String,
        streams: List<CloudStream>,
        itemsNotYetAppliedByStream: List<Pair<CloudStream, List<CloudRemoteItem>>>,
        counts: SyncPassCounts
    ) {
        val pass = NotePass(sessionToken, streams, counts)
        val noteItemsByStream = itemsNotYetAppliedByStream.map { (stream, items) ->
            stream to items.filter { item -> isNoteItemOf(stream, item) }
        }
        noteItemsByStream.forEach { (stream, items) ->
            parentsBeforeChildren(items.filterNot { it.deleted }, idOf = { it.itemId }, parentIdOf = { it.parentId })
                .forEach { item -> applyPulledLiveNote(pass, stream, item, openPulledNote(stream, item)) }
        }
        noteItemsByStream.forEach { (stream, items) ->
            items.filter { it.deleted }.forEach { item -> applyPulledNoteTombstone(pass, stream, item) }
        }
    }

    internal suspend fun pushNotes(sessionToken: String, streams: List<CloudStream>, counts: SyncPassCounts) {
        val pass = NotePass(sessionToken, streams, counts)
        val localNotesById = noteDao.getAllNotesForBackup().associateBy { it.noteId }
        val noteIdsToPush = noteDao.getNotesNeedingCloudSync().mapTo(LinkedHashSet()) { it.noteId }
        streams.forEach { stream ->
            stream.rowsByItemId.values.filter { it.isNoteRow() }.forEach { row ->
                val note = localNotesById[row.localId]
                if (note != null && rowNoLongerMatchesNote(pass, stream, row, note)) noteIdsToPush += note.noteId
            }
        }
        parentsBeforeChildren(
            items = noteIdsToPush.mapNotNull(localNotesById::get),
            idOf = { it.noteId },
            parentIdOf = ::noteKeyParentCandidateOf
        ).forEach { note -> pushNoteUnlessBroken(pass, note.noteId) }
        pushLocalDeletions(pass)
    }

    private fun isNoteItemOf(stream: CloudStream, item: CloudRemoteItem): Boolean =
        item.itemType == CloudItemType.NOTE || (item.itemType == CloudItemType.DAILY && stream.isAccountStream)

    private suspend fun openPulledNote(stream: CloudStream, item: CloudRemoteItem): PreparedSyncOperations {
        val downloadUrl = item.downloadUrl ?: throw CloudDecryptionException()
        val blobNonce = item.blobNonce ?: throw CloudDecryptionException()
        val noteKey = streamRecords.openPulledItemKey(stream, item)
        val encryptedNote = CloudEncryptedBytes(blobTransfer.download(downloadUrl), blobNonce)
        val noteJson = itemKeyManager.decryptContent(encryptedNote, noteKey, item.noteItemType(), item.itemId).decodeToString()
        return NoteJsonParser.parseJsonToDatabaseOperations(noteJson)
    }

    private suspend fun applyPulledLiveNote(
        pass: NotePass,
        stream: CloudStream,
        item: CloudRemoteItem,
        remoteOperations: PreparedSyncOperations
    ) {
        val isDailyItem = item.noteItemType() == CloudItemType.DAILY
        val remoteMetadata = remoteOperations.metadataUpsert.copy(
            spaceId = if (isDailyItem) ACCOUNT_SPACE_ID else stream.appSpaceId
        )
        val remoteDateString = remoteMetadata.dateString
        if (isDailyItem && (!remoteMetadata.isDaily || remoteDateString == null)) {
            throw NotePayloadSyncException("Daily note item ${item.itemId} holds no date")
        }
        val localMetadata = if (isDailyItem && remoteDateString != null) {
            noteDao.getDailyNoteMetadata(ACCOUNT_SPACE_ID, remoteDateString)
        } else {
            noteDao.getNoteById(item.itemId)
        }

        if (localMetadata == null) {
            val localDeletion = if (isDailyItem && remoteDateString != null) {
                selfHostDeletedNoteDao.getTombstoneByDateString(ACCOUNT_SPACE_ID, remoteDateString)
            } else {
                selfHostDeletedNoteDao.getTombstoneByNoteId(item.itemId)
            }
            if (localDeletion != null && remoteMetadata.updatedAt <= localDeletion.deletedAt) {
                streamRecords.saveRow(stream, rowForLiveItem(stream, item, localDeletion.noteId, remoteMetadata.updatedAt))
                return
            }
        }

        val noteId = localMetadata?.noteId ?: if (isDailyItem) unusedNoteIdOrNew(remoteMetadata.noteId) else item.itemId
        val localBlocks = blockDao.getAllBlocksForNoteIncludingDeleted(noteId).filter { it.noteId == noteId }
        val remoteUpserts = remoteOperations.blockUpserts.map { it.copy(noteId = noteId) }
        val remoteDeletions = remoteOperations.blockDeletions
        val mergedBlocks = NoteMergeHelper.mergeBlocks(
            noteId = noteId,
            localBlocks = localBlocks,
            remoteUpserts = remoteUpserts,
            remoteDeletions = remoteDeletions,
            remoteIsNewer = localMetadata == null || remoteMetadata.updatedAt > localMetadata.updatedAt
        )
        val newerMetadata = pickNewerMetadata(localMetadata, remoteMetadata)
        val mergedMetadata = newerMetadata.copy(
            noteId = noteId,
            kind = localMetadata?.kind ?: newerMetadata.kind,
            filePath = "",
            selfHostSyncedAt = localMetadata?.selfHostSyncedAt ?: 0L,
            cloudSyncedAt = localMetadata?.cloudSyncedAt ?: 0L
        )

        spaceRepository.ensureSpaceExists(mergedMetadata.spaceId)
        if (localMetadata != null && localMetadata.spaceId != mergedMetadata.spaceId) {
            noteRepository.applyRemoteSpaceChangeWhileSyncLockHeld(noteId, mergedMetadata.spaceId)
        }
        noteDao.insertOrUpdateMetadata(mergedMetadata)
        blockDao.insertOrUpdateBlocks(mergedBlocks)
        val canvasChanged = canvasRepository.applyRemoteCanvasWhileSyncLockHeld(noteId, remoteOperations.canvas)

        val refreshedContent = NoteContent(
            blocks = mergedBlocks.sortedBy { it.displayOrder }.mapNotNull { decodeBlockOrNull(it) }
        )
        val mergedDateString = mergedMetadata.dateString
        if (mergedMetadata.isDaily && mergedDateString != null) {
            noteRepository.refreshDailyNoteCache(mergedMetadata.spaceId, mergedDateString, refreshedContent)
        }
        noteRepository.refreshNoteContentCache(noteId, refreshedContent)
        noteRepository.refreshProjectionsForNote(mergedMetadata, refreshedContent.blocks)
        SyncEventBus.emitSyncCompleted(
            if (mergedMetadata.isDaily) mergedDateString ?: noteId else noteId,
            mergedMetadata.spaceId
        )

        val noteChangedHere = canvasChanged ||
            localMetadata?.copy(filePath = "") != mergedMetadata ||
            localBlocks.toSet() != mergedBlocks.toSet()
        if (noteChangedHere) {
            pass.counts.receivedCount++
            if (mergedMetadata.isDaily && mergedDateString != null) {
                noteRepository.indexDailyNote(mergedDateString, refreshedContent, mergedMetadata)
            } else {
                noteRepository.indexNote(mergedMetadata, refreshedContent)
            }
        }

        val cloudSyncedAt = cloudSyncedAtAfterPull(
            localMetadata = localMetadata,
            remoteMetadata = remoteMetadata,
            mergedMetadata = mergedMetadata,
            mergedBlocks = mergedBlocks,
            remoteUpserts = remoteUpserts,
            remoteDeletions = remoteDeletions
        )
        if (cloudSyncedAt != mergedMetadata.cloudSyncedAt) noteDao.updateCloudSyncedAt(noteId, cloudSyncedAt)

        streamRecords.saveRow(stream, rowForLiveItem(stream, item, noteId, remoteMetadata.updatedAt))
    }

    private suspend fun applyPulledNoteTombstone(pass: NotePass, stream: CloudStream, item: CloudRemoteItem) {
        val row = stream.rowsByItemId[item.itemId]
        val localNote = localNoteOfItem(pass, stream, item, row)
        if (localNote != null && !localEditIsNewerThanTombstone(localNote.updatedAt, item.updatedAtMicroseconds)) {
            noteRepository.hardDeleteLocalNote(localNote.noteId)
            SyncEventBus.emitSyncCompleted(
                if (localNote.isDaily) localNote.dateString ?: localNote.noteId else localNote.noteId,
                localNote.spaceId
            )
            pass.counts.receivedCount++
        }
        streamRecords.saveRow(
            stream,
            CloudItemEntity(
                spaceRef = stream.spaceRef,
                itemId = item.itemId,
                itemType = item.noteItemType().name,
                localId = localNote?.noteId ?: row?.localId ?: item.itemId,
                parentId = row?.parentId,
                encryptedKey = row?.encryptedKey,
                encryptedKeyNonce = row?.encryptedKeyNonce,
                revision = item.revision,
                syncedUpdatedAt = tombstoneTimeInMillis(item.updatedAtMicroseconds),
                isDeleted = true
            )
        )
    }

    private suspend fun localNoteOfItem(
        pass: NotePass,
        stream: CloudStream,
        item: CloudRemoteItem,
        row: CloudItemEntity?
    ): NoteMetadataEntity? =
        if (item.noteItemType() == CloudItemType.DAILY) {
            row?.localId?.let { noteDao.getNoteById(it) }?.takeIf { it.isDaily && it.spaceId == ACCOUNT_SPACE_ID }
                ?: localDailyNotesByItemId(pass)[item.itemId]
        } else {
            noteDao.getNoteById(item.itemId)?.takeIf { !it.isDaily && it.spaceId == stream.appSpaceId }
        }

    private suspend fun localDailyNotesByItemId(pass: NotePass): Map<String, NoteMetadataEntity> =
        pass.localDailyNotesByItemId ?: noteDao.getAllDailyNoteMetadata(ACCOUNT_SPACE_ID)
            .mapNotNull { note -> note.dateString?.let { dateString -> dailyItemIdOf(pass, dateString) to note } }
            .toMap()
            .also { pass.localDailyNotesByItemId = it }

    private suspend fun dailyItemIdOf(pass: NotePass, dateString: String): String =
        dailyNoteIds.dailyNoteId(streamRecords.openKeyOf(pass.accountStream, ACCOUNT_SPACE_ID), dateString)

    private suspend fun rowNoLongerMatchesNote(
        pass: NotePass,
        stream: CloudStream,
        row: CloudItemEntity,
        note: NoteMetadataEntity
    ): Boolean {
        val noteBelongsInThisRow = streamSpaceIdOf(note) == stream.appSpaceId &&
            note.isDaily == (row.itemType == CloudItemType.DAILY.name)
        return when {
            row.isDeleted -> noteBelongsInThisRow
            !noteBelongsInThisRow -> true
            else -> row.parentId != keyParentIdOf(pass, stream, note)
        }
    }

    private suspend fun keyParentIdOf(pass: NotePass, stream: CloudStream, note: NoteMetadataEntity): String =
        noteKeyParentId(note) { parentId -> parentCanHoldKey(pass, stream, note, parentId) }

    private suspend fun parentCanHoldKey(
        pass: NotePass,
        stream: CloudStream,
        note: NoteMetadataEntity,
        parentId: String
    ): Boolean {
        if (parentId == note.databaseId?.let(::databaseNoteId)) {
            return noteDao.getNoteById(parentId)?.spaceId == note.spaceId
        }
        val folder = if (parentId in pass.foldersById) {
            pass.foldersById[parentId]
        } else {
            folderDao.getFolderById(parentId).also { pass.foldersById[parentId] = it }
        }
        val folderRow = stream.rowsByItemId[parentId]
        return folder != null && folder.spaceId == note.spaceId && folderRow?.isDeleted != true &&
            (folderRow != null || folder.updatedAt > 0L)
    }

    private fun keyParentHasKey(stream: CloudStream, keyParentId: String): Boolean {
        if (keyParentId == stream.appSpaceId) return true
        val parentRow = stream.rowsByItemId[keyParentId] ?: return false
        return !parentRow.isDeleted && parentRow.encryptedKey != null
    }

    private suspend fun pushNoteUnlessBroken(pass: NotePass, noteId: String) {
        try {
            if (moveNoteOutOfOtherStreams(pass, noteId)) commitNote(pass, noteId)
        } catch (_: NotePayloadSyncException) {
            pass.counts.notSentCount++
        }
    }

    private suspend fun moveNoteOutOfOtherStreams(pass: NotePass, noteId: String): Boolean {
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val note = noteDao.getNoteById(noteId) ?: return false
            if (note.spaceId !in pass.streamsBySpaceId) return false
            val (oldStream, oldRow) = liveRowsInOtherStreams(pass, note).firstOrNull() ?: return true
            when (val result = apiClient.deleteItem(pass.sessionToken, oldStream.spaceRef, oldRow.itemId, oldRow.revision)) {
                is CloudWriteResult.Saved -> {
                    streamRecords.saveRow(
                        oldStream,
                        oldRow.copy(revision = result.revision, syncedUpdatedAt = note.updatedAt, isDeleted = true)
                    )
                    pass.counts.sentCount++
                }
                is CloudWriteResult.Conflict ->
                    takeInConflictingNote(pass, oldStream, oldRow.itemId, CloudItemType.NOTE, result.currentItem)
            }
        }
        val note = noteDao.getNoteById(noteId) ?: return false
        return liveRowsInOtherStreams(pass, note).isEmpty()
    }

    private fun liveRowsInOtherStreams(pass: NotePass, note: NoteMetadataEntity): List<Pair<CloudStream, CloudItemEntity>> {
        if (note.isDaily) return emptyList()
        return pass.streams.filter { it.appSpaceId != note.spaceId }.mapNotNull { stream ->
            stream.rowsByItemId[note.noteId]
                ?.takeIf { it.itemType == CloudItemType.NOTE.name && !it.isDeleted }
                ?.let { row -> stream to row }
        }
    }

    private suspend fun commitNote(pass: NotePass, noteId: String) {
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val note = noteDao.getNoteById(noteId) ?: return
            val stream = pass.streamsBySpaceId[note.spaceId] ?: return
            if (note.isDaily && !stream.isAccountStream) return
            val itemType = if (note.isDaily) CloudItemType.DAILY else CloudItemType.NOTE
            val itemId = if (note.isDaily) dailyItemIdOf(pass, note.dateString ?: return) else note.noteId
            val row = stream.rowsByItemId[itemId]
            val keyParentId = keyParentIdOf(pass, stream, note)
            val needsPush = row == null || row.isDeleted || row.localId != note.noteId ||
                row.parentId != keyParentId || note.updatedAt > note.cloudSyncedAt
            if (!needsPush || !keyParentHasKey(stream, keyParentId)) return

            val noteKey = noteKeyToSend(pass, stream, itemId, keyParentId)
            val noteJson = NoteJsonCompiler.compileNoteToJson(
                metadata = note,
                blocks = blockDao.getAllBlocksForNoteIncludingDeleted(noteId),
                canvas = canvasRepository.loadCanvasIncludingDeleted(noteId)
            )
            val encryptedNote = itemKeyManager.encryptContent(noteJson.encodeToByteArray(), noteKey.openKey, itemType, itemId)
            val sizeBytes = encryptedNote.cipherText.size.toLong()
            val uploadUrl = requestUploadUrlUnlessTooLarge(pass, stream, itemId, sizeBytes) ?: run {
                pass.counts.notSentCount++
                return
            }
            blobTransfer.upload(uploadUrl.uploadUrl, encryptedNote.cipherText)
            val result = apiClient.commitBlobItem(
                sessionToken = pass.sessionToken,
                spaceRef = stream.spaceRef,
                itemId = itemId,
                baseRevision = row?.revision ?: 0L,
                itemType = itemType,
                payloadVersion = NOTE_PAYLOAD_SCHEMA_VERSION,
                parentId = keyParentId,
                lockedKey = noteKey.lockedKey,
                objectKey = uploadUrl.objectKey,
                blobNonce = encryptedNote.nonce,
                sizeBytes = sizeBytes
            )
            when (result) {
                is CloudWriteResult.Saved -> {
                    stream.openKeys[itemId] = noteKey.openKey
                    streamRecords.saveRow(
                        stream,
                        CloudItemEntity(
                            spaceRef = stream.spaceRef,
                            itemId = itemId,
                            itemType = itemType.name,
                            localId = noteId,
                            parentId = keyParentId,
                            encryptedKey = noteKey.lockedKey.cipherText.toBase64(),
                            encryptedKeyNonce = noteKey.lockedKey.nonce.toBase64(),
                            revision = result.revision,
                            syncedUpdatedAt = note.updatedAt,
                            isDeleted = false
                        )
                    )
                    noteDao.updateCloudSyncedAt(noteId, note.updatedAt)
                    pass.counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict ->
                    takeInConflictingNote(pass, stream, itemId, itemType, result.currentItem)
            }
        }
    }

    private suspend fun noteKeyToSend(pass: NotePass, stream: CloudStream, itemId: String, keyParentId: String): NoteKeyToSend {
        val keyParentKey = streamRecords.openKeyOf(stream, keyParentId)
        val savedKey = savedNoteKeyOf(stream, itemId) ?: pass.streams.firstNotNullOfOrNull { savedNoteKeyOf(it, itemId) }
        if (savedKey == null) {
            val newNoteKey = itemKeyManager.newItemKey()
            return NoteKeyToSend(
                openKey = newNoteKey,
                lockedKey = itemKeyManager.lockChildKey(newNoteKey, itemId, keyParentKey, keyParentId)
            )
        }
        val lockedKey = if (savedKey.stream === stream && savedKey.parentId == keyParentId) {
            savedKey.lockedKey
        } else {
            itemKeyManager.relockChildKeyForMove(
                lockedChildKey = savedKey.lockedKey,
                childId = itemId,
                oldParentKey = streamRecords.openKeyOf(savedKey.stream, savedKey.parentId),
                oldParentId = savedKey.parentId,
                newParentKey = keyParentKey,
                newParentId = keyParentId
            )
        }
        return NoteKeyToSend(openKey = streamRecords.openKeyOf(savedKey.stream, itemId), lockedKey = lockedKey)
    }

    private fun savedNoteKeyOf(stream: CloudStream, itemId: String): SavedNoteKey? {
        val row = stream.rowsByItemId[itemId] ?: return null
        val lockedKey = row.lockedKeyOrNull() ?: return null
        val parentId = row.parentId ?: return null
        return SavedNoteKey(stream, lockedKey, parentId)
    }

    private suspend fun requestUploadUrlUnlessTooLarge(
        pass: NotePass,
        stream: CloudStream,
        itemId: String,
        sizeBytes: Long
    ): CloudUploadUrl? =
        try {
            apiClient.requestUploadUrl(pass.sessionToken, stream.spaceRef, itemId, sizeBytes)
        } catch (error: CloudApiException) {
            if (error.code != CloudApiErrorCode.REQUEST_TOO_LARGE) throw error
            null
        }

    private suspend fun pushLocalDeletions(pass: NotePass) {
        val liveNoteRowsByLocalId = pass.streams
            .flatMap { stream -> stream.rowsByItemId.values.filter { it.isNoteRow() && !it.isDeleted }.map { row -> stream to row } }
            .groupBy { (_, row) -> row.localId }
        if (liveNoteRowsByLocalId.isEmpty()) return
        selfHostDeletedNoteDao.getAllTombstones().forEach { tombstone ->
            val liveRows = liveNoteRowsByLocalId[tombstone.noteId] ?: return@forEach
            if (noteWasMadeAgain(tombstone)) return@forEach
            liveRows.forEach { (stream, row) -> deleteRemovedNote(pass, stream, row, tombstone) }
        }
    }

    private suspend fun noteWasMadeAgain(tombstone: SelfHostDeletedNoteEntity): Boolean {
        if (noteDao.getNoteById(tombstone.noteId) != null) return true
        val dateString = tombstone.dateString
        return tombstone.isDaily && dateString != null && noteDao.getDailyNoteMetadata(ACCOUNT_SPACE_ID, dateString) != null
    }

    private suspend fun deleteRemovedNote(
        pass: NotePass,
        stream: CloudStream,
        row: CloudItemEntity,
        tombstone: SelfHostDeletedNoteEntity
    ) {
        var rowToDelete = row
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            when (val result = apiClient.deleteItem(pass.sessionToken, stream.spaceRef, rowToDelete.itemId, rowToDelete.revision)) {
                is CloudWriteResult.Saved -> {
                    streamRecords.saveRow(
                        stream,
                        rowToDelete.copy(revision = result.revision, syncedUpdatedAt = tombstone.deletedAt, isDeleted = true)
                    )
                    pass.counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict -> {
                    val currentItem = result.currentItem
                    if (currentItem == null) {
                        streamRecords.saveRow(stream, rowToDelete.copy(isDeleted = true))
                        return
                    }
                    checkThisAppCanRead(currentItem)
                    check(currentItem.itemType?.name == rowToDelete.itemType) { "An item changed its type on the server" }
                    if (currentItem.deleted) {
                        applyPulledNoteTombstone(pass, stream, currentItem)
                        return
                    }
                    val remoteOperations = openPulledNote(stream, currentItem)
                    val remoteUpdatedAt = remoteOperations.metadataUpsert.updatedAt
                    if (remoteUpdatedAt > tombstone.deletedAt) {
                        applyPulledLiveNote(pass, stream, currentItem, remoteOperations)
                        selfHostDeletedNoteDao.deleteTombstone(tombstone.noteId)
                        return
                    }
                    rowToDelete = rowForLiveItem(stream, currentItem, rowToDelete.localId, remoteUpdatedAt)
                    streamRecords.saveRow(stream, rowToDelete)
                }
            }
        }
    }

    private suspend fun takeInConflictingNote(
        pass: NotePass,
        stream: CloudStream,
        itemId: String,
        itemType: CloudItemType,
        currentItem: CloudRemoteItem?
    ) {
        if (currentItem == null) {
            stream.rowsByItemId[itemId]?.let { row -> stream.rowsByItemId[itemId] = row.copy(revision = 0L) }
            return
        }
        checkThisAppCanRead(currentItem)
        check(currentItem.itemType == itemType) { "An item changed its type on the server" }
        if (currentItem.deleted) {
            applyPulledNoteTombstone(pass, stream, currentItem)
        } else {
            applyPulledLiveNote(pass, stream, currentItem, openPulledNote(stream, currentItem))
        }
    }

    private fun rowForLiveItem(stream: CloudStream, item: CloudRemoteItem, localId: String, syncedUpdatedAt: Long) =
        CloudItemEntity(
            spaceRef = stream.spaceRef,
            itemId = item.itemId,
            itemType = item.noteItemType().name,
            localId = localId,
            parentId = item.parentId,
            encryptedKey = item.lockedKey?.cipherText?.toBase64(),
            encryptedKeyNonce = item.lockedKey?.nonce?.toBase64(),
            revision = item.revision,
            syncedUpdatedAt = syncedUpdatedAt,
            isDeleted = false
        )

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun unusedNoteIdOrNew(noteId: String): String =
        if (noteDao.getNoteById(noteId) == null) noteId else Uuid.random().toString()

    private fun pickNewerMetadata(local: NoteMetadataEntity?, remote: NoteMetadataEntity): NoteMetadataEntity =
        when {
            local == null -> remote
            remote.updatedAt > local.updatedAt -> remote.withNewerDetailsFrom(local)
            else -> local.withNewerDetailsFrom(remote)
        }

    private fun decodeBlockOrNull(block: NoteBlockEntity): NoteBlock? =
        try {
            blockJson.decodeFromString(NoteBlockSerializer, block.blockDataJson)
        } catch (_: Exception) {
            null
        }

    private fun CloudRemoteItem.noteItemType(): CloudItemType =
        if (itemType == CloudItemType.DAILY) CloudItemType.DAILY else CloudItemType.NOTE

    private fun CloudItemEntity.isNoteRow(): Boolean =
        itemType == CloudItemType.NOTE.name || itemType == CloudItemType.DAILY.name
}
