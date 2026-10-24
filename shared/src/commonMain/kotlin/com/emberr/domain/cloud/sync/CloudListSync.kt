package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.CalendarEventExceptionDao
import com.emberr.data.local.room.dao.CategoryDao
import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CustomPropertyDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.PropertyTagDao
import com.emberr.data.local.room.dao.SelfHostDeletedApiConfigDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.ai.external.ExternalAiProvider
import com.emberr.domain.ai.external.ExternalAiProviderConfig
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudRemoteItem
import com.emberr.domain.cloud.api.CloudWriteResult
import com.emberr.domain.cloud.api.toBase64
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.repository.BookmarkCategoryOrderStore
import com.emberr.domain.repository.FavoriteNoteOrderStore
import com.emberr.domain.selfhost.merge.mergeApiConfig
import com.emberr.domain.selfhost.merge.mergeCategories
import com.emberr.domain.selfhost.merge.mergeCustomProperties
import com.emberr.domain.selfhost.merge.mergeEventExceptions
import com.emberr.domain.selfhost.merge.mergePropertyTags
import com.emberr.domain.selfhost.sync.ApiConfigSyncEntry
import com.emberr.domain.space.SpaceRepository
import kotlinx.serialization.json.Json

internal class ListPass {
    val pulledCopiesBySpaceId = HashMap<String, CloudListsPayload>()
    var exceptionsByStream: Map<String, List<CalendarEventExceptionEntity>>? = null
}

private class ListKeyToSend(val openKey: ByteArray, val lockedKey: CloudEncryptedBytes)

