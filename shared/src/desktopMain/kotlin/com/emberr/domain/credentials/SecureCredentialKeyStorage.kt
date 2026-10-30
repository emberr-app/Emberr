package com.emberr.domain.credentials

import com.emberr.core.security.secrets.DesktopSecretStore
import com.emberr.core.security.secrets.SecretNamespace
import com.emberr.domain.model.CredentialKey
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

actual class SecureCredentialKeyStorage(
    private val secretStore: DesktopSecretStore,
    storageDirectory: File
) : CredentialKeyStore {

    private val json = Json { ignoreUnknownKeys = true }
    private val keyIndexSerializer = ListSerializer(CredentialKeyIndexEntry.serializer())
    private val keyIndexFile = File(storageDirectory, KEY_INDEX_FILE_NAME)
    private val temporaryKeyIndexFile = File(storageDirectory, "$KEY_INDEX_FILE_NAME.tmp")

    actual override fun listKeyIndex(): List<CredentialKeyIndexEntry> {
        if (!keyIndexFile.exists()) return emptyList()
        return json.decodeFromString(keyIndexSerializer, keyIndexFile.readText())
    }

    actual override fun readKey(keyId: String): String? =
        secretStore.readSecret(SecretNamespace.CredentialKeys, secretAccountFor(keyId))

    actual override fun addKey(key: CredentialKey) {
        secretStore.writeSecret(SecretNamespace.CredentialKeys, secretAccountFor(key.id), key.key)
        val keyIndex = listKeyIndex()
        if (keyIndex.any { entry -> entry.id == key.id }) return
        val updatedKeyIndex = keyIndex + CredentialKeyIndexEntry(key.id, key.createdAt)
        temporaryKeyIndexFile.writeText(json.encodeToString(keyIndexSerializer, updatedKeyIndex))
        moveTemporaryKeyIndexIntoPlace()
    }

    private fun moveTemporaryKeyIndexIntoPlace() {
        val temporaryPath = temporaryKeyIndexFile.toPath()
        val destinationPath = keyIndexFile.toPath()
        try {
            Files.move(
                temporaryPath,
                destinationPath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        } catch (cause: AtomicMoveNotSupportedException) {
            Files.move(temporaryPath, destinationPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun secretAccountFor(keyId: String) = "credential_key_$keyId"

    private companion object {
        const val KEY_INDEX_FILE_NAME = "credential_key_index.json"
    }
}
