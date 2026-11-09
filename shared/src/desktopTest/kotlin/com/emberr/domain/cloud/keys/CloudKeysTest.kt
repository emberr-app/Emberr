package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CloudKeysTest {

    private val cloudCrypto = CloudCrypto()
    private val accountKeyManager = CloudAccountKeyManager(cloudCrypto)
    private val itemKeyManager = CloudItemKeyManager(cloudCrypto)
    private val recoveryWords = CloudRecoveryWords(cloudCrypto)
    private val dailyNoteIds = CloudDailyNoteIds(cloudCrypto)
    private val mediaCrypto = CloudMediaCrypto(cloudCrypto)

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
    fun aDailyNoteIdDependsOnTheAccountKeyAndDate() = runTest {
        val accountKey = itemKeyManager.newItemKey()
        val otherAccountKey = itemKeyManager.newItemKey()

        val dailyNoteId = dailyNoteIds.dailyNoteId(accountKey, "2026-10-18")

        assertEquals(dailyNoteId, dailyNoteIds.dailyNoteId(accountKey, "2026-10-18"))
        assertNotEquals(dailyNoteId, dailyNoteIds.dailyNoteId(otherAccountKey, "2026-10-18"))
        assertTrue(Regex("daily_[0-9a-f]{32}").matches(dailyNoteId))
    }

    @Test
    fun aMediaIdAndKeyDependOnlyOnTheAccountKeyAndFileName() = runTest {
        val accountKey = itemKeyManager.newItemKey()
        val otherDeviceSameAccountKey = accountKey.copyOf()

        val mediaId = mediaCrypto.mediaId(accountKey, "media_4f1c.jpg")
        val otherFileMediaId = mediaCrypto.mediaId(accountKey, "media_9a2e.jpg")

        assertTrue(Regex("media_[0-9a-f]{32}").matches(mediaId))
        assertEquals(mediaId, mediaCrypto.mediaId(otherDeviceSameAccountKey, "media_4f1c.jpg"))
        assertNotEquals(mediaId, otherFileMediaId)
        val mediaKey = mediaCrypto.mediaKey(accountKey, mediaId)
        assertEquals(32, mediaKey.size)
        assertContentEquals(mediaKey, mediaCrypto.mediaKey(otherDeviceSameAccountKey, mediaId))
        assertFalse(mediaKey.contentEquals(mediaCrypto.mediaKey(accountKey, otherFileMediaId)))
    }

    @Test
    fun aMediaPieceOpensOnlyAtItsOwnIndexAndCount() = runTest {
        val mediaId = "media_00112233445566778899aabbccddeeff"
        val mediaKey = mediaCrypto.mediaKey(itemKeyManager.newItemKey(), mediaId)
        val piece = ByteArray(1000) { it.toByte() }

        val encryptedPiece = mediaCrypto.encryptPiece(piece, mediaKey, mediaId, pieceIndex = 1, pieceCount = 3)

        assertEquals(piece.size + 40, encryptedPiece.size)
        assertContentEquals(piece, mediaCrypto.decryptPiece(encryptedPiece, mediaKey, mediaId, pieceIndex = 1, pieceCount = 3))
        assertFailsWith<CloudDecryptionException> {
            mediaCrypto.decryptPiece(encryptedPiece, mediaKey, mediaId, pieceIndex = 2, pieceCount = 3)
        }
        assertFailsWith<CloudDecryptionException> {
            mediaCrypto.decryptPiece(encryptedPiece, mediaKey, mediaId, pieceIndex = 1, pieceCount = 2)
        }
    }

    @Test
    fun encryptedPieceSizesAddFortyBytesToEveryEightMebibytePiece() {
        val eightMebibytes = 8L * 1024 * 1024

        assertEquals(listOf(40L), encryptedMediaPieceSizes(0L))
        assertEquals(listOf(eightMebibytes + 40), encryptedMediaPieceSizes(eightMebibytes))
        assertEquals(listOf(eightMebibytes + 40, 41L), encryptedMediaPieceSizes(eightMebibytes + 1))
    }
}
