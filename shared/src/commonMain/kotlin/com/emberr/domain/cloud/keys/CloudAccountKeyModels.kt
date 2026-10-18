package com.emberr.domain.cloud.keys

class CloudAccountKeys(
    val salt: ByteArray,
    val memLimit: Long,
    val opsLimit: Int,
    val encryptedMasterKey: ByteArray,
    val encryptedMasterKeyNonce: ByteArray,
    val masterKeyEncryptedWithRecoveryKey: ByteArray,
    val masterKeyEncryptedWithRecoveryKeyNonce: ByteArray,
    val recoveryKeyEncryptedWithMasterKey: ByteArray,
    val recoveryKeyEncryptedWithMasterKeyNonce: ByteArray,
    val publicKey: ByteArray,
    val encryptedPrivateKey: ByteArray,
    val encryptedPrivateKeyNonce: ByteArray
)

class CloudSignupKeys(
    val accountKeys: CloudAccountKeys,
    val loginKey: ByteArray,
    val masterKey: ByteArray,
    val privateKey: ByteArray,
    val recoveryKey: ByteArray
)

class CloudRecoveryKeys(
    val masterKeyEncryptedWithRecoveryKey: ByteArray,
    val masterKeyEncryptedWithRecoveryKeyNonce: ByteArray,
    val publicKey: ByteArray,
    val encryptedPrivateKey: ByteArray,
    val encryptedPrivateKeyNonce: ByteArray
)

class CloudPasswordKeys(
    val salt: ByteArray,
    val memLimit: Long,
    val opsLimit: Int,
    val encryptedMasterKey: ByteArray,
    val encryptedMasterKeyNonce: ByteArray
)

class CloudNewPasswordKeys(
    val passwordKeys: CloudPasswordKeys,
    val loginKey: ByteArray
)
