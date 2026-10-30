package com.emberr.domain.credentials

import com.emberr.core.security.AesGcmEncryptionManager
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialKey
import com.emberr.domain.selfhost.crypto.Pbkdf2KeyDerivationManager
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CredentialVaultTest {

    @Test
    fun passwordSurvivesJsonRoundTripWithoutAppearingInTheJson() = runTest {
        val vault = vaultWith(InMemoryCredentialKeyStore())

        val passwordField = passwordFieldFrom(vault.encrypt(CredentialFieldKind.PASSWORD, SECRET_PASSWORD))
        val fieldJson = Json.encodeToString(CredentialField.serializer(), passwordField)
        val restoredField = Json.decodeFromString(CredentialField.serializer(), fieldJson)

        assertFalse(fieldJson.contains(SECRET_PASSWORD))
        assertEquals(SECRET_PASSWORD, vault.decrypt(restoredField))
    }

    @Test
    fun missingKeysDecryptToNullAndMergeRestoresThemWithoutRemovingAny() = runTest {
        val otherDeviceStore = InMemoryCredentialKeyStore()
        val fieldFromOtherDevice = passwordFieldFrom(
            vaultWith(otherDeviceStore).encrypt(CredentialFieldKind.PASSWORD, SECRET_PASSWORD)
        )
        val thisDeviceStore = InMemoryCredentialKeyStore()
        val ownField = passwordFieldFrom(vaultWith(thisDeviceStore).encrypt(CredentialFieldKind.PASSWORD, OWN_PASSWORD))
        val ownKey = thisDeviceStore.storedKey(ownField.keyId!!)
        thisDeviceStore.loseSecret(ownKey.id)
        val vaultAfterKeyringReset = vaultWith(thisDeviceStore)

        assertNull(vaultAfterKeyringReset.decrypt(fieldFromOtherDevice))
        assertNull(vaultAfterKeyringReset.decrypt(ownField))

        val newKeyId = passwordFieldFrom(vaultAfterKeyringReset.encrypt(CredentialFieldKind.PASSWORD, "new")).keyId
        assertNotEquals(ownKey.id, newKeyId)
        assertEquals(listOf(ownKey.id, newKeyId), thisDeviceStore.listKeyIndex().map { entry -> entry.id })

        val otherDeviceKey = otherDeviceStore.storedKey(fieldFromOtherDevice.keyId!!)
        assertTrue(vaultAfterKeyringReset.mergeKeys(listOf(ownKey, otherDeviceKey)))

        assertEquals(
            listOf(ownKey.id, newKeyId, otherDeviceKey.id),
            thisDeviceStore.listKeyIndex().map { entry -> entry.id }
        )
        assertEquals(OWN_PASSWORD, vaultAfterKeyringReset.decrypt(ownField))
        assertEquals(SECRET_PASSWORD, vaultAfterKeyringReset.decrypt(fieldFromOtherDevice))
    }

    private fun vaultWith(keyStore: CredentialKeyStore) = CredentialVault(
        keyStore = keyStore,
        syncEncryptionManager = AesGcmEncryptionManager(),
        keyDerivationManager = Pbkdf2KeyDerivationManager()
    )

    private fun passwordFieldFrom(encryption: CredentialEncryption): CredentialField {
        val encrypted = assertIs<CredentialEncryption.Encrypted>(encryption)
        return CredentialField(
            id = "password-field",
            kind = CredentialFieldKind.PASSWORD,
            label = "Password",
            encryptedValue = encrypted.encryptedValue,
            keyId = encrypted.keyId
        )
    }

    private class InMemoryCredentialKeyStore : CredentialKeyStore {
        private val keyIndex = mutableListOf<CredentialKeyIndexEntry>()
        private val secretsByKeyId = mutableMapOf<String, String>()

        override fun listKeyIndex(): List<CredentialKeyIndexEntry> = keyIndex.toList()

        override fun readKey(keyId: String): String? = secretsByKeyId[keyId]

        override fun addKey(key: CredentialKey) {
            secretsByKeyId[key.id] = key.key
            if (keyIndex.none { entry -> entry.id == key.id }) {
                keyIndex += CredentialKeyIndexEntry(key.id, key.createdAt)
            }
        }

        fun storedKey(keyId: String): CredentialKey {
            val entry = keyIndex.first { indexEntry -> indexEntry.id == keyId }
            return CredentialKey(entry.id, secretsByKeyId.getValue(keyId), entry.createdAt)
        }

        fun loseSecret(keyId: String) {
            secretsByKeyId.remove(keyId)
        }
    }

    private companion object {
        const val SECRET_PASSWORD = "correct-horse-battery-staple"
        const val OWN_PASSWORD = "this-device-password"
    }
}
