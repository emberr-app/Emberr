package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudAccountKeys
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.cloud.keys.CloudPasswordKeys
import com.emberr.domain.cloud.keys.CloudRecoveryKeys
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

@Serializable
internal class EmailCodeRequest(val email: String, val purpose: String)

@Serializable
internal class PasswordParamsResponse(val salt: String, val memLimit: Long, val opsLimit: Int)

@Serializable
internal class AccountKeysJson(
    val salt: String,
    val memLimit: Long,
    val opsLimit: Int,
    val encryptedMasterKey: String,
    val encryptedMasterKeyNonce: String,
    val masterKeyEncryptedWithRecoveryKey: String,
    val masterKeyEncryptedWithRecoveryKeyNonce: String,
    val recoveryKeyEncryptedWithMasterKey: String,
    val recoveryKeyEncryptedWithMasterKeyNonce: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val encryptedPrivateKeyNonce: String
)

@Serializable
internal class RecoveryKeysJson(
    val masterKeyEncryptedWithRecoveryKey: String,
    val masterKeyEncryptedWithRecoveryKeyNonce: String,
    val publicKey: String,
    val encryptedPrivateKey: String,
    val encryptedPrivateKeyNonce: String
)

@Serializable
internal class PasswordKeysJson(
    val salt: String,
    val memLimit: Long,
    val opsLimit: Int,
    val encryptedMasterKey: String,
    val encryptedMasterKeyNonce: String
)

@Serializable
internal class SignupRequest(
    val email: String,
    val code: String,
    val deviceName: String,
    val loginKey: String,
    val keys: AccountKeysJson
)

@Serializable
internal class SignupResponse(val token: String)

@Serializable
internal class LoginRequest(val email: String, val loginKey: String, val deviceName: String)

@Serializable
internal class LoginResponse(val token: String, val keys: AccountKeysJson)

@Serializable
internal class RecoverRequest(val email: String, val code: String, val deviceName: String)

@Serializable
internal class RecoverResponse(val encryptedToken: String, val keys: RecoveryKeysJson)

@Serializable
internal class SessionJson(
    val id: String,
    val deviceName: String,
    val createdAt: Long,
    val lastUsedAt: Long,
    val isCurrent: Boolean
)

@Serializable
internal class SessionsResponse(val sessions: List<SessionJson>)

@Serializable
internal class AccountDetailsResponse(
    val email: String,
    val storageUsedBytes: Long,
    val keys: AccountKeysJson
)

@Serializable
internal class DeleteAccountRequest(val loginKey: String, val code: String)

@Serializable
internal class EraseAccountDataRequest(val loginKey: String)

@Serializable
internal class AccountEmailCodeRequest(val purpose: String, val email: String? = null)

@Serializable
internal class ChangeEmailRequest(val email: String, val code: String)

@Serializable
internal class ChangePasswordRequest(
    val loginKey: String,
    val keys: PasswordKeysJson,
    val logOutOtherDevices: Boolean,
    val code: String? = null
)

@Serializable
internal class RegisterSpaceRequest(
    val clientSpaceId: String,
    val kind: String,
    val encryptedKey: String,
    val encryptedKeyNonce: String
)

@Serializable
internal class RegisterSpaceResponse(
    val spaceRef: Long,
    val encryptedKey: String,
    val encryptedKeyNonce: String
)

@Serializable
internal class SpaceJson(
    val spaceRef: Long,
    val clientSpaceId: String,
    val kind: String,
    val latestRevision: Long,
    val encryptedKey: String,
    val encryptedKeyNonce: String
)

@Serializable
internal class SyncStateResponse(val spaces: List<SpaceJson>)

@Serializable
internal class ItemJson(
    val itemId: String,
    val itemType: String,
    val payloadVersion: Int,
    val parentId: String?,
    val encryptedKey: String?,
    val encryptedKeyNonce: String?,
    val inlinePayload: String?,
    val inlinePayloadNonce: String?,
    val blobNonce: String?,
    val downloadUrl: String?,
    val revision: Long,
    val sizeBytes: Long,
    val deleted: Boolean,
    val updatedAt: Long
)

@Serializable
internal class ChangesResponse(
    val resyncRequired: Boolean = false,
    val items: List<ItemJson> = emptyList(),
    val latest: Long,
    val hasMore: Boolean = false
)

@Serializable
internal class UploadUrlRequest(val sizeBytes: Long)

@Serializable
internal class UploadUrlResponse(val objectKey: String, val uploadUrl: String, val expiresAt: Long)

@Serializable
internal class CommitInlineItemRequest(
    val baseRevision: Long,
    val itemType: String,
    val payloadVersion: Int,
    val parentId: String? = null,
    val encryptedKey: String? = null,
    val encryptedKeyNonce: String? = null,
    val inlinePayload: String,
    val inlinePayloadNonce: String
)

@Serializable
internal class CommitBlobItemRequest(
    val baseRevision: Long,
    val itemType: String,
    val payloadVersion: Int,
    val parentId: String? = null,
    val encryptedKey: String,
    val encryptedKeyNonce: String,
    val objectKey: String,
    val blobNonce: String,
    val sizeBytes: Long
)

