@file:OptIn(ExperimentalUnsignedTypes::class)

package com.emberr.domain.cloud.crypto

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.aead.AeadCorrupedOrTamperedDataException
import com.ionspin.kotlin.crypto.aead.AuthenticatedEncryptionWithAssociatedData
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_ABYTES
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_KEYBYTES
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
import com.ionspin.kotlin.crypto.box.Box
import com.ionspin.kotlin.crypto.box.BoxCorruptedOrTamperedDataException
import com.ionspin.kotlin.crypto.box.crypto_box_PUBLICKEYBYTES
import com.ionspin.kotlin.crypto.box.crypto_box_SEALBYTES
import com.ionspin.kotlin.crypto.box.crypto_box_SECRETKEYBYTES
import com.ionspin.kotlin.crypto.generichash.GenericHash
import com.ionspin.kotlin.crypto.hash.Hash
import com.ionspin.kotlin.crypto.kdf.Kdf
import com.ionspin.kotlin.crypto.kdf.crypto_kdf_CONTEXTBYTES
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_SALTBYTES
import com.ionspin.kotlin.crypto.pwhash.crypto_pwhash_argon2id_ALG_ARGON2ID13
import com.ionspin.kotlin.crypto.util.LibsodiumRandom
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CloudEncryptedBytes(val cipherText: ByteArray, val nonce: ByteArray)

class CloudKeyPair(val publicKey: ByteArray, val secretKey: ByteArray)

private val libsodiumStartLock = Mutex()

class CloudCrypto {

    suspend fun randomBytes(size: Int): ByteArray {
        startLibsodiumIfNeeded()
        return LibsodiumRandom.buf(size).asByteArray()
    }

    suspend fun deriveKeyFromPassword(password: String, salt: ByteArray, memLimitBytes: Long, opsLimit: Int): ByteArray {
        require(salt.size == crypto_pwhash_SALTBYTES) { "Argon2id needs a $crypto_pwhash_SALTBYTES-byte salt" }
        startLibsodiumIfNeeded()
        return runArgon2id(
            passwordBytes = password.encodeToByteArray(),
            salt = salt,
            opsLimit = opsLimit,
            memLimitBytes = memLimitBytes,
            algorithm = crypto_pwhash_argon2id_ALG_ARGON2ID13,
            keySize = KEY_SIZE
        )
    }

    suspend fun deriveSubKey(key: ByteArray, subKeyId: Int, context: String, subKeySize: Int): ByteArray {
        require(key.size == KEY_SIZE) { "A sub-key needs a $KEY_SIZE-byte key" }
        require(context.encodeToByteArray().size == crypto_kdf_CONTEXTBYTES) { "A sub-key context must be exactly $crypto_kdf_CONTEXTBYTES bytes" }
        startLibsodiumIfNeeded()
        return Kdf.deriveFromKey(subKeyId.toUInt(), subKeySize, context, key.asUByteArray()).asByteArray()
    }

    suspend fun encrypt(plainText: ByteArray, key: ByteArray, associatedData: ByteArray): CloudEncryptedBytes {
        require(key.size == crypto_aead_xchacha20poly1305_ietf_KEYBYTES) { "Encryption needs a $KEY_SIZE-byte key" }
        startLibsodiumIfNeeded()
        val nonce = LibsodiumRandom.buf(crypto_aead_xchacha20poly1305_ietf_NPUBBYTES)
        val cipherText = AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfEncrypt(
            message = plainText.asUByteArray(),
            associatedData = associatedData.asUByteArray(),
            nonce = nonce,
            key = key.asUByteArray()
        )
        return CloudEncryptedBytes(cipherText = cipherText.asByteArray(), nonce = nonce.asByteArray())
    }

    suspend fun decrypt(cipherText: ByteArray, nonce: ByteArray, key: ByteArray, associatedData: ByteArray): ByteArray {
        startLibsodiumIfNeeded()
        val sizesAreValid = cipherText.size >= crypto_aead_xchacha20poly1305_ietf_ABYTES &&
            nonce.size == crypto_aead_xchacha20poly1305_ietf_NPUBBYTES &&
            key.size == crypto_aead_xchacha20poly1305_ietf_KEYBYTES
        if (!sizesAreValid) throw CloudDecryptionException()
        return try {
            AuthenticatedEncryptionWithAssociatedData.xChaCha20Poly1305IetfDecrypt(
                ciphertextAndTag = cipherText.asUByteArray(),
                associatedData = associatedData.asUByteArray(),
                nonce = nonce.asUByteArray(),
                key = key.asUByteArray()
            ).asByteArray()
        } catch (cause: AeadCorrupedOrTamperedDataException) {
            throw CloudDecryptionException(cause)
        }
    }

    suspend fun newKeyPair(): CloudKeyPair {
        startLibsodiumIfNeeded()
        val keyPair = Box.keypair()
        return CloudKeyPair(publicKey = keyPair.publicKey.asByteArray(), secretKey = keyPair.secretKey.asByteArray())
    }

    suspend fun openSealedBox(sealedBox: ByteArray, publicKey: ByteArray, secretKey: ByteArray): ByteArray {
        startLibsodiumIfNeeded()
        val sizesAreValid = sealedBox.size >= crypto_box_SEALBYTES &&
            publicKey.size == crypto_box_PUBLICKEYBYTES &&
            secretKey.size == crypto_box_SECRETKEYBYTES
        if (!sizesAreValid) throw CloudDecryptionException()
        return try {
            Box.sealOpen(sealedBox.asUByteArray(), publicKey.asUByteArray(), secretKey.asUByteArray()).asByteArray()
        } catch (cause: BoxCorruptedOrTamperedDataException) {
            throw CloudDecryptionException(cause)
        }
    }

    suspend fun keyedHash(message: ByteArray, key: ByteArray, hashSize: Int): ByteArray {
        startLibsodiumIfNeeded()
        return GenericHash.genericHash(message.asUByteArray(), hashSize, key.asUByteArray()).asByteArray()
    }

    suspend fun sha256(data: ByteArray): ByteArray {
        startLibsodiumIfNeeded()
        return Hash.sha256(data.asUByteArray()).asByteArray()
    }

    private suspend fun startLibsodiumIfNeeded() {
        libsodiumStartLock.withLock {
            if (!LibsodiumInitializer.isInitialized()) {
                LibsodiumInitializer.initialize()
            }
        }
    }

    companion object {
        const val KEY_SIZE = 32
    }
}
