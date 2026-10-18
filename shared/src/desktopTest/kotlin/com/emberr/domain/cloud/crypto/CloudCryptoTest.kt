@file:OptIn(ExperimentalUnsignedTypes::class)

package com.emberr.domain.cloud.crypto

import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.pwhash.PasswordHash
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_MEMLIMIT_MIN
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_argon2id_ALG_ARGON2ID13
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class CloudCryptoTest {

    private val cloudCrypto = CloudCrypto()

    @Test
    fun encryptedBytesOpenOnlyWithTheSameKeyAndAssociatedData() = runTest {
        val key = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)
        val wrongKey = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)
        val plainText = "a note key".encodeToByteArray()
        val associatedData = "emberr-cloud:v1:lock:note-1:folder-1".encodeToByteArray()
        val wrongAssociatedData = "emberr-cloud:v1:lock:note-1:folder-2".encodeToByteArray()

        val encrypted = cloudCrypto.encrypt(plainText, key, associatedData)

        assertContentEquals(plainText, cloudCrypto.decrypt(encrypted.cipherText, encrypted.nonce, key, associatedData))
        assertFailsWith<CloudDecryptionException> {
            cloudCrypto.decrypt(encrypted.cipherText, encrypted.nonce, key, wrongAssociatedData)
        }
        assertFailsWith<CloudDecryptionException> {
            cloudCrypto.decrypt(encrypted.cipherText, encrypted.nonce, wrongKey, associatedData)
        }
    }

    @Test
    fun aSealedBoxOpensWithTheMatchingKeyPair() = runTest {
        val keyPair = cloudCrypto.newKeyPair()
        val message = "a shared folder key".encodeToByteArray()

        val sealedBox = Box.seal(message.asUByteArray(), keyPair.publicKey.asUByteArray()).asByteArray()

        assertContentEquals(message, cloudCrypto.openSealedBox(sealedBox, keyPair.publicKey, keyPair.secretKey))
    }

    @Test
    fun aSubKeyDependsOnlyOnTheKeyIdAndContext() = runTest {
        val key = cloudCrypto.randomBytes(CloudCrypto.KEY_SIZE)

        val firstSubKey = cloudCrypto.deriveSubKey(key, subKeyId = 1, context = "loginctx", subKeySize = CloudCrypto.KEY_SIZE)
        val sameSubKey = cloudCrypto.deriveSubKey(key, subKeyId = 1, context = "loginctx", subKeySize = CloudCrypto.KEY_SIZE)
        val otherIdSubKey = cloudCrypto.deriveSubKey(key, subKeyId = 2, context = "loginctx", subKeySize = CloudCrypto.KEY_SIZE)

        assertContentEquals(firstSubKey, sameSubKey)
        assertFalse(firstSubKey.contentEquals(otherIdSubKey))
    }

    @Test
    fun argon2idHashesEveryUtf8ByteOfThePassword() = runTest {
        val salt = cloudCrypto.randomBytes(16)
        val opsLimit = 1

        val asciiKey = cloudCrypto.deriveKeyFromPassword("password", salt, crypto_pwhash_MEMLIMIT_MIN.toLong(), opsLimit)
        val libraryAsciiKey = PasswordHash.pwhash(
            CloudCrypto.KEY_SIZE,
            "password",
            salt.asUByteArray(),
            opsLimit.toULong(),
            crypto_pwhash_MEMLIMIT_MIN,
            crypto_pwhash_argon2id_ALG_ARGON2ID13
        ).asByteArray()
        assertContentEquals(libraryAsciiKey, asciiKey)

        val firstNonAsciiKey = cloudCrypto.deriveKeyFromPassword("pässworda", salt, crypto_pwhash_MEMLIMIT_MIN.toLong(), opsLimit)
        val secondNonAsciiKey = cloudCrypto.deriveKeyFromPassword("pässwordb", salt, crypto_pwhash_MEMLIMIT_MIN.toLong(), opsLimit)
        assertFalse(firstNonAsciiKey.contentEquals(secondNonAsciiKey))
    }
}