@Serializable
internal class DeleteItemRequest(val baseRevision: Long)

@Serializable
internal class RevisionResponse(val revision: Long)

@Serializable
internal class ConflictResponse(val item: ItemJson? = null)

@Serializable
internal class ErrorResponse(val code: String)

internal fun ByteArray.toBase64(): String = Base64.Default.encode(this)

internal fun String.fromBase64(): ByteArray = Base64.Default.decode(this)

internal fun CloudAccountKeys.toJson() = AccountKeysJson(
    salt = salt.toBase64(),
    memLimit = memLimit,
    opsLimit = opsLimit,
    encryptedMasterKey = encryptedMasterKey.toBase64(),
    encryptedMasterKeyNonce = encryptedMasterKeyNonce.toBase64(),
    masterKeyEncryptedWithRecoveryKey = masterKeyEncryptedWithRecoveryKey.toBase64(),
    masterKeyEncryptedWithRecoveryKeyNonce = masterKeyEncryptedWithRecoveryKeyNonce.toBase64(),
    recoveryKeyEncryptedWithMasterKey = recoveryKeyEncryptedWithMasterKey.toBase64(),
    recoveryKeyEncryptedWithMasterKeyNonce = recoveryKeyEncryptedWithMasterKeyNonce.toBase64(),
    publicKey = publicKey.toBase64(),
    encryptedPrivateKey = encryptedPrivateKey.toBase64(),
    encryptedPrivateKeyNonce = encryptedPrivateKeyNonce.toBase64()
)

internal fun AccountKeysJson.toCloudAccountKeys() = CloudAccountKeys(
    salt = salt.fromBase64(),
    memLimit = memLimit,
    opsLimit = opsLimit,
    encryptedMasterKey = encryptedMasterKey.fromBase64(),
    encryptedMasterKeyNonce = encryptedMasterKeyNonce.fromBase64(),
    masterKeyEncryptedWithRecoveryKey = masterKeyEncryptedWithRecoveryKey.fromBase64(),
    masterKeyEncryptedWithRecoveryKeyNonce = masterKeyEncryptedWithRecoveryKeyNonce.fromBase64(),
    recoveryKeyEncryptedWithMasterKey = recoveryKeyEncryptedWithMasterKey.fromBase64(),
    recoveryKeyEncryptedWithMasterKeyNonce = recoveryKeyEncryptedWithMasterKeyNonce.fromBase64(),
    publicKey = publicKey.fromBase64(),
    encryptedPrivateKey = encryptedPrivateKey.fromBase64(),
    encryptedPrivateKeyNonce = encryptedPrivateKeyNonce.fromBase64()
)

internal fun RecoveryKeysJson.toCloudRecoveryKeys() = CloudRecoveryKeys(
    masterKeyEncryptedWithRecoveryKey = masterKeyEncryptedWithRecoveryKey.fromBase64(),
    masterKeyEncryptedWithRecoveryKeyNonce = masterKeyEncryptedWithRecoveryKeyNonce.fromBase64(),
    publicKey = publicKey.fromBase64(),
    encryptedPrivateKey = encryptedPrivateKey.fromBase64(),
    encryptedPrivateKeyNonce = encryptedPrivateKeyNonce.fromBase64()
)

internal fun CloudPasswordKeys.toJson() = PasswordKeysJson(
    salt = salt.toBase64(),
    memLimit = memLimit,
    opsLimit = opsLimit,
    encryptedMasterKey = encryptedMasterKey.toBase64(),
    encryptedMasterKeyNonce = encryptedMasterKeyNonce.toBase64()
)

internal fun SessionJson.toCloudSession() = CloudSession(
    id = id,
    deviceName = deviceName,
    createdAtMicroseconds = createdAt,
    lastUsedAtMicroseconds = lastUsedAt,
    isCurrent = isCurrent
)

internal fun SpaceJson.toCloudRemoteSpace() = CloudRemoteSpace(
    spaceRef = spaceRef,
    clientSpaceId = clientSpaceId,
    kind = CloudSpaceKind.entries.firstOrNull { it.wireValue == kind },
    latestRevision = latestRevision,
    lockedSpaceKey = CloudEncryptedBytes(encryptedKey.fromBase64(), encryptedKeyNonce.fromBase64())
)

internal fun ItemJson.toCloudRemoteItem() = CloudRemoteItem(
    itemId = itemId,
    itemType = CloudItemType.entries.firstOrNull { it.name == itemType },
    payloadVersion = payloadVersion,
    parentId = parentId,
    lockedKey = encryptedBytesOrNull(encryptedKey, encryptedKeyNonce),
    inlinePayload = encryptedBytesOrNull(inlinePayload, inlinePayloadNonce),
    blobNonce = blobNonce?.fromBase64(),
    downloadUrl = downloadUrl,
    revision = revision,
    sizeBytes = sizeBytes,
    deleted = deleted,
    updatedAtMicroseconds = updatedAt
)

private fun encryptedBytesOrNull(cipherText: String?, nonce: String?): CloudEncryptedBytes? =
    if (cipherText == null || nonce == null) null else CloudEncryptedBytes(cipherText.fromBase64(), nonce.fromBase64())
