package com.emberr.domain.cloud.account

import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretNamespace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64

class DesktopCloudAccountStorage(private val secretStore: DesktopSecretStore) : CloudAccountStorage {

    override suspend fun saveSessionToken(sessionToken: String) = writeSecret(ACCOUNT_SESSION_TOKEN, sessionToken)

    override suspend fun getSessionToken(): String? = readSecret(ACCOUNT_SESSION_TOKEN)

    override suspend fun saveMasterKey(masterKey: ByteArray) = writeKey(ACCOUNT_MASTER_KEY, masterKey)

    override suspend fun getMasterKey(): ByteArray? = readKey(ACCOUNT_MASTER_KEY)

    override suspend fun savePrivateKey(privateKey: ByteArray) = writeKey(ACCOUNT_PRIVATE_KEY, privateKey)

    override suspend fun getPrivateKey(): ByteArray? = readKey(ACCOUNT_PRIVATE_KEY)

    override suspend fun saveEmail(email: String) = writeSecret(ACCOUNT_EMAIL, email)

    override suspend fun getEmail(): String? = readSecret(ACCOUNT_EMAIL)

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        listOf(ACCOUNT_SESSION_TOKEN, ACCOUNT_MASTER_KEY, ACCOUNT_PRIVATE_KEY, ACCOUNT_EMAIL).forEach { account ->
            secretStore.removeSecret(SecretNamespace.EmberrCloud, account)
        }
    }

    private suspend fun writeSecret(account: String, secret: String) = withContext(Dispatchers.IO) {
        secretStore.writeSecret(SecretNamespace.EmberrCloud, account, secret)
    }

    private suspend fun readSecret(account: String): String? = withContext(Dispatchers.IO) {
        secretStore.readSecret(SecretNamespace.EmberrCloud, account)
    }

    private suspend fun writeKey(account: String, key: ByteArray) =
        writeSecret(account, Base64.getEncoder().encodeToString(key))

    private suspend fun readKey(account: String): ByteArray? {
        val encoded = readSecret(account) ?: return null
        return try {
            Base64.getDecoder().decode(encoded)
        } catch (cause: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val ACCOUNT_SESSION_TOKEN = "session_token"
        const val ACCOUNT_MASTER_KEY = "master_key"
        const val ACCOUNT_PRIVATE_KEY = "private_key"
        const val ACCOUNT_EMAIL = "email"
    }
}
