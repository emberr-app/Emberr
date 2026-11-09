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

class CloudSubscription(
    val plan: String,
    val storageLimitBytes: Long,
    val expiresAtMicroseconds: Long,
    val cloudCopyRemovedAtMicroseconds: Long?
) {
    val isTrial: Boolean
        get() = plan == TRIAL_PLAN

    companion object {
        const val TRIAL_PLAN = "trial"
    }
}

class CloudAccountDetails(
    val email: String,
    val storageUsedBytes: Long,
    val keys: CloudAccountKeys,
    val subscription: CloudSubscription
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

class CloudRemoteMediaLock(
    val mediaId: String,
    val noteId: String,
    val lockedKey: CloudEncryptedBytes?,
    val revision: Long,
    val deleted: Boolean,
    val updatedAtMicroseconds: Long
)

sealed class CloudChanges {
    class Page(
        val items: List<CloudRemoteItem>,
        val mediaLocks: List<CloudRemoteMediaLock>,
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

class CloudMediaPieceUploadUrl(
    val index: Int,
    val uploadUrl: String
)

class CloudMediaUploadUrls(
    val committed: Boolean,
    val pieces: List<CloudMediaPieceUploadUrl>,
    val expiresAtMicroseconds: Long?
)

class CloudMediaPieceDownloadUrl(
    val index: Int,
    val downloadUrl: String,
    val sizeBytes: Long
)

class CloudMediaDownloadUrls(
    val pieces: List<CloudMediaPieceDownloadUrl>,
    val expiresAtMicroseconds: Long
)

sealed class CloudWriteResult {
    class Saved(val revision: Long) : CloudWriteResult()

    class Conflict(val currentItem: CloudRemoteItem?) : CloudWriteResult()
}
