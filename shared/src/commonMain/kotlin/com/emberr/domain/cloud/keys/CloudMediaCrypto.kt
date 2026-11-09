package com.emberr.domain.cloud.keys

import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.selfhost.webdav.mediaPieceCount
import com.emberr.domain.selfhost.webdav.mediaPieceLength
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_ABYTES
import com.ionspin.kotlin.crypto.aead.crypto_aead_xchacha20poly1305_ietf_NPUBBYTES

private val PIECE_NONCE_SIZE = crypto_aead_xchacha20poly1305_ietf_NPUBBYTES
private val ENCRYPTED_PIECE_EXTRA_BYTES = PIECE_NONCE_SIZE + crypto_aead_xchacha20poly1305_ietf_ABYTES

fun encryptedMediaPieceSizes(fileSizeBytes: Long): List<Long> =
    List(mediaPieceCount(fileSizeBytes)) { pieceIndex ->
        mediaPieceLength(pieceIndex, fileSizeBytes) + ENCRYPTED_PIECE_EXTRA_BYTES.toLong()
    }

class CloudMediaCrypto(private val cloudCrypto: CloudCrypto) {

    suspend fun mediaId(accountKey: ByteArray, mediaFileName: String): String {
        val mediaIdKey = cloudCrypto.deriveSubKey(accountKey, MEDIA_ID_KEY_ID, MEDIA_ID_KEY_CONTEXT, CloudCrypto.KEY_SIZE)
        val fileNameHash = cloudCrypto.keyedHash(mediaFileName.encodeToByteArray(), mediaIdKey, FILE_NAME_HASH_SIZE)
        return MEDIA_ID_PREFIX + fileNameHash.joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    suspend fun mediaKey(accountKey: ByteArray, mediaId: String): ByteArray {
        val mediaKeyRoot = cloudCrypto.deriveSubKey(accountKey, MEDIA_KEY_ROOT_ID, MEDIA_KEY_ROOT_CONTEXT, CloudCrypto.KEY_SIZE)
        return cloudCrypto.keyedHash(mediaId.encodeToByteArray(), mediaKeyRoot, CloudCrypto.KEY_SIZE)
    }

    suspend fun encryptPiece(
        piece: ByteArray,
        mediaKey: ByteArray,
        mediaId: String,
        pieceIndex: Int,
        pieceCount: Int
    ): ByteArray {
        val encryptedPiece = cloudCrypto.encrypt(piece, mediaKey, pieceAssociatedData(mediaId, pieceIndex, pieceCount))
        return encryptedPiece.nonce + encryptedPiece.cipherText
    }

    suspend fun decryptPiece(
        encryptedPiece: ByteArray,
        mediaKey: ByteArray,
        mediaId: String,
        pieceIndex: Int,
        pieceCount: Int
    ): ByteArray {
        if (encryptedPiece.size < PIECE_NONCE_SIZE) throw CloudDecryptionException()
        return cloudCrypto.decrypt(
            cipherText = encryptedPiece.copyOfRange(PIECE_NONCE_SIZE, encryptedPiece.size),
            nonce = encryptedPiece.copyOfRange(0, PIECE_NONCE_SIZE),
            key = mediaKey,
            associatedData = pieceAssociatedData(mediaId, pieceIndex, pieceCount)
        )
    }

    private fun pieceAssociatedData(mediaId: String, pieceIndex: Int, pieceCount: Int): ByteArray =
        "emberr-cloud:v1:media:$mediaId:$pieceIndex:$pieceCount".encodeToByteArray()

    companion object {
        private const val MEDIA_ID_KEY_ID = 2
        private const val MEDIA_ID_KEY_CONTEXT = "mediaids"
        private const val MEDIA_KEY_ROOT_ID = 3
        private const val MEDIA_KEY_ROOT_CONTEXT = "mediakey"
        private const val FILE_NAME_HASH_SIZE = 16
        private const val MEDIA_ID_PREFIX = "media_"
    }
}
