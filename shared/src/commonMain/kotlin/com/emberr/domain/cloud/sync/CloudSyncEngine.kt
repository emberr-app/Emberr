package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CloudSpaceDao
import com.emberr.data.local.room.dao.FolderDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.CloudSpaceEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.PLACEHOLDER_SPACE_UPDATED_AT
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.account.CloudAccountStorage
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudBlobTransferException
import com.emberr.domain.cloud.api.CloudChanges
import com.emberr.domain.cloud.api.CloudRemoteItem
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.CloudSpaceKind
import com.emberr.domain.cloud.api.CloudWriteResult
import com.emberr.domain.cloud.api.fromBase64
import com.emberr.domain.cloud.api.toBase64
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.repository.AccountMergeResult
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.selfhost.merge.mergeFolders
import com.emberr.domain.selfhost.merge.mergeSpaces
import com.emberr.domain.selfhost.sync.SelfHostConnectionState
import com.emberr.domain.selfhost.translation.NotePayloadSyncException
import com.emberr.domain.space.SpaceRepository
import com.emberr.domain.sync.SyncPairingState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.time.Clock

private class PulledChanges(val items: List<CloudRemoteItem>, val latestRevision: Long, val isFullList: Boolean)

private class FolderKeyToSend(val openKey: ByteArray, val lockedKey: CloudEncryptedBytes)

