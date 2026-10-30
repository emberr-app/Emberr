package com.emberr.domain.credentials

import com.emberr.core.security.SyncEncryptionManager
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialKey
import com.emberr.domain.selfhost.crypto.KeyDerivationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface CredentialEncryption {
    data class Encrypted(val encryptedValue: String, val keyId: String) : CredentialEncryption
    data object KeyringUnavailable : CredentialEncryption
}

class CredentialVault(
    private val keyStore: CredentialKeyStore,
    private val syncEncryptionManager: SyncEncryptionManager,
    private val keyDerivationManager: KeyDerivationManager
) {

    private val keyringLock = Mutex()
    private val readableKeysById = mutableMapOf<String, String>()

    private val _keyringVersion = MutableStateFlow(0)
    val keyringVersion: StateFlow<Int> = _keyringVersion.asStateFlow()

    suspend fun encrypt(kind: CredentialFieldKind, text: String): CredentialEncryption =
        withContext(Dispatchers.IO) {
            val encryptionKey = keyringLock.withLock { keyForNewValues() }
                ?: return@withContext CredentialEncryption.KeyringUnavailable
            CredentialEncryption.Encrypted(
                encryptedValue = syncEncryptionManager.encryptPayload(
                    text,
                    encryptionKey.key,
                    associatedData(encryptionKey.id, kind)
                ),
                keyId = encryptionKey.id
            )
        }

    suspend fun decrypt(field: CredentialField): String? = withContext(Dispatchers.IO) {
        val encryptedValue = field.encryptedValue ?: return@withContext null
        val keyId = field.keyId ?: return@withContext null
        val key = keyringLock.withLock { readableKey(keyId) } ?: return@withContext null
        runCatching {
            syncEncryptionManager.decryptPayload(encryptedValue, key, associatedData(keyId, field.kind))
        }.getOrNull()
    }

    suspend fun mergeKeys(incoming: List<CredentialKey>): Boolean = withContext(Dispatchers.IO) {
        keyringLock.withLock { addMissingKeys(incoming) }
    }

    suspend fun allReadableKeys(): List<CredentialKey> = withContext(Dispatchers.IO) {
        keyringLock.withLock {
            runCatching { keyStore.listKeyIndex() }.getOrNull().orEmpty()
                .distinctBy { entry -> entry.id }
                .mapNotNull { entry ->
                    readableKey(entry.id)?.let { key -> CredentialKey(entry.id, key, entry.createdAt) }
                }
        }
    }

    private fun keyForNewValues(): CredentialKey? {
        val keyIndex = runCatching { keyStore.listKeyIndex() }.getOrNull() ?: return null
        val oldestReadableKey = keyIndex
            .sortedWith(compareBy<CredentialKeyIndexEntry> { entry -> entry.createdAt }.thenBy { entry -> entry.id })
            .firstNotNullOfOrNull { entry ->
                readableKey(entry.id)?.let { key -> CredentialKey(entry.id, key, entry.createdAt) }
            }
        if (oldestReadableKey != null) return oldestReadableKey
        val newKey = createKey()
        return if (addKey(newKey)) newKey else null
    }

    private fun addMissingKeys(incoming: List<CredentialKey>): Boolean {
        val listedKeyIds = runCatching { keyStore.listKeyIndex() }.getOrNull()
            ?.map { entry -> entry.id }?.toSet()
            ?: return false
        return incoming
            .distinctBy { key -> key.id }
            .filter { key -> key.id !in listedKeyIds || readableKey(key.id) == null }
            .map { key -> addKey(key) }
            .all { wasAdded -> wasAdded }
    }

    private fun readableKey(keyId: String): String? {
        readableKeysById[keyId]?.let { key -> return key }
        return runCatching { keyStore.readKey(keyId) }.getOrNull()
            ?.also { key -> readableKeysById[keyId] = key }
    }

    private fun addKey(key: CredentialKey): Boolean {
        val wasSaved = runCatching { keyStore.addKey(key) }.isSuccess &&
            runCatching { keyStore.readKey(key.id) }.getOrNull() == key.key
        if (wasSaved) {
            readableKeysById[key.id] = key.key
            _keyringVersion.update { version -> version + 1 }
        }
        return wasSaved
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalEncodingApi::class)
    private fun createKey() = CredentialKey(
        id = Uuid.random().toString(),
        key = Base64.encode(keyDerivationManager.generateVaultKey()),
        createdAt = Clock.System.now().toEpochMilliseconds()
    )

    private fun associatedData(keyId: String, kind: CredentialFieldKind): ByteArray =
        "$keyId/${kind.name}".encodeToByteArray()
}
