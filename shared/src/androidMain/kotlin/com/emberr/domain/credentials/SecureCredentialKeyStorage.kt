package com.emberr.domain.credentials

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.emberr.core.security.AndroidSecretCipher
import com.emberr.core.security.TinkSecretStore
import com.emberr.domain.model.CredentialKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

actual class SecureCredentialKeyStorage(
    context: Context,
    secretCipher: AndroidSecretCipher
) : CredentialKeyStore {

    private val json = Json { ignoreUnknownKeys = true }
    private val keyIndexSerializer = ListSerializer(CredentialKeyIndexEntry.serializer())

    private val secretStore = TinkSecretStore(
        applicationContext = context.applicationContext,
        storeFileName = SECRET_STORE_FILE_NAME,
        secretCipher = secretCipher
    )

    private val keyIndexPreferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(KEY_INDEX_FILE_NAME, Context.MODE_PRIVATE)

    actual override fun listKeyIndex(): List<CredentialKeyIndexEntry> {
        val keyIndexJson = keyIndexPreferences.getString(KEY_INDEX_NAME, null) ?: return emptyList()
        return json.decodeFromString(keyIndexSerializer, keyIndexJson)
    }

    actual override fun readKey(keyId: String): String? = secretStore.readSecret(secretNameFor(keyId))

    actual override fun addKey(key: CredentialKey) {
        secretStore.writeSecret(secretNameFor(key.id), key.key)
        val keyIndex = listKeyIndex()
        if (keyIndex.any { entry -> entry.id == key.id }) return
        val updatedKeyIndex = keyIndex + CredentialKeyIndexEntry(key.id, key.createdAt)
        keyIndexPreferences.edit(commit = true) {
            putString(KEY_INDEX_NAME, json.encodeToString(keyIndexSerializer, updatedKeyIndex))
        }
    }

    private fun secretNameFor(keyId: String) = "credential_key_$keyId"

    private companion object {
        const val SECRET_STORE_FILE_NAME = "emberr_credential_key_vault"
        const val KEY_INDEX_FILE_NAME = "emberr_credential_key_index"
        const val KEY_INDEX_NAME = "credential_key_index"
    }
}
