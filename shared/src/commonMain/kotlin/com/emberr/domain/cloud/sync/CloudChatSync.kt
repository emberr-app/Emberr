package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.ChatSessionDao
import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.entity.ChatSessionEntity
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.domain.ai.chat.ChatSessionMerge
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
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.space.SpaceRepository
import com.emberr.domain.util.sync.ChatSyncEventBus
import kotlinx.serialization.json.Json

private class ChatKeyToSend(val openKey: ByteArray, val lockedKey: CloudEncryptedBytes)

class CloudChatSync(
    private val apiClient: CloudApiClient,
    private val blobTransfer: CloudBlobTransfer,
    private val itemKeyManager: CloudItemKeyManager,
    cloudItemDao: CloudItemDao,
    private val chatSessionDao: ChatSessionDao,
    private val spaceRepository: SpaceRepository
) {

    private val payloadJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val streamRecords = CloudStreamRecords(itemKeyManager, cloudItemDao)

    internal suspend fun applyPulledChats(
        itemsNotYetAppliedByStream: List<Pair<CloudStream, List<CloudRemoteItem>>>,
        counts: SyncPassCounts
    ) {
        itemsNotYetAppliedByStream.forEach { (stream, items) ->
            items.filter { it.itemType == CloudItemType.CHAT }.forEach { item -> applyPulledChat(stream, item, counts) }
        }
    }

    internal suspend fun pushChats(sessionToken: String, streams: List<CloudStream>, counts: SyncPassCounts) {
        val streamsBySpaceId = streams.associateBy { it.appSpaceId }
        chatSessionDao.getAllSessionsIncludingDeleted().forEach { session ->
            val stream = streamsBySpaceId[session.spaceId] ?: return@forEach
            pushChat(sessionToken, stream, session.id, counts)
        }
    }

    private suspend fun applyPulledChat(stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts) {
        if (item.deleted) {
            applyChatTombstone(stream, item, counts)
            return
        }
        val localSession = chatSessionDao.getSession(item.itemId)
        val remoteSession = openPulledChat(stream, item).copy(id = item.itemId, spaceId = stream.appSpaceId)

        when {
            localSession == null -> saveRemoteSession(remoteSession, counts)
            localSession.updatedAt == remoteSession.updatedAt -> Unit
            !localSession.isDeleted && !remoteSession.isDeleted -> {
                val sessionToKeep = ChatSessionMerge.mergeWithServerCopy(localSession, remoteSession)
                if (sessionToKeep != localSession) {
                    val localSessionUnchangedDuringSync = chatSessionDao.getSession(localSession.id) == localSession
                    if (!localSessionUnchangedDuringSync) return
                    spaceRepository.ensureSpaceExists(sessionToKeep.spaceId)
                    chatSessionDao.upsertSession(sessionToKeep)
                    ChatSyncEventBus.emitSessionChanged(sessionToKeep.id)
                    counts.receivedCount++
                }
            }
            localSession.updatedAt > remoteSession.updatedAt -> Unit
            remoteSession.isDeleted -> {
                chatSessionDao.softDeleteSession(remoteSession.id, remoteSession.updatedAt)
                ChatSyncEventBus.emitSessionChanged(remoteSession.id)
                counts.receivedCount++
            }
            else -> saveRemoteSession(remoteSession, counts)
        }
        streamRecords.saveRow(stream, rowForChatItem(stream, item, syncedUpdatedAt = remoteSession.updatedAt))
    }

    private suspend fun saveRemoteSession(remoteSession: ChatSessionEntity, counts: SyncPassCounts) {
        spaceRepository.ensureSpaceExists(remoteSession.spaceId)
        chatSessionDao.upsertSession(remoteSession)
        ChatSyncEventBus.emitSessionChanged(remoteSession.id)
        counts.receivedCount++
    }

    private suspend fun applyChatTombstone(stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts) {
        val tombstoneMillis = tombstoneTimeInMillis(item.updatedAtMicroseconds)
        val localSession = chatSessionDao.getSession(item.itemId)
        if (localSession != null && !localSession.isDeleted &&
            !localEditIsNewerThanTombstone(localSession.updatedAt, item.updatedAtMicroseconds)
        ) {
            chatSessionDao.softDeleteSession(localSession.id, tombstoneMillis)
            ChatSyncEventBus.emitSessionChanged(localSession.id)
            counts.receivedCount++
        }
        streamRecords.saveRow(stream, rowForChatItem(stream, item, syncedUpdatedAt = tombstoneMillis))
    }

    private suspend fun openPulledChat(stream: CloudStream, item: CloudRemoteItem): ChatSessionEntity {
        val downloadUrl = item.downloadUrl ?: throw CloudDecryptionException()
        val blobNonce = item.blobNonce ?: throw CloudDecryptionException()
        val chatKey = streamRecords.openPulledItemKey(stream, item)
        val encryptedChat = CloudEncryptedBytes(blobTransfer.download(downloadUrl), blobNonce)
        val chatJson = itemKeyManager.decryptContent(encryptedChat, chatKey, CloudItemType.CHAT, item.itemId).decodeToString()
        return payloadJson.decodeFromString(ChatSessionEntity.serializer(), chatJson)
    }

    private suspend fun pushChat(sessionToken: String, stream: CloudStream, sessionId: String, counts: SyncPassCounts) {
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val session = chatSessionDao.getSession(sessionId) ?: return
            if (session.spaceId != stream.appSpaceId) return
            val row = stream.rowsByItemId[sessionId]
            if (row != null && session.updatedAt <= row.syncedUpdatedAt) return

            val chatKey = chatKeyToSend(stream, sessionId, row)
            val encryptedChat = itemKeyManager.encryptContent(
                content = payloadJson.encodeToString(ChatSessionEntity.serializer(), session).encodeToByteArray(),
                itemKey = chatKey.openKey,
                itemType = CloudItemType.CHAT,
                itemId = sessionId
            )
            val sizeBytes = encryptedChat.cipherText.size.toLong()
            val uploadUrl = requestUploadUrlUnlessTooLarge(sessionToken, stream, sessionId, sizeBytes) ?: run {
                counts.notSentCount++
                return
            }
            blobTransfer.upload(uploadUrl.uploadUrl, encryptedChat.cipherText)
            val result = apiClient.commitBlobItem(
                sessionToken = sessionToken,
                spaceRef = stream.spaceRef,
                itemId = sessionId,
                baseRevision = row?.revision ?: 0L,
                itemType = CloudItemType.CHAT,
                payloadVersion = CHAT_PAYLOAD_VERSION,
                parentId = stream.appSpaceId,
                lockedKey = chatKey.lockedKey,
                objectKey = uploadUrl.objectKey,
                blobNonce = encryptedChat.nonce,
                sizeBytes = sizeBytes
            )
            when (result) {
                is CloudWriteResult.Saved -> {
                    stream.openKeys[sessionId] = chatKey.openKey
                    streamRecords.saveRow(
                        stream,
                        CloudItemEntity(
                            spaceRef = stream.spaceRef,
                            itemId = sessionId,
                            itemType = CloudItemType.CHAT.name,
                            localId = sessionId,
                            parentId = stream.appSpaceId,
                            encryptedKey = chatKey.lockedKey.cipherText.toBase64(),
                            encryptedKeyNonce = chatKey.lockedKey.nonce.toBase64(),
                            revision = result.revision,
                            syncedUpdatedAt = session.updatedAt,
                            isDeleted = false
                        )
                    )
                    counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict -> takeInConflictingChat(stream, sessionId, result.currentItem, counts)
            }
        }
    }

    private suspend fun chatKeyToSend(stream: CloudStream, sessionId: String, row: CloudItemEntity?): ChatKeyToSend {
        val savedLockedKey = row?.lockedKeyOrNull()
        if (row != null && savedLockedKey != null && row.parentId == stream.appSpaceId) {
            return ChatKeyToSend(openKey = streamRecords.openKeyOf(stream, sessionId), lockedKey = savedLockedKey)
        }
        val newChatKey = itemKeyManager.newItemKey()
        return ChatKeyToSend(
            openKey = newChatKey,
            lockedKey = itemKeyManager.lockChildKey(
                childKey = newChatKey,
                childId = sessionId,
                parentKey = streamRecords.openKeyOf(stream, stream.appSpaceId),
                parentId = stream.appSpaceId
            )
        )
    }

    private suspend fun requestUploadUrlUnlessTooLarge(
        sessionToken: String,
        stream: CloudStream,
        itemId: String,
        sizeBytes: Long
    ): CloudUploadUrl? =
        try {
            apiClient.requestUploadUrl(sessionToken, stream.spaceRef, itemId, sizeBytes)
        } catch (error: CloudApiException) {
            if (error.code != CloudApiErrorCode.REQUEST_TOO_LARGE) throw error
            null
        }

    private suspend fun takeInConflictingChat(
        stream: CloudStream,
        sessionId: String,
        currentItem: CloudRemoteItem?,
        counts: SyncPassCounts
    ) {
        if (currentItem == null) {
            stream.rowsByItemId[sessionId]?.let { row -> stream.rowsByItemId[sessionId] = row.copy(revision = 0L) }
            return
        }
        checkThisAppCanRead(currentItem)
        check(currentItem.itemType == CloudItemType.CHAT) { "An item changed its type on the server" }
        applyPulledChat(stream, currentItem, counts)
    }

    private fun rowForChatItem(stream: CloudStream, item: CloudRemoteItem, syncedUpdatedAt: Long): CloudItemEntity {
        val previousRow = stream.rowsByItemId[item.itemId]
        return CloudItemEntity(
            spaceRef = stream.spaceRef,
            itemId = item.itemId,
            itemType = CloudItemType.CHAT.name,
            localId = item.itemId,
            parentId = item.parentId ?: previousRow?.parentId,
            encryptedKey = item.lockedKey?.cipherText?.toBase64() ?: previousRow?.encryptedKey,
            encryptedKeyNonce = item.lockedKey?.nonce?.toBase64() ?: previousRow?.encryptedKeyNonce,
            revision = item.revision,
            syncedUpdatedAt = syncedUpdatedAt,
            isDeleted = item.deleted
        )
    }
}
