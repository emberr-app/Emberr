package com.emberr.domain.cloud.account

import android.content.Context
import android.util.Base64
import com.emberr.core.security.AndroidSecretCipher
import com.emberr.core.security.TinkSecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCloudAccountStorage(
    context: Context,
    secretCipher: AndroidSecretCipher
) : CloudAccountStorage {

    private val secretStore = TinkSecretStore(
        applicationContext = context.applicationContext,
        storeFileName = SECRET_STORE_FILE_NAME,
        secretCipher = secretCipher
    )

    override suspend fun saveSessionToken(sessionToken: String) = writeSecret(KEY_SESSION_TOKEN, sessionToken)

    override suspend fun getSessionToken(): String? = readSecret(KEY_SESSION_TOKEN)

    override suspend fun saveMasterKey(masterKey: ByteArray) = writeKey(KEY_MASTER_KEY, masterKey)

    override suspend fun getMasterKey(): ByteArray? = readKey(KEY_MASTER_KEY)

    override suspend fun savePrivateKey(privateKey: ByteArray) = writeKey(KEY_PRIVATE_KEY, privateKey)

    override suspend fun getPrivateKey(): ByteArray? = readKey(KEY_PRIVATE_KEY)

    override suspend fun saveEmail(email: String) = writeSecret(KEY_EMAIL, email)

    override suspend fun getEmail(): String? = readSecret(KEY_EMAIL)

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        secretStore.clearAllSecrets()
    }

    private suspend fun writeSecret(secretName: String, secretValue: String) = withContext(Dispatchers.IO) {
        secretStore.writeSecret(secretName, secretValue)
    }

    private suspend fun readSecret(secretName: String): String? = withContext(Dispatchers.IO) {
        secretStore.readSecret(secretName)
    }

    private suspend fun writeKey(secretName: String, key: ByteArray) =
        writeSecret(secretName, Base64.encodeToString(key, Base64.NO_WRAP))

    private suspend fun readKey(secretName: String): ByteArray? {
        val encoded = readSecret(secretName) ?: return null
        return try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (cause: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val SECRET_STORE_FILE_NAME = "emberr_cloud_vault"
        const val KEY_SESSION_TOKEN = "cloud_session_token"
        const val KEY_MASTER_KEY = "cloud_master_key"
        const val KEY_PRIVATE_KEY = "cloud_private_key"
        const val KEY_EMAIL = "cloud_email"
    }
}
