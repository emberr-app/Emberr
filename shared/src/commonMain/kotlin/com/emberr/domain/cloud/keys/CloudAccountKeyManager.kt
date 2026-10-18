package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.crypto.CloudDecryptionException

class CloudAccountKeyManager(private val cloudCrypto: CloudCrypto) {

    suspend fun createKeysForSignup(password: String): CloudSignupKeys {
        val masterKey = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)
        val recoveryKey = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)
        val keyPair = cloudCrypto.newKeyPair()
        val newPasswordKeys = createKeysForNewPassword(password, masterKey)
        val passwordKeys = newPasswordKeys.passwordKeys
        val masterKeyLockedByRecoveryKey = cloudCrypto.encrypt(
            masterKey,
            recoveryKey,
            MASTER_KEY_LOCKED_BY_RECOVERY_KEY.encodeToByteArray()
        )
        val recoveryKeyLockedByMasterKey = cloudCrypto.encrypt(
            recoveryKey,
            masterKey,
            RECOVERY_KEY_LOCKED_BY_MASTER_KEY.encodeToByteArray()
        )
        val privateKeyLockedByMasterKey = cloudCrypto.encrypt(
            keyPair.secretKey,
            masterKey,
            PRIVATE_KEY_LOCKED_BY_MASTER_KEY.encodeToByteArray()
        )
        val accountKeys = CloudAccountKeys(
            salt = passwordKeys.salt,
            memLimit = passwordKeys.memLimit,
            opsLimit = passwordKeys.opsLimit,
            encryptedMasterKey = passwordKeys.encryptedMasterKey,
            encryptedMasterKeyNonce = passwordKeys.encryptedMasterKeyNonce,
            masterKeyEncryptedWithRecoveryKey = masterKeyLockedByRecoveryKey.cipherText,
            masterKeyEncryptedWithRecoveryKeyNonce = masterKeyLockedByRecoveryKey.nonce,
            recoveryKeyEncryptedWithMasterKey = recoveryKeyLockedByMasterKey.cipherText,
            recoveryKeyEncryptedWithMasterKeyNonce = recoveryKeyLockedByMasterKey.nonce,
            publicKey = keyPair.publicKey,
            encryptedPrivateKey = privateKeyLockedByMasterKey.cipherText,
            encryptedPrivateKeyNonce = privateKeyLockedByMasterKey.nonce
        )
        return CloudSignupKeys(
            accountKeys = accountKeys,
            loginKey = newPasswordKeys.loginKey,
            masterKey = masterKey,
            privateKey = keyPair.secretKey,
            recoveryKey = recoveryKey
        )
    }

    suspend fun createKeysForNewPassword(newPassword: String, masterKey: ByteArray): CloudNewPasswordKeys {
        val salt = cloudCrypto.randomBytes(SALT_SIZE)
        val passwordKey = derivePasswordKey(newPassword, salt, DEFAULT_MEM_LIMIT_BYTES, DEFAULT_OPS_LIMIT)
        val masterKeyLockedByPasswordKey = cloudCrypto.encrypt(
            masterKey,
            passwordKey,
            MASTER_KEY_LOCKED_BY_PASSWORD_KEY.encodeToByteArray()
        )
        val passwordKeys = CloudPasswordKeys(
            salt = salt,
            memLimit = DEFAULT_MEM_LIMIT_BYTES,
            opsLimit = DEFAULT_OPS_LIMIT,
            encryptedMasterKey = masterKeyLockedByPasswordKey.cipherText,
            encryptedMasterKeyNonce = masterKeyLockedByPasswordKey.nonce
        )
        return CloudNewPasswordKeys(passwordKeys = passwordKeys, loginKey = deriveLoginKey(passwordKey))
    }

    suspend fun derivePasswordKey(password: String, salt: ByteArray, memLimit: Long, opsLimit: Int): ByteArray =
        cloudCrypto.deriveKeyFromPassword(password, salt, memLimit, opsLimit)

    suspend fun deriveLoginKey(passwordKey: ByteArray): ByteArray =
        cloudCrypto.deriveSubKey(passwordKey, LOGIN_KEY_ID, LOGIN_KEY_CONTEXT, LOGIN_KEY_SIZE)

    suspend fun unlockMasterKeyWithPasswordKey(passwordKey: ByteArray, accountKeys: CloudAccountKeys): ByteArray =
        try {
            cloudCrypto.decrypt(
                accountKeys.encryptedMasterKey,
                accountKeys.encryptedMasterKeyNonce,
                passwordKey,
                MASTER_KEY_LOCKED_BY_PASSWORD_KEY.encodeToByteArray()
            )
        } catch (cause: CloudDecryptionException) {
            throw IncorrectCloudPasswordException(cause)
        }

    suspend fun unlockMasterKeyWithRecoveryKey(recoveryKey: ByteArray, recoveryKeys: CloudRecoveryKeys): ByteArray =
        try {
            cloudCrypto.decrypt(
                recoveryKeys.masterKeyEncryptedWithRecoveryKey,
                recoveryKeys.masterKeyEncryptedWithRecoveryKeyNonce,
                recoveryKey,
                MASTER_KEY_LOCKED_BY_RECOVERY_KEY.encodeToByteArray()
            )
        } catch (cause: CloudDecryptionException) {
            throw IncorrectRecoveryKeyException(cause)
        }

    suspend fun unlockPrivateKey(
        masterKey: ByteArray,
        encryptedPrivateKey: ByteArray,
        encryptedPrivateKeyNonce: ByteArray
    ): ByteArray = cloudCrypto.decrypt(
        encryptedPrivateKey,
        encryptedPrivateKeyNonce,
        masterKey,
        PRIVATE_KEY_LOCKED_BY_MASTER_KEY.encodeToByteArray()
    )

    suspend fun readRecoveryKey(masterKey: ByteArray, accountKeys: CloudAccountKeys): ByteArray =
        cloudCrypto.decrypt(
            accountKeys.recoveryKeyEncryptedWithMasterKey,
            accountKeys.recoveryKeyEncryptedWithMasterKeyNonce,
            masterKey,
            RECOVERY_KEY_LOCKED_BY_MASTER_KEY.encodeToByteArray()
        )

    companion object {
        const val SALT_SIZE = 16
        const val DEFAULT_MEM_LIMIT_BYTES = 67108864L
        const val DEFAULT_OPS_LIMIT = 3
        private const val LOGIN_KEY_ID = 1
        private const val LOGIN_KEY_CONTEXT = "loginctx"
        private const val LOGIN_KEY_SIZE = 32
        private const val MASTER_KEY_LOCKED_BY_PASSWORD_KEY = "emberr-cloud:v1:lock:master:password"
        private const val MASTER_KEY_LOCKED_BY_RECOVERY_KEY = "emberr-cloud:v1:lock:master:recovery"
        private const val RECOVERY_KEY_LOCKED_BY_MASTER_KEY = "emberr-cloud:v1:lock:recovery:master"
        private const val PRIVATE_KEY_LOCKED_BY_MASTER_KEY = "emberr-cloud:v1:lock:private:master"
    }
}
