package com.emberr.domain.cloud.api

import com.emberr.domain.cloud.keys.CloudAccountKeys
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
internal class LoginResponse(val emailCodeNeeded: Boolean)

@Serializable
internal class VerifyDeviceRequest(
    val email: String,
    val loginKey: String,
    val code: String,
    val deviceName: String
)

@Serializable
internal class VerifyDeviceResponse(val token: String, val keys: AccountKeysJson)

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
internal class AccountEmailCodeRequest(val purpose: String, val email: String? = null)

@Serializable
internal class ChangeEmailRequest(val email: String, val code: String)

@Serializable
internal class ChangePasswordRequest(
    val loginKey: String,
    val keys: PasswordKeysJson,
    val logOutOtherDevices: Boolean
)

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
