package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudAccountKeys
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.cloud.keys.CloudRecoveryKeys

enum class CloudEmailCodePurpose(internal val wireValue: String) {
    SIGNUP("signup"),
    RECOVER("recover")
}

class CloudPasswordParams(
    val salt: ByteArray,
    val memLimit: Long,
    val opsLimit: Int
)

class CloudSignedInDevice(
    val token: String,
    val keys: CloudAccountKeys
)

class CloudRecoveryStart(
    val encryptedToken: ByteArray,
    val keys: CloudRecoveryKeys
)

class CloudSession(
    val id: String,
    val deviceName: String,
    val createdAtMicroseconds: Long,
    val lastUsedAtMicroseconds: Long,
    val isCurrent: Boolean
)

class CloudAccountDetails(
    val email: String,
    val storageUsedBytes: Long,
    val keys: CloudAccountKeys
)

enum class CloudSpaceKind(internal val wireValue: String) {
    SPACE("space"),
    ACCOUNT("account")
}

class CloudRegisteredSpace(
    val spaceRef: Long,
    val lockedSpaceKey: CloudEncryptedBytes
)

class CloudRemoteSpace(
    val spaceRef: Long,
    val clientSpaceId: String,
    val kind: CloudSpaceKind?,
    val latestRevision: Long,
    val lockedSpaceKey: CloudEncryptedBytes
)

class CloudRemoteItem(
    val itemId: String,
    val itemType: CloudItemType?,
    val payloadVersion: Int,
    val parentId: String?,
    val lockedKey: CloudEncryptedBytes?,
    val inlinePayload: CloudEncryptedBytes?,
    val blobNonce: ByteArray?,
    val downloadUrl: String?,
    val revision: Long,
    val sizeBytes: Long,
    val deleted: Boolean,
    val updatedAtMicroseconds: Long
)

sealed class CloudChanges {
    class Page(
        val items: List<CloudRemoteItem>,
        val latest: Long,
        val hasMore: Boolean
    ) : CloudChanges()

    class ResyncRequired(val latest: Long) : CloudChanges()
}

class CloudUploadUrl(
    val objectKey: String,
    val uploadUrl: String,
    val expiresAtMicroseconds: Long
)

sealed class CloudWriteResult {
    class Saved(val revision: Long) : CloudWriteResult()

    class Conflict(val currentItem: CloudRemoteItem?) : CloudWriteResult()
}