class CloudListSync(
    private val apiClient: CloudApiClient,
    private val itemKeyManager: CloudItemKeyManager,
    cloudItemDao: CloudItemDao,
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val categoryDao: CategoryDao,
    private val propertyTagDao: PropertyTagDao,
    private val customPropertyDao: CustomPropertyDao,
    private val calendarEventExceptionDao: CalendarEventExceptionDao,
    private val selfHostDeletedApiConfigDao: SelfHostDeletedApiConfigDao,
    private val aiSettingsRepository: AiSettingsRepository,
    private val bookmarkCategoryOrderStore: BookmarkCategoryOrderStore,
    private val favoriteNoteOrderStore: FavoriteNoteOrderStore,
    private val spaceRepository: SpaceRepository
) {

    private val payloadJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val streamRecords = CloudStreamRecords(itemKeyManager, cloudItemDao)

    internal suspend fun applyPulledLists(
        itemsNotYetAppliedByStream: List<Pair<CloudStream, List<CloudRemoteItem>>>,
        counts: SyncPassCounts
    ): ListPass {
        val pass = ListPass()
        itemsNotYetAppliedByStream.forEach { (stream, items) ->
            items.filter { item -> isListItemOf(stream, item) }.forEach { item -> applyPulledList(pass, stream, item, counts) }
        }
        return pass
    }

    internal suspend fun pushLists(
        sessionToken: String,
        streams: List<CloudStream>,
        pass: ListPass,
        categoriesChangedByAccountMerge: Boolean,
        counts: SyncPassCounts
    ) {
        streams.forEach { stream ->
            pushList(
                sessionToken = sessionToken,
                stream = stream,
                pass = pass,
                mustSendChanges = stream.isAccountStream && categoriesChangedByAccountMerge,
                counts = counts
            )
        }
    }

    private fun listItemIdOf(stream: CloudStream): String =
        if (stream.isAccountStream) ACCOUNT_LISTS_ITEM_ID else SPACE_LISTS_ITEM_ID

    private fun listItemTypeOf(stream: CloudStream): CloudItemType =
        if (stream.isAccountStream) CloudItemType.ACCOUNT_LISTS else CloudItemType.SPACE_LISTS

    private fun isListItemOf(stream: CloudStream, item: CloudRemoteItem): Boolean =
        item.itemType == listItemTypeOf(stream) && item.itemId == listItemIdOf(stream)

    private suspend fun applyPulledList(pass: ListPass, stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts) = withLocalDataLock {
        val syncedUpdatedAt = if (item.deleted) {
            pass.pulledCopiesBySpaceId.remove(stream.appSpaceId)
            applyListTombstone(stream, item, counts)
        } else {
            val pulledCopy = openPulledList(stream, item)
            pass.pulledCopiesBySpaceId[stream.appSpaceId] = pulledCopy
            if (mergeIntoThisDevice(pulledCopy)) counts.receivedCount++
            pulledCopy.newestEntryTime()
        }
        val previousRow = stream.rowsByItemId[item.itemId]
        streamRecords.saveRow(
            stream,
            CloudItemEntity(
                spaceRef = stream.spaceRef,
                itemId = item.itemId,
                itemType = listItemTypeOf(stream).name,
                localId = item.itemId,
                parentId = item.parentId ?: previousRow?.parentId,
                encryptedKey = item.lockedKey?.cipherText?.toBase64() ?: previousRow?.encryptedKey,
                encryptedKeyNonce = item.lockedKey?.nonce?.toBase64() ?: previousRow?.encryptedKeyNonce,
                revision = item.revision,
                syncedUpdatedAt = syncedUpdatedAt,
                isDeleted = item.deleted
            )
        )
    }

    private suspend fun openPulledList(stream: CloudStream, item: CloudRemoteItem): CloudListsPayload {
        val encryptedPayload = item.inlinePayload ?: throw CloudDecryptionException()
        val listKey = streamRecords.openPulledItemKey(stream, item)
        val listJson = itemKeyManager.decryptContent(encryptedPayload, listKey, listItemTypeOf(stream), item.itemId).decodeToString()
        val pulledList = payloadJson.decodeFromString(CloudListsPayload.serializer(), listJson)
        val propertyTags = pulledList.propertyTags.map { it.copy(spaceId = stream.appSpaceId) }
        val customProperties = pulledList.customProperties.map { it.copy(spaceId = stream.appSpaceId) }
        if (!stream.isAccountStream) {
            return CloudListsPayload(
                propertyTags = propertyTags,
                customProperties = customProperties,
                eventExceptions = pulledList.eventExceptions
            )
        }
        return pulledList.copy(
            categories = pulledList.categories.map { it.copy(spaceId = ACCOUNT_SPACE_ID) },
            propertyTags = propertyTags,
            customProperties = customProperties
        )
    }

    private suspend fun mergeIntoThisDevice(pulledList: CloudListsPayload): Boolean {
        val localCategories = categoryDao.getCategoriesModifiedSince(0L)
        val changedCategories = mergeCategories(localCategories, pulledList.categories) - localCategories.toSet()
        changedCategories.forEach { spaceRepository.ensureSpaceExists(it.spaceId) }
        changedCategories.forEach { categoryDao.insertOrUpdateCategory(it) }

        val localTags = propertyTagDao.getTagsModifiedSince(0L)
        val changedTags = mergePropertyTags(localTags, pulledList.propertyTags) - localTags.toSet()
        changedTags.forEach { spaceRepository.ensureSpaceExists(it.spaceId) }
        changedTags.forEach { propertyTagDao.insertOrUpdateTag(it) }

        val localProperties = customPropertyDao.getPropertiesModifiedSince(0L)
        val changedProperties = mergeCustomProperties(localProperties, pulledList.customProperties) - localProperties.toSet()
        changedProperties.forEach { spaceRepository.ensureSpaceExists(it.spaceId) }
        changedProperties.forEach { customPropertyDao.insertOrUpdateProperty(it) }

        val localExceptions = calendarEventExceptionDao.getExceptionsModifiedSince(0L)
        val changedExceptions = mergeEventExceptions(localExceptions, pulledList.eventExceptions) - localExceptions.toSet()
        changedExceptions.forEach { calendarEventExceptionDao.upsert(it) }

        val apiConfigsChanged = applyPulledApiConfigs(pulledList.apiConfigs)
        val bookmarkOrdersChanged = bookmarkCategoryOrderStore.applyRemoteOrders(pulledList.bookmarkCategoryOrders)
        val favoriteOrdersChanged = favoriteNoteOrderStore.applyRemoteOrders(pulledList.favoriteNoteOrders)

        return changedCategories.isNotEmpty() || changedTags.isNotEmpty() || changedProperties.isNotEmpty() ||
            changedExceptions.isNotEmpty() || apiConfigsChanged || bookmarkOrdersChanged || favoriteOrdersChanged
    }

    private suspend fun applyPulledApiConfigs(pulledEntries: List<ApiConfigSyncEntry>): Boolean {
        if (pulledEntries.isEmpty()) return false
        val pulledEntriesByProvider = pulledEntries.associateBy { it.provider }
        val localTombstonesByProvider = selfHostDeletedApiConfigDao.getAllTombstones().associateBy { it.provider }
        var changedAnything = false
        for (provider in ExternalAiProvider.entries) {
            val pulledEntry = pulledEntriesByProvider[provider.name] ?: continue
            val entryToApply = mergeApiConfig(
                provider = provider,
                localConfig = aiSettingsRepository.getProviderConfig(provider),
                localTombstone = localTombstonesByProvider[provider.name],
                remoteEntry = pulledEntry
            ).remoteEntryToApply ?: continue
            if (entryToApply.isDeleted) {
                aiSettingsRepository.applyRemoteProviderConfigDeletion(provider, entryToApply.updatedAt)
            } else {
                aiSettingsRepository.saveProviderConfig(
                    provider,
                    ExternalAiProviderConfig(
                        apiKey = entryToApply.apiKey,
                        model = entryToApply.model,
                        baseUrl = entryToApply.baseUrl,
                        updatedAt = entryToApply.updatedAt
                    )
                )
            }
            changedAnything = true
        }
        return changedAnything
    }

    private suspend fun applyListTombstone(stream: CloudStream, item: CloudRemoteItem, counts: SyncPassCounts): Long {
        val tombstoneMillis = tombstoneTimeInMillis(item.updatedAtMicroseconds)
        fun isNotNewerThanTombstone(updatedAt: Long) = !localEditIsNewerThanTombstone(updatedAt, item.updatedAtMicroseconds)

        val localList = buildListOf(stream, exceptionsByStream = emptyMap())
        val categoriesToDelete = localList.categories.filter { !it.isDeleted && isNotNewerThanTombstone(it.updatedAt) }
        categoriesToDelete.forEach { categoryDao.markCategoryDeleted(it.categoryId, tombstoneMillis) }
        val tagsToDelete = localList.propertyTags.filter { !it.isDeleted && isNotNewerThanTombstone(it.updatedAt) }
        tagsToDelete.forEach { propertyTagDao.insertOrUpdateTag(it.copy(isDeleted = true, updatedAt = tombstoneMillis)) }
        val propertiesToDelete = localList.customProperties.filter { !it.isDeleted && isNotNewerThanTombstone(it.updatedAt) }
        propertiesToDelete.forEach { customPropertyDao.insertOrUpdateProperty(it.copy(isDeleted = true, updatedAt = tombstoneMillis)) }
        val apiConfigsToDelete = localList.apiConfigs.filter { !it.isDeleted && isNotNewerThanTombstone(it.updatedAt) }
        apiConfigsToDelete.forEach { entry ->
            aiSettingsRepository.applyRemoteProviderConfigDeletion(ExternalAiProvider.valueOf(entry.provider), tombstoneMillis)
        }

        val deletedAnything = categoriesToDelete.isNotEmpty() || tagsToDelete.isNotEmpty() ||
            propertiesToDelete.isNotEmpty() || apiConfigsToDelete.isNotEmpty()
        if (deletedAnything) counts.receivedCount++
        return tombstoneMillis
    }

    private suspend fun pushList(
        sessionToken: String,
        stream: CloudStream,
        pass: ListPass,
        mustSendChanges: Boolean,
        counts: SyncPassCounts
    ) {
        val itemId = listItemIdOf(stream)
        val itemType = listItemTypeOf(stream)
        repeat(MAX_COMMIT_TRIES_PER_ITEM) {
            val row = stream.rowsByItemId[itemId]
            val pulledCopy = pass.pulledCopiesBySpaceId[stream.appSpaceId]
            val builtList = withLocalDataLock { buildListOf(stream, routedEventExceptions(pass)) }
            val listToSend = if (pulledCopy == null) {
                builtList
            } else {
                builtList.copy(eventExceptions = mergeEventExceptions(builtList.eventExceptions, pulledCopy.eventExceptions))
            }
            if (!listNeedsPush(listToSend, pulledCopy, row, mustSendChanges)) return

            val listKey = listKeyToSend(stream, itemId, row)
            val encryptedPayload = itemKeyManager.encryptContent(
                content = payloadJson.encodeToString(CloudListsPayload.serializer(), listToSend).encodeToByteArray(),
                itemKey = listKey.openKey,
                itemType = itemType,
                itemId = itemId
            )
            val result = try {
                apiClient.commitInlineItem(
                    sessionToken = sessionToken,
                    spaceRef = stream.spaceRef,
                    itemId = itemId,
                    baseRevision = row?.revision ?: 0L,
                    itemType = itemType,
                    payloadVersion = LISTS_PAYLOAD_VERSION,
                    parentId = stream.appSpaceId,
                    lockedKey = listKey.lockedKey,
                    encryptedPayload = encryptedPayload
                )
            } catch (error: CloudApiException) {
                if (error.code != CloudApiErrorCode.REQUEST_TOO_LARGE) throw error
                counts.notSentCount++
                return
            }
            when (result) {
                is CloudWriteResult.Saved -> {
                    stream.openKeys[itemId] = listKey.openKey
                    streamRecords.saveRow(
                        stream,
                        CloudItemEntity(
                            spaceRef = stream.spaceRef,
                            itemId = itemId,
                            itemType = itemType.name,
                            localId = itemId,
                            parentId = stream.appSpaceId,
                            encryptedKey = listKey.lockedKey.cipherText.toBase64(),
                            encryptedKeyNonce = listKey.lockedKey.nonce.toBase64(),
                            revision = result.revision,
                            syncedUpdatedAt = listToSend.newestEntryTime(),
                            isDeleted = false
                        )
                    )
                    counts.sentCount++
                    return
                }
                is CloudWriteResult.Conflict -> takeInConflictingList(pass, stream, itemId, result.currentItem, counts)
            }
        }
    }

    private suspend fun buildListOf(
        stream: CloudStream,
        exceptionsByStream: Map<String, List<CalendarEventExceptionEntity>>
    ): CloudListsPayload {
        val propertyTags = propertyTagDao.getTagsModifiedSince(0L).filter { it.spaceId == stream.appSpaceId }
        val customProperties = customPropertyDao.getPropertiesModifiedSince(0L).filter { it.spaceId == stream.appSpaceId }
        val eventExceptions = exceptionsByStream[stream.appSpaceId].orEmpty()
        if (!stream.isAccountStream) {
            return CloudListsPayload(
                propertyTags = propertyTags,
                customProperties = customProperties,
                eventExceptions = eventExceptions
            )
        }
        return CloudListsPayload(
            categories = categoryDao.getCategoriesModifiedSince(0L).filter { it.spaceId == ACCOUNT_SPACE_ID },
            propertyTags = propertyTags,
            customProperties = customProperties,
            eventExceptions = eventExceptions,
            apiConfigs = localApiConfigEntries(),
            bookmarkCategoryOrders = bookmarkCategoryOrderStore.getAllOrders(),
            favoriteNoteOrders = favoriteNoteOrderStore.getAllOrders()
        )
    }

    private suspend fun localApiConfigEntries(): List<ApiConfigSyncEntry> {
        val localTombstonesByProvider = selfHostDeletedApiConfigDao.getAllTombstones().associateBy { it.provider }
        return ExternalAiProvider.entries.mapNotNull { provider ->
            mergeApiConfig(
                provider = provider,
                localConfig = aiSettingsRepository.getProviderConfig(provider),
                localTombstone = localTombstonesByProvider[provider.name],
                remoteEntry = null
            ).mergedEntry
        }
    }

    private suspend fun routedEventExceptions(pass: ListPass): Map<String, List<CalendarEventExceptionEntity>> {
        pass.exceptionsByStream?.let { return it }
        val noteIdByBlockId = blockDao.getBlocksThatHaveEventExceptions().associate { it.blockId to it.noteId }
        val notesById = HashMap<String, NoteMetadataEntity?>()
        val exceptionsByStream = eventExceptionsByStream(
            exceptions = calendarEventExceptionDao.getExceptionsModifiedSince(0L),
            noteIdOfBlock = { blockId -> noteIdByBlockId[blockId] },
            noteOf = { noteId -> notesById.getOrPut(noteId) { noteDao.getNoteById(noteId) } }
        )
        pass.exceptionsByStream = exceptionsByStream
        return exceptionsByStream
    }

    private suspend fun listKeyToSend(stream: CloudStream, itemId: String, row: CloudItemEntity?): ListKeyToSend {
        val savedLockedKey = row?.lockedKeyOrNull()
        if (row != null && savedLockedKey != null && row.parentId == stream.appSpaceId) {
            return ListKeyToSend(openKey = streamRecords.openKeyOf(stream, itemId), lockedKey = savedLockedKey)
        }
        val newListKey = itemKeyManager.newItemKey()
        return ListKeyToSend(
            openKey = newListKey,
            lockedKey = itemKeyManager.lockChildKey(
                childKey = newListKey,
                childId = itemId,
                parentKey = streamRecords.openKeyOf(stream, stream.appSpaceId),
                parentId = stream.appSpaceId
            )
        )
    }

    private suspend fun takeInConflictingList(
        pass: ListPass,
        stream: CloudStream,
        itemId: String,
        currentItem: CloudRemoteItem?,
        counts: SyncPassCounts
    ) {
        if (currentItem == null) {
            stream.rowsByItemId[itemId]?.let { row -> stream.rowsByItemId[itemId] = row.copy(revision = 0L) }
            return
        }
        checkThisAppCanRead(currentItem)
        check(currentItem.itemType == listItemTypeOf(stream)) { "An item changed its type on the server" }
        pass.exceptionsByStream = null
        applyPulledList(pass, stream, currentItem, counts)
    }
}