class CloudSyncEngine(
    private val apiClient: CloudApiClient,
    private val accountStorage: CloudAccountStorage,
    private val accountRepository: CloudAccountRepository,
    private val itemKeyManager: CloudItemKeyManager,
    private val cloudSpaceDao: CloudSpaceDao,
    private val cloudItemDao: CloudItemDao,
    private val spaceDao: SpaceDao,
    private val folderDao: FolderDao,
    private val spaceRepository: SpaceRepository,
    private val noteRepository: NoteRepository,
    private val noteSync: CloudNoteSync,
    private val listSync: CloudListSync,
    private val chatSync: CloudChatSync,
    private val localChanges: CloudLocalChanges,
    private val selfHostConnectionState: SelfHostConnectionState,
    private val syncPairingState: SyncPairingState
) {

    private val payloadJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val streamRecords = CloudStreamRecords(itemKeyManager, cloudItemDao)

    private val mutableStatus = MutableStateFlow(CloudSyncStatus())
    val status: StateFlow<CloudSyncStatus> = mutableStatus.asStateFlow()

    @Volatile
    var isPausedUntilAppRestart = false
        private set

    suspend fun syncNow(): CloudSyncResult = runPass(waitForRunningPass = false)

    private suspend fun runPass(waitForRunningPass: Boolean): CloudSyncResult = withContext(Dispatchers.IO) {
        if (anotherSyncMethodIsOn()) return@withContext rememberResult(CloudSyncResult.AnotherSyncMethodIsOn)
        if (waitForRunningPass) {
            CloudSyncPassLock.mutex.lock()
        } else if (!CloudSyncPassLock.mutex.tryLock()) {
            return@withContext CloudSyncResult.Busy
        }
        val passOutcome = try {
            mutableStatus.update { it.copy(isSyncing = true) }
            noteRepository.waitUntilDailyNotesAreInAccount()
            Result.success(syncWhilePassLockHeld())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Result.failure(error)
        } finally {
            CloudSyncPassLock.mutex.unlock()
            mutableStatus.update { it.copy(isSyncing = false) }
        }
        rememberResult(passOutcome.getOrElse { error -> resultForFailedPass(error) })
    }

    suspend fun syncAutomatically(): CloudSyncResult? =
        if (isPausedUntilAppRestart) null else syncNow()

    suspend fun syncAutomaticallyIfThereIsWork(
        waitForRunningPass: Boolean = false,
        beforeSyncing: suspend () -> Unit = {}
    ): CloudSyncResult? =
        withContext(Dispatchers.IO) {
            if (isPausedUntilAppRestart) return@withContext null
            if (anotherSyncMethodIsOn()) return@withContext rememberResult(CloudSyncResult.AnotherSyncMethodIsOn)
            val hasWork = try {
                thereIsWorkToSync()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                return@withContext rememberResult(resultForFailedPass(error))
            }
            if (!hasWork) return@withContext null
            beforeSyncing()
            runPass(waitForRunningPass)
        }

    fun forgetStatus() {
        mutableStatus.value = CloudSyncStatus()
    }

    private fun anotherSyncMethodIsOn(): Boolean =
        selfHostConnectionState.isConnected.value || syncPairingState.isPaired.value

    private suspend fun thereIsWorkToSync(): Boolean {
        val sessionToken = accountStorage.getSessionToken() ?: return false
        val latestRevisionsBySpaceId = apiClient.fetchSyncState(sessionToken)
            .associate { remoteSpace -> remoteSpace.clientSpaceId to remoteSpace.latestRevision }
        val savedSpaces = cloudSpaceDao.getAllSpaces()
        return cloudSyncHasWork(
            latestRevisionsBySpaceId = latestRevisionsBySpaceId,
            lastRevisionsSeenBySpaceId = savedSpaces.associate { it.appSpaceId to it.lastRevisionSeen },
            localChangesAreWaiting = localChanges.areWaiting(savedSpaces)
        )
    }

    private fun rememberResult(result: CloudSyncResult): CloudSyncResult {
        if (result == CloudSyncResult.UpdateRequired) isPausedUntilAppRestart = true
        mutableStatus.update { status ->
            if (result is CloudSyncResult.Done) {
                status.copy(
                    lastSyncedAtMillis = Clock.System.now().toEpochMilliseconds(),
                    lastProblem = result.takeIf { it.notSentCount > 0 }
                )
            } else {
                status.copy(lastProblem = result)
            }
        }
        return result
    }

    private suspend fun resultForFailedPass(error: Throwable): CloudSyncResult = when (error) {
        is CloudApiException -> when (error.code) {
            CloudApiErrorCode.INVALID_TOKEN -> try {
                accountRepository.clearLocalAccount()
                CloudSyncResult.NotSignedIn
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                CloudSyncResult.Failed("You were signed out, but this device's sync records could not be cleared.")
            }
            CloudApiErrorCode.UPGRADE_REQUIRED -> CloudSyncResult.UpdateRequired
            else -> CloudSyncResult.Failed("Emberr Cloud refused the sync (${error.code}).")
        }
        is CloudServerUnreachableException -> CloudSyncResult.Failed("Can't reach the Emberr Cloud server.")
        is CloudBlobTransferException ->
            CloudSyncResult.Failed("A storage link expired or was refused (HTTP ${error.httpStatus}). Try again.")
        is CloudUpdateRequiredException -> CloudSyncResult.UpdateRequired
        is CloudDecryptionException, is CloudKeyNotFoundException ->
            CloudSyncResult.Failed("Some synced data could not be decrypted.")
        is NotePayloadSyncException -> CloudSyncResult.Failed("A synced note could not be read.")
        is CloudLocalDataBusyException -> CloudSyncResult.Busy
        else -> CloudSyncResult.Failed("Something went wrong while syncing.")
    }

    private suspend fun syncWhilePassLockHeld(): CloudSyncResult {
        val sessionToken = accountStorage.getSessionToken() ?: return CloudSyncResult.NotSignedIn
        val masterKey = accountStorage.getMasterKey() ?: return CloudSyncResult.NotSignedIn
        val counts = SyncPassCounts()

        val streams = registerAndOpenStreams(sessionToken, masterKey)
        val pulledChangesByStream = streams.map { stream -> stream to fetchEveryChangedItem(sessionToken, stream) }
        val pulledItemsByStream = pulledChangesByStream.map { (stream, pulledChanges) ->
            val prunedTombstones = if (pulledChanges.isFullList) {
                tombstonesPrunedFromFullList(stream.rowsByItemId.values, pulledChanges.items)
            } else {
                emptyList()
            }
            stream to (pulledChanges.items + prunedTombstones)
        }
        pulledItemsByStream.forEach { (_, items) -> items.forEach(::checkThisAppCanRead) }
        val itemsNotYetAppliedByStream = pulledItemsByStream.map { (stream, items) ->
            stream to items.filter { item -> item.revision != stream.rowsByItemId[item.itemId]?.revision }
        }
        itemsNotYetAppliedByStream.forEach { (stream, items) -> applyPulledSpaceAndFolders(stream, items, counts) }
        moveActiveSpaceIfSpacesChanged(counts)
        noteSync.applyPulledNotes(sessionToken, streams, itemsNotYetAppliedByStream, counts)
        val listPass = listSync.applyPulledLists(itemsNotYetAppliedByStream, counts)
        chatSync.applyPulledChats(itemsNotYetAppliedByStream, counts)
        pulledChangesByStream.forEach { (stream, pulledChanges) ->
            cloudSpaceDao.updateLastRevisionSeen(stream.appSpaceId, pulledChanges.latestRevision)
        }

        val accountMerge = mergeDailyNotesIntoAccount()
        streams.filterNot { it.isAccountStream }.forEach { stream -> pushStream(sessionToken, stream, counts) }
        noteSync.pushNotes(sessionToken, streams, counts)
        listSync.pushLists(
            sessionToken = sessionToken,
            streams = streams,
            pass = listPass,
            categoriesChangedByAccountMerge = accountMerge?.categoriesChanged == true,
            counts = counts
        )
        chatSync.pushChats(sessionToken, streams, counts)
        moveActiveSpaceIfSpacesChanged(counts)

        return CloudSyncResult.Done(
            receivedCount = counts.receivedCount,
            sentCount = counts.sentCount,
            notSentCount = counts.notSentCount
        )
    }

    private suspend fun mergeDailyNotesIntoAccount(): AccountMergeResult? =
        try {
            withLocalDataLock { noteRepository.mergeDailyNotesAndCategoriesIntoAccountWhileSyncLockHeld() }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

    private suspend fun moveActiveSpaceIfSpacesChanged(counts: SyncPassCounts) {
        if (!counts.spacesChanged) return
        withLocalDataLock { spaceRepository.moveActiveSpaceIfItNoLongerExists() }
        counts.spacesChanged = false
    }

    private suspend fun registerAndOpenStreams(sessionToken: String, masterKey: ByteArray): List<CloudStream> {
        val registeredSpaceIds = cloudSpaceDao.getAllSpaces().map { it.appSpaceId }.toMutableSet()
        val spaceIdsToRegister = spaceDao.getAllSpacesOnce().map { it.spaceId }.filter { it != ACCOUNT_SPACE_ID } + ACCOUNT_SPACE_ID
        spaceIdsToRegister.filterNot { it in registeredSpaceIds }.forEach { appSpaceId ->
            registerSpace(sessionToken, masterKey, appSpaceId)
            registeredSpaceIds += appSpaceId
        }

        apiClient.fetchSyncState(sessionToken).forEach { remoteSpace ->
            if (remoteSpace.kind == null) throw CloudUpdateRequiredException()
            if (remoteSpace.clientSpaceId !in registeredSpaceIds) {
                cloudSpaceDao.upsertSpace(
                    CloudSpaceEntity(
                        appSpaceId = remoteSpace.clientSpaceId,
                        spaceRef = remoteSpace.spaceRef,
                        encryptedKey = remoteSpace.lockedSpaceKey.cipherText.toBase64(),
                        encryptedKeyNonce = remoteSpace.lockedSpaceKey.nonce.toBase64()
                    )
                )
                registeredSpaceIds += remoteSpace.clientSpaceId
            }
        }

        return cloudSpaceDao.getAllSpaces().map { savedSpace -> openStream(savedSpace, masterKey) }
    }

    private suspend fun registerSpace(sessionToken: String, masterKey: ByteArray, appSpaceId: String) {
        val newSpaceKey = itemKeyManager.newItemKey()
        val lockedSpaceKey = itemKeyManager.lockChildKey(
            childKey = newSpaceKey,
            childId = appSpaceId,
            parentKey = masterKey,
            parentId = CloudItemKeyManager.MASTER_KEY_PARENT_ID
        )
        val kind = if (appSpaceId == ACCOUNT_SPACE_ID) CloudSpaceKind.ACCOUNT else CloudSpaceKind.SPACE
        val registeredSpace = apiClient.registerSpace(sessionToken, appSpaceId, kind, lockedSpaceKey)
        cloudSpaceDao.upsertSpace(
            CloudSpaceEntity(
                appSpaceId = appSpaceId,
                spaceRef = registeredSpace.spaceRef,
                encryptedKey = registeredSpace.lockedSpaceKey.cipherText.toBase64(),
                encryptedKeyNonce = registeredSpace.lockedSpaceKey.nonce.toBase64()
            )
        )
    }

    private suspend fun openStream(savedSpace: CloudSpaceEntity, masterKey: ByteArray): CloudStream {
        val spaceKey = itemKeyManager.unlockChildKey(
            lockedChildKey = CloudEncryptedBytes(
                savedSpace.encryptedKey.fromBase64(),
                savedSpace.encryptedKeyNonce.fromBase64()
            ),
            childId = savedSpace.appSpaceId,
            parentKey = masterKey,
            parentId = CloudItemKeyManager.MASTER_KEY_PARENT_ID
        )
        return CloudStream(
            appSpaceId = savedSpace.appSpaceId,
            spaceRef = savedSpace.spaceRef,
            lastRevisionSeen = savedSpace.lastRevisionSeen,
            rowsByItemId = cloudItemDao.getItemsInSpace(savedSpace.spaceRef).associateBy { it.itemId }.toMutableMap(),
            openKeys = mutableMapOf(savedSpace.appSpaceId to spaceKey)
        )
    }

    private suspend fun applyPulledSpaceAndFolders(stream: CloudStream, itemsNotYetApplied: List<CloudRemoteItem>, counts: SyncPassCounts) {
        if (stream.isAccountStream) return
        itemsNotYetApplied
            .filter { it.itemType == CloudItemType.SPACE && it.itemId == stream.appSpaceId }
            .forEach { item -> applyPulledSpace(stream, item, counts) }
        parentsBeforeChildren(
            items = itemsNotYetApplied.filter { it.itemType == CloudItemType.FOLDER },
            idOf = { it.itemId },
            parentIdOf = { it.parentId }
        ).forEach { item -> applyPulledFolder(stream, item, counts) }
    }

    private suspend fun fetchEveryChangedItem(sessionToken: String, stream: CloudStream): PulledChanges {
        val newestItemsById = LinkedHashMap<String, CloudRemoteItem>()
        var since = stream.lastRevisionSeen
        var isFullList = false
        while (true) {
            when (val changes = apiClient.fetchChanges(sessionToken, stream.spaceRef, since)) {
                is CloudChanges.ResyncRequired -> {
                    newestItemsById.clear()
                    since = 0L
                    isFullList = true
                }
                is CloudChanges.Page -> {
                    changes.items.forEach { item ->
                        val alreadyPulled = newestItemsById[item.itemId]
                        if (alreadyPulled == null || item.revision > alreadyPulled.revision) {
                            newestItemsById[item.itemId] = item
                        }
                    }
                    if (!changes.hasMore) return PulledChanges(newestItemsById.values.toList(), changes.latest, isFullList)
                    since = changes.latest
                }
            }
        }
    }

    private suspend fun applyPulledSpace(stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts) = withLocalDataLock {
        val localSpace = spaceDao.getSpaceById(stream.appSpaceId)
        val syncedUpdatedAt = if (item.deleted) {
            applySpaceTombstone(localSpace, item, counts)
        } else {
            val payload = decryptPayload(item, CloudItemType.SPACE, streamRecords.openKeyOf(stream, stream.appSpaceId))
            val remoteSpace = payloadJson.decodeFromString(SpaceEntity.serializer(), payload).copy(spaceId = stream.appSpaceId)
            val mergedSpace = mergeSpaces(listOfNotNull(localSpace), listOf(remoteSpace)).single()
            if (mergedSpace != localSpace) {
                spaceDao.insertOrUpdateSpace(mergedSpace)
                counts.receivedCount++
                counts.spacesChanged = true
            }
            remoteSpace.updatedAt
        }
        streamRecords.saveRow(stream, rowFromRemoteItem(stream, item, CloudItemType.SPACE, syncedUpdatedAt))
    }

    private suspend fun applySpaceTombstone(localSpace: SpaceEntity?, item: CloudRemoteItem, counts: SyncPassCounts): Long {
        val tombstoneMillis = tombstoneTimeInMillis(item.updatedAtMicroseconds)
        if (localSpace != null && !localSpace.isDeleted &&
            !localEditIsNewerThanTombstone(localSpace.updatedAt, item.updatedAtMicroseconds)
        ) {
            spaceDao.markSpaceDeleted(localSpace.spaceId, tombstoneMillis)
            counts.receivedCount++
            counts.spacesChanged = true
        }
        return tombstoneMillis
    }

    private suspend fun applyPulledFolder(stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts) = withLocalDataLock {
        val localFolder = folderDao.getFolderById(item.itemId)
        val syncedUpdatedAt = if (item.deleted) {
            applyFolderTombstone(localFolder, item, counts)
        } else {
            val payload = decryptPayload(item, CloudItemType.FOLDER, streamRecords.openPulledItemKey(stream, item))
            val remoteFolder = payloadJson.decodeFromString(FolderEntity.serializer(), payload)
                .copy(folderId = item.itemId, spaceId = stream.appSpaceId)
            val mergedFolder = mergeFolders(listOfNotNull(localFolder), listOf(remoteFolder)).single()
            if (mergedFolder != localFolder) {
                spaceRepository.ensureSpaceExists(mergedFolder.spaceId)
                folderDao.insertFolder(mergedFolder)
                counts.receivedCount++
            }
            remoteFolder.updatedAt
        }
        streamRecords.saveRow(stream, rowFromRemoteItem(stream, item, CloudItemType.FOLDER, syncedUpdatedAt))
    }

    private suspend fun applyFolderTombstone(localFolder: FolderEntity?, item: CloudRemoteItem, counts: SyncPassCounts): Long {
        val tombstoneMillis = tombstoneTimeInMillis(item.updatedAtMicroseconds)
        if (localFolder != null && !localFolder.isDeleted &&
            !localEditIsNewerThanTombstone(localFolder.updatedAt, item.updatedAtMicroseconds)
        ) {
            folderDao.markFolderDeleted(localFolder.folderId, tombstoneMillis)
            counts.receivedCount++
        }
        return tombstoneMillis
    }

    private suspend fun decryptPayload(item: CloudRemoteItem, itemType: CloudItemType, itemKey: ByteArray): String {
        val encryptedPayload = item.inlinePayload ?: throw CloudDecryptionException()
        return itemKeyManager.decryptContent(encryptedPayload, itemKey, itemType, item.itemId).decodeToString()
    }

    private fun rowFromRemoteItem(
        stream: CloudStream,
        item: CloudRemoteItem,
        itemType: CloudItemType,
        syncedUpdatedAt: Long
    ): CloudItemEntity {
        val previousRow = stream.rowsByItemId[item.itemId]
        val lockedKey = item.lockedKey
        return CloudItemEntity(
            spaceRef = stream.spaceRef,
            itemId = item.itemId,
            itemType = itemType.name,
            localId = item.itemId,
            parentId = item.parentId ?: previousRow?.parentId,
            encryptedKey = lockedKey?.cipherText?.toBase64() ?: previousRow?.encryptedKey,
            encryptedKeyNonce = lockedKey?.nonce?.toBase64() ?: previousRow?.encryptedKeyNonce,
            revision = item.revision,
            syncedUpdatedAt = syncedUpdatedAt,
            isDeleted = item.deleted
        )
    }

    private suspend fun pushStream(sessionToken: String, stream: CloudStream, counts: SyncPassCounts) {
        pushSpace(sessionToken, stream, counts)
        val changedFolders = folderDao.getFoldersModifiedSince(0L).filter { folder ->
            folder.spaceId == stream.appSpaceId && needsPush(folder.updatedAt, stream.rowsByItemId[folder.folderId])
        }
        parentsBeforeChildren(changedFolders, idOf = { it.folderId }, parentIdOf = { it.parentFolderId })
            .forEach { folder -> pushFolder(sessionToken, stream, folder.folderId, counts) }
    }

    private fun needsPush(localUpdatedAt: Long, row: CloudItemEntity?): Boolean =
        row == null || localUpdatedAt > row.syncedUpdatedAt

    private suspend fun pushSpace(sessionToken: String, stream: CloudStream, counts: SyncPassCounts) {
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val space = spaceDao.getSpaceById(stream.appSpaceId) ?: return
            val row = stream.rowsByItemId[stream.appSpaceId]
            if (space.updatedAt == PLACEHOLDER_SPACE_UPDATED_AT || !needsPush(space.updatedAt, row)) return

            val encryptedPayload = itemKeyManager.encryptContent(
                content = payloadJson.encodeToString(SpaceEntity.serializer(), space).encodeToByteArray(),
                itemKey = streamRecords.openKeyOf(stream, stream.appSpaceId),
                itemType = CloudItemType.SPACE,
                itemId = stream.appSpaceId
            )
            val result = apiClient.commitInlineItem(
                sessionToken = sessionToken,
                spaceRef = stream.spaceRef,
                itemId = stream.appSpaceId,
                baseRevision = row?.revision ?: 0L,
                itemType = CloudItemType.SPACE,
                payloadVersion = SPACE_AND_FOLDER_PAYLOAD_VERSION,
                parentId = null,
                lockedKey = null,
                encryptedPayload = encryptedPayload
            )
            when (result) {
                is CloudWriteResult.Saved -> {
                    streamRecords.saveRow(
                        stream,
                        CloudItemEntity(
                            spaceRef = stream.spaceRef,
                            itemId = stream.appSpaceId,
                            itemType = CloudItemType.SPACE.name,
                            localId = stream.appSpaceId,
                            parentId = null,
                            encryptedKey = null,
                            encryptedKeyNonce = null,
                            revision = result.revision,
                            syncedUpdatedAt = space.updatedAt,
                            isDeleted = false
                        )
                    )
                    counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict ->
                    takeInConflictingItem(stream, stream.appSpaceId, CloudItemType.SPACE, result.currentItem, counts)
            }
        }
    }

    private suspend fun pushFolder(sessionToken: String, stream: CloudStream, folderId: String, counts: SyncPassCounts) {
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val folder = folderDao.getFolderById(folderId) ?: return
            val row = stream.rowsByItemId[folderId]
            if (!needsPush(folder.updatedAt, row)) return
            val keyParentId = keyParentIdFor(stream, folder) ?: return

            val folderKey = folderKeyToSend(stream, folderId, row, keyParentId)
            val encryptedPayload = itemKeyManager.encryptContent(
                content = payloadJson.encodeToString(FolderEntity.serializer(), folder).encodeToByteArray(),
                itemKey = folderKey.openKey,
                itemType = CloudItemType.FOLDER,
                itemId = folderId
            )
            val result = apiClient.commitInlineItem(
                sessionToken = sessionToken,
                spaceRef = stream.spaceRef,
                itemId = folderId,
                baseRevision = row?.revision ?: 0L,
                itemType = CloudItemType.FOLDER,
                payloadVersion = SPACE_AND_FOLDER_PAYLOAD_VERSION,
                parentId = keyParentId,
                lockedKey = folderKey.lockedKey,
                encryptedPayload = encryptedPayload
            )
            when (result) {
                is CloudWriteResult.Saved -> {
                    stream.openKeys[folderId] = folderKey.openKey
                    streamRecords.saveRow(
                        stream,
                        CloudItemEntity(
                            spaceRef = stream.spaceRef,
                            itemId = folderId,
                            itemType = CloudItemType.FOLDER.name,
                            localId = folderId,
                            parentId = keyParentId,
                            encryptedKey = folderKey.lockedKey.cipherText.toBase64(),
                            encryptedKeyNonce = folderKey.lockedKey.nonce.toBase64(),
                            revision = result.revision,
                            syncedUpdatedAt = folder.updatedAt,
                            isDeleted = false
                        )
                    )
                    counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict ->
                    takeInConflictingItem(stream, folderId, CloudItemType.FOLDER, result.currentItem, counts)
            }
        }
    }

    private suspend fun keyParentIdFor(stream: CloudStream, folder: FolderEntity): String? {
        val parentFolderId = folder.parentFolderId ?: return stream.appSpaceId
        val parentFolder = folderDao.getFolderById(parentFolderId)
        if (parentFolder == null || parentFolder.spaceId != stream.appSpaceId) return stream.appSpaceId
        val parentRow = stream.rowsByItemId[parentFolderId]
        return when {
            parentRow?.isDeleted == true -> stream.appSpaceId
            parentRow?.encryptedKey == null -> null
            else -> parentFolderId
        }
    }

    private suspend fun folderKeyToSend(
        stream: CloudStream,
        folderId: String,
        row: CloudItemEntity?,
        keyParentId: String
    ): FolderKeyToSend {
        val keyParentKey = streamRecords.openKeyOf(stream, keyParentId)
        val savedLockedKey = row?.lockedKeyOrNull()
        val savedParentId = row?.parentId
        if (savedLockedKey == null || savedParentId == null) {
            val newFolderKey = itemKeyManager.newItemKey()
            return FolderKeyToSend(
                openKey = newFolderKey,
                lockedKey = itemKeyManager.lockChildKey(newFolderKey, folderId, keyParentKey, keyParentId)
            )
        }
        val lockedKey = if (savedParentId == keyParentId) {
            savedLockedKey
        } else {
            itemKeyManager.relockChildKeyForMove(
                lockedChildKey = savedLockedKey,
                childId = folderId,
                oldParentKey = streamRecords.openKeyOf(stream, savedParentId),
                oldParentId = savedParentId,
                newParentKey = keyParentKey,
                newParentId = keyParentId
            )
        }
        return FolderKeyToSend(openKey = streamRecords.openKeyOf(stream, folderId), lockedKey = lockedKey)
    }

    private suspend fun takeInConflictingItem(
        stream: CloudStream,
        itemId: String,
        itemType: CloudItemType,
        currentItem: CloudRemoteItem?,
        counts: SyncPassCounts
    ) {
        if (currentItem == null) {
            stream.rowsByItemId[itemId]?.let { row -> stream.rowsByItemId[itemId] = row.copy(revision = 0L) }
            return
        }
        checkThisAppCanRead(currentItem)
        check(currentItem.itemType == itemType) { "An item changed its type on the server" }
        when (itemType) {
            CloudItemType.SPACE -> applyPulledSpace(stream, currentItem, counts)
            else -> applyPulledFolder(stream, currentItem, counts)
        }
    }
}
