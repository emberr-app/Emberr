package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.domain.cloud.api.CloudRemoteItem
import com.emberr.domain.cloud.api.fromBase64
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.selfhost.translation.NOTE_PAYLOAD_SCHEMA_VERSION

internal const val SPACE_AND_FOLDER_PAYLOAD_VERSION = 1
internal const val LISTS_PAYLOAD_VERSION = 1
internal const val CHAT_PAYLOAD_VERSION = 1
internal const val MAX_COMMIT_TRIES_PER_ITEM = 3

private val highestPayloadVersionThisAppReads = mapOf(
    CloudItemType.SPACE to SPACE_AND_FOLDER_PAYLOAD_VERSION,
    CloudItemType.FOLDER to SPACE_AND_FOLDER_PAYLOAD_VERSION,
    CloudItemType.NOTE to NOTE_PAYLOAD_SCHEMA_VERSION,
    CloudItemType.DAILY to NOTE_PAYLOAD_SCHEMA_VERSION,
    CloudItemType.CHAT to CHAT_PAYLOAD_VERSION,
    CloudItemType.SPACE_LISTS to LISTS_PAYLOAD_VERSION,
    CloudItemType.ACCOUNT_LISTS to LISTS_PAYLOAD_VERSION
)

internal class CloudUpdateRequiredException : Exception("Emberr Cloud holds data this version of the app cannot read")

internal class CloudKeyNotFoundException : Exception("An item's key could not be opened because its parent's key is missing")

internal class CloudStream(
    val appSpaceId: String,
    val spaceRef: Long,
    val lastRevisionSeen: Long,
    val rowsByItemId: MutableMap<String, CloudItemEntity>,
    val openKeys: MutableMap<String, ByteArray>
) {
    val isAccountStream: Boolean
        get() = appSpaceId == ACCOUNT_SPACE_ID
}

internal class SyncPassCounts {
    var receivedCount = 0
    var sentCount = 0
    var notSentCount = 0
    var spacesChanged = false
}

internal fun checkThisAppCanRead(item: CloudRemoteItem) {
    val highestPayloadVersion = item.itemType?.let(highestPayloadVersionThisAppReads::get)
    if (highestPayloadVersion == null || item.payloadVersion > highestPayloadVersion) {
        throw CloudUpdateRequiredException()
    }
}

internal fun CloudItemEntity.lockedKeyOrNull(): CloudEncryptedBytes? {
    val cipherText = encryptedKey ?: return null
    val nonce = encryptedKeyNonce ?: return null
    return CloudEncryptedBytes(cipherText.fromBase64(), nonce.fromBase64())
}

internal class CloudStreamRecords(
    private val itemKeyManager: CloudItemKeyManager,
    private val cloudItemDao: CloudItemDao
) {

    suspend fun openKeyOf(
        stream: CloudStream,
        itemId: String,
        idsOnThisPath: MutableSet<String> = mutableSetOf()
    ): ByteArray {
        stream.openKeys[itemId]?.let { return it }
        val row = stream.rowsByItemId[itemId]
        val parentId = row?.parentId
        val lockedKey = row?.lockedKeyOrNull()
        if (parentId == null || lockedKey == null || !idsOnThisPath.add(itemId)) throw CloudKeyNotFoundException()
        val itemKey = itemKeyManager.unlockChildKey(lockedKey, itemId, openKeyOf(stream, parentId, idsOnThisPath), parentId)
        stream.openKeys[itemId] = itemKey
        return itemKey
    }

    suspend fun openPulledItemKey(stream: CloudStream, item: CloudRemoteItem): ByteArray {
        val lockedKey = item.lockedKey ?: throw CloudKeyNotFoundException()
        val parentId = item.parentId ?: throw CloudKeyNotFoundException()
        val itemKey = itemKeyManager.unlockChildKey(lockedKey, item.itemId, openKeyOf(stream, parentId), parentId)
        stream.openKeys[item.itemId] = itemKey
        return itemKey
    }

    suspend fun saveRow(stream: CloudStream, row: CloudItemEntity) {
        cloudItemDao.upsertItem(row)
        stream.rowsByItemId[row.itemId] = row
    }
}
