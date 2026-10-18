package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes

enum class CloudItemType {
    SPACE,
    FOLDER,
    NOTE,
    DAILY,
    CHAT,
    SPACE_LISTS,
    PERSONAL_LISTS
}

class CloudItemKeyManager(private val cloudCrypto: CloudCrypto) {

    suspend fun newItemKey(): ByteArray = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)

    suspend fun lockChildKey(childKey: ByteArray, childId: String, parentKey: ByteArray, parentId: String): CloudEncryptedBytes =
        cloudCrypto.encrypt(childKey, parentKey, lockAssociatedData(childId, parentId))

    suspend fun unlockChildKey(
        lockedChildKey: CloudEncryptedBytes,
        childId: String,
        parentKey: ByteArray,
        parentId: String
    ): ByteArray = cloudCrypto.decrypt(
        lockedChildKey.cipherText,
        lockedChildKey.nonce,
        parentKey,
        lockAssociatedData(childId, parentId)
    )

    suspend fun relockChildKeyForMove(
        lockedChildKey: CloudEncryptedBytes,
        childId: String,
        oldParentKey: ByteArray,
        oldParentId: String,
        newParentKey: ByteArray,
        newParentId: String
    ): CloudEncryptedBytes {
        val childKey = unlockChildKey(lockedChildKey, childId, oldParentKey, oldParentId)
        return lockChildKey(childKey, childId, newParentKey, newParentId)
    }

    suspend fun encryptContent(
        content: ByteArray,
        itemKey: ByteArray,
        itemType: CloudItemType,
        itemId: String
    ): CloudEncryptedBytes = cloudCrypto.encrypt(content, itemKey, contentAssociatedData(itemType, itemId))

    suspend fun decryptContent(
        encryptedContent: CloudEncryptedBytes,
        itemKey: ByteArray,
        itemType: CloudItemType,
        itemId: String
    ): ByteArray = cloudCrypto.decrypt(
        encryptedContent.cipherText,
        encryptedContent.nonce,
        itemKey,
        contentAssociatedData(itemType, itemId)
    )

    private fun lockAssociatedData(childId: String, parentId: String): ByteArray =
        "emberr-cloud:v1:lock:$childId:$parentId".encodeToByteArray()

    private fun contentAssociatedData(itemType: CloudItemType, itemId: String): ByteArray =
        "emberr-cloud:v1:${itemType.name}:$itemId".encodeToByteArray()

    companion object {
        const val MASTER_KEY_PARENT_ID = "master"
    }
}
