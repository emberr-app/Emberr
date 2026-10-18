package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CloudKeysTest {

    private val cloudCrypto = CloudCrypto()
    private val accountKeyManager = CloudAccountKeyManager(cloudCrypto)
    private val itemKeyManager = CloudItemKeyManager(cloudCrypto)
    private val recoveryWords = CloudRecoveryWords(cloudCrypto)
    private val dailyNoteIds = CloudDailyNoteIds(cloudCrypto)

    @Test
    fun loggingInWithTheSignupPasswordUnlocksTheSameKeys() = runTest {
        val signupKeys = accountKeyManager.createKeysForSignup("correct horse")
        val accountKeys = signupKeys.accountKeys

        assertEquals(16, accountKeys.salt.size)
        assertEquals(32, accountKeys.publicKey.size)
        listOf(
            accountKeys.encryptedMasterKey,
            accountKeys.masterKeyEncryptedWithRecoveryKey,
            accountKeys.recoveryKeyEncryptedWithMasterKey,
            accountKeys.encryptedPrivateKey
        ).forEach { lockedKey -> assertEquals(48, lockedKey.size) }
        listOf(
            accountKeys.encryptedMasterKeyNonce,
            accountKeys.masterKeyEncryptedWithRecoveryKeyNonce,
            accountKeys.recoveryKeyEncryptedWithMasterKeyNonce,
            accountKeys.encryptedPrivateKeyNonce
        ).forEach { nonce -> assertEquals(24, nonce.size) }

        val passwordKey = accountKeyManager.derivePasswordKey(
            "correct horse",
            accountKeys.salt,
            accountKeys.memLimit,
            accountKeys.opsLimit
        )
        val masterKey = accountKeyManager.unlockMasterKeyWithPasswordKey(passwordKey, accountKeys)
        val privateKey = accountKeyManager.unlockPrivateKey(
            masterKey,
            accountKeys.encryptedPrivateKey,
            accountKeys.encryptedPrivateKeyNonce
        )

        assertContentEquals(signupKeys.loginKey, accountKeyManager.deriveLoginKey(passwordKey))
        assertContentEquals(signupKeys.masterKey, masterKey)
        assertContentEquals(signupKeys.privateKey, privateKey)

        val wrongPasswordKey = accountKeyManager.derivePasswordKey(
            "wrong horse",
            accountKeys.salt,
            accountKeys.memLimit,
            accountKeys.opsLimit
        )
        assertFailsWith<IncorrectCloudPasswordException> {
            accountKeyManager.unlockMasterKeyWithPasswordKey(wrongPasswordKey, accountKeys)
        }
    }

    @Test
    fun recoveryWordsUnlockTheMasterKeyAndANewPasswordReplacesTheOldOne() = runTest {
        val signupKeys = accountKeyManager.createKeysForSignup("old password")
        val accountKeys = signupKeys.accountKeys
        val shownWords = recoveryWords.wordsFromRecoveryKey(signupKeys.recoveryKey)
        val recoveryKeys = CloudRecoveryKeys(
            masterKeyEncryptedWithRecoveryKey = accountKeys.masterKeyEncryptedWithRecoveryKey,
            masterKeyEncryptedWithRecoveryKeyNonce = accountKeys.masterKeyEncryptedWithRecoveryKeyNonce,
            publicKey = accountKeys.publicKey,
            encryptedPrivateKey = accountKeys.encryptedPrivateKey,
            encryptedPrivateKeyNonce = accountKeys.encryptedPrivateKeyNonce
        )

        val typedRecoveryKey = recoveryWords.recoveryKeyFromWords(shownWords)
        val recoveredMasterKey = accountKeyManager.unlockMasterKeyWithRecoveryKey(typedRecoveryKey, recoveryKeys)
        val recoveredPrivateKey = accountKeyManager.unlockPrivateKey(
            recoveredMasterKey,
            recoveryKeys.encryptedPrivateKey,
            recoveryKeys.encryptedPrivateKeyNonce
        )
        assertContentEquals(signupKeys.masterKey, recoveredMasterKey)
        assertContentEquals(signupKeys.privateKey, recoveredPrivateKey)
        assertContentEquals(signupKeys.recoveryKey, accountKeyManager.readRecoveryKey(recoveredMasterKey, accountKeys))

        val newPasswordKeys = accountKeyManager.createKeysForNewPassword("new password", recoveredMasterKey)
        val passwordKeys = newPasswordKeys.passwordKeys
        val keysAfterChange = CloudAccountKeys(
            salt = passwordKeys.salt,
            memLimit = passwordKeys.memLimit,
            opsLimit = passwordKeys.opsLimit,
            encryptedMasterKey = passwordKeys.encryptedMasterKey,
            encryptedMasterKeyNonce = passwordKeys.encryptedMasterKeyNonce,
            masterKeyEncryptedWithRecoveryKey = accountKeys.masterKeyEncryptedWithRecoveryKey,
            masterKeyEncryptedWithRecoveryKeyNonce = accountKeys.masterKeyEncryptedWithRecoveryKeyNonce,
            recoveryKeyEncryptedWithMasterKey = accountKeys.recoveryKeyEncryptedWithMasterKey,
            recoveryKeyEncryptedWithMasterKeyNonce = accountKeys.recoveryKeyEncryptedWithMasterKeyNonce,
            publicKey = accountKeys.publicKey,
            encryptedPrivateKey = accountKeys.encryptedPrivateKey,
            encryptedPrivateKeyNonce = accountKeys.encryptedPrivateKeyNonce
        )

        val oldPasswordKey = accountKeyManager.derivePasswordKey(
            "old password",
            keysAfterChange.salt,
            keysAfterChange.memLimit,
            keysAfterChange.opsLimit
        )
        assertFailsWith<IncorrectCloudPasswordException> {
            accountKeyManager.unlockMasterKeyWithPasswordKey(oldPasswordKey, keysAfterChange)
        }
        val newPasswordKey = accountKeyManager.derivePasswordKey(
            "new password",
            keysAfterChange.salt,
            keysAfterChange.memLimit,
            keysAfterChange.opsLimit
        )
        assertContentEquals(newPasswordKeys.loginKey, accountKeyManager.deriveLoginKey(newPasswordKey))
        assertContentEquals(
            signupKeys.masterKey,
            accountKeyManager.unlockMasterKeyWithPasswordKey(newPasswordKey, keysAfterChange)
        )
    }

    @Test
    fun aMovedNoteKeepsItsContentAndOpensOnlyFromTheNewFolder() = runTest {
        val oldFolderKey = itemKeyManager.newItemKey()
        val newFolderKey = itemKeyManager.newItemKey()
        val noteKey = itemKeyManager.newItemKey()
        val noteContent = "blocks of the note".encodeToByteArray()
        val encryptedContent = itemKeyManager.encryptContent(noteContent, noteKey, CloudItemType.NOTE, "note-1")
        val lockedUnderOldFolder = itemKeyManager.lockChildKey(noteKey, "note-1", oldFolderKey, "folder-old")

        val lockedUnderNewFolder = itemKeyManager.relockChildKeyForMove(
            lockedUnderOldFolder,
            "note-1",
            oldFolderKey,
            "folder-old",
            newFolderKey,
            "folder-new"
        )

        val unlockedNoteKey = itemKeyManager.unlockChildKey(lockedUnderNewFolder, "note-1", newFolderKey, "folder-new")
        assertContentEquals(
            noteContent,
            itemKeyManager.decryptContent(encryptedContent, unlockedNoteKey, CloudItemType.NOTE, "note-1")
        )
        assertFailsWith<CloudDecryptionException> {
            itemKeyManager.unlockChildKey(lockedUnderNewFolder, "note-1", oldFolderKey, "folder-new")
        }
    }

    @Test
    fun recoveryWordsMatchTheOfficialVectorsAndRefuseAChangedWord() = runTest {
        val allZeros = ByteArray(CloudCrypto.KEY_SIZE)
        val allOnes = ByteArray(CloudCrypto.KEY_SIZE) { 0xff.toByte() }
        val allZerosWords = List(23) { "abandon" }.joinToString(" ") + " art"
        val allOnesWords = List(23) { "zoo" }.joinToString(" ") + " vote"

        assertEquals(allZerosWords, recoveryWords.wordsFromRecoveryKey(allZeros))
        assertEquals(allOnesWords, recoveryWords.wordsFromRecoveryKey(allOnes))
        assertContentEquals(allZeros, recoveryWords.recoveryKeyFromWords(allZerosWords))
        assertContentEquals(allOnes, recoveryWords.recoveryKeyFromWords("  ${allOnesWords.uppercase()}\n"))

        val changedWords = "ability" + allZerosWords.removePrefix("abandon")
        assertFailsWith<InvalidRecoveryWordsException> {
            recoveryWords.recoveryKeyFromWords(changedWords)
        }
    }

    @Test
    fun aDailyNoteIdDependsOnTheSpaceKeyAndDate() = runTest {
        val spaceKey = itemKeyManager.newItemKey()
        val otherSpaceKey = itemKeyManager.newItemKey()

        val dailyNoteId = dailyNoteIds.dailyNoteId(spaceKey, "2026-10-18")

        assertEquals(dailyNoteId, dailyNoteIds.dailyNoteId(spaceKey, "2026-10-18"))
        assertNotEquals(dailyNoteId, dailyNoteIds.dailyNoteId(otherSpaceKey, "2026-10-18"))
        assertTrue(Regex("daily_[0-9a-f]{32}").matches(dailyNoteId))
    }
}
