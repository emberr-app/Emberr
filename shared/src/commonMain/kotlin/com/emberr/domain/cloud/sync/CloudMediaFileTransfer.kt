package com.emberr.domain.cloud.sync

import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudBlobTransfer
import com.emberr.domain.cloud.api.CloudMediaDownloadUrls
import com.emberr.domain.cloud.api.CloudMediaUploadUrls
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.cloud.keys.CloudMediaCrypto
import com.emberr.domain.cloud.keys.encryptedMediaPieceSizes
import com.emberr.domain.cloud.keys.plainMediaSizeOf
import com.emberr.domain.selfhost.webdav.PARTIAL_DOWNLOAD_SUFFIX
import com.emberr.domain.selfhost.webdav.mediaPieceCount
import com.emberr.domain.selfhost.webdav.mediaPieceLength
import com.emberr.domain.selfhost.webdav.mediaPieceStart
import com.emberr.domain.selfhost.webdav.piecesAlreadyDownloaded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.time.Clock

private const val MICROSECONDS_PER_MILLISECOND = 1_000L
private const val ASK_FOR_NEW_URLS_WHEN_MICROSECONDS_LEFT = 60L * 1_000_000

class CloudMediaFileTransfer(
    private val apiClient: CloudApiClient,
    private val blobTransfer: CloudBlobTransfer,
    private val mediaCrypto: CloudMediaCrypto
) {

    suspend fun upload(
        sessionToken: String,
        mediaId: String,
        mediaKey: ByteArray,
        sourceFile: File,
        beforeEachPiece: () -> Unit,
        onBytesDone: (Long) -> Unit
    ) {
        val fileSizeBytes = withContext(Dispatchers.IO) { sourceFile.length() }
        val pieceSizes = encryptedMediaPieceSizes(fileSizeBytes)
        val pieceCount = pieceSizes.size
        var answer = apiClient.requestMediaUploadUrls(sessionToken, mediaId, pieceSizes)
        var answerIsFresh = true
        if (answer.committed) return
        var bytesDone = bytesAlreadyOnServer(answer, fileSizeBytes)
        onBytesDone(bytesDone)

        var piecesToSend = answer.pieces
        while (piecesToSend.isNotEmpty()) {
            if (!answerIsFresh && expiresWithinAMinute(answer.expiresAtMicroseconds)) {
                answer = apiClient.requestMediaUploadUrls(sessionToken, mediaId, pieceSizes)
                answerIsFresh = true
                if (answer.committed) return
                piecesToSend = answer.pieces
                bytesDone = bytesAlreadyOnServer(answer, fileSizeBytes)
                continue
            }
            beforeEachPiece()
            val piece = piecesToSend.first()
            val pieceLength = mediaPieceLength(piece.index, fileSizeBytes).toLong()
            val plainPiece = withContext(Dispatchers.IO) { readPiece(sourceFile, piece.index, fileSizeBytes) }
            val encryptedPiece = mediaCrypto.encryptPiece(plainPiece, mediaKey, mediaId, piece.index, pieceCount)
            val bytesDoneBeforePiece = bytesDone
            blobTransfer.upload(piece.uploadUrl, encryptedPiece) { bytesSent ->
                onBytesDone(bytesDoneBeforePiece + pieceLength * bytesSent / encryptedPiece.size)
            }
            bytesDone += pieceLength
            onBytesDone(bytesDone)
            piecesToSend = piecesToSend.drop(1)
            answerIsFresh = false
        }
        apiClient.commitMedia(sessionToken, mediaId)
    }

    suspend fun download(
        sessionToken: String,
        mediaId: String,
        mediaKey: ByteArray,
        firstAnswer: CloudMediaDownloadUrls,
        destinationFile: File,
        beforeEachPiece: () -> Unit,
        onBytesDone: (Long) -> Unit
    ) {
        if (withContext(Dispatchers.IO) { destinationFile.exists() }) return
        var answer = firstAnswer
        var answerIsFresh = false
        val fileSizeBytes = plainMediaSizeOf(answer.pieces.map { it.sizeBytes })
        val pieceCount = mediaPieceCount(fileSizeBytes)
        if (answer.pieces.size != pieceCount) throw CloudDecryptionException()
        val partialFile = File(destinationFile.parentFile, "${destinationFile.name}$PARTIAL_DOWNLOAD_SUFFIX")

        val piecesDone = withContext(Dispatchers.IO) {
            destinationFile.parentFile?.mkdirs()
            val alreadyDownloaded = piecesAlreadyDownloaded(if (partialFile.exists()) partialFile.length() else 0L, fileSizeBytes)
            RandomAccessFile(partialFile, "rw").use { it.setLength(minOf(mediaPieceStart(alreadyDownloaded), fileSizeBytes)) }
            alreadyDownloaded
        }
        onBytesDone(minOf(mediaPieceStart(piecesDone), fileSizeBytes))

        for (pieceIndex in piecesDone until pieceCount) {
            beforeEachPiece()
            if (!answerIsFresh && expiresWithinAMinute(answer.expiresAtMicroseconds)) {
                answer = apiClient.requestMediaDownloadUrls(sessionToken, mediaId)
                answerIsFresh = true
            }
            val piece = answer.pieces.first { it.index == pieceIndex }
            val pieceStart = mediaPieceStart(pieceIndex)
            val pieceLength = mediaPieceLength(pieceIndex, fileSizeBytes).toLong()
            val encryptedPiece = blobTransfer.download(piece.downloadUrl) { bytesReceived ->
                if (piece.sizeBytes > 0) onBytesDone(pieceStart + pieceLength * bytesReceived / piece.sizeBytes)
            }
            val plainPiece = mediaCrypto.decryptPiece(encryptedPiece, mediaKey, mediaId, pieceIndex, pieceCount)
            check(plainPiece.size == mediaPieceLength(pieceIndex, fileSizeBytes)) {
                "Piece $pieceIndex of $mediaId has the wrong size"
            }
            withContext(Dispatchers.IO) { FileOutputStream(partialFile, true).use { it.write(plainPiece) } }
            onBytesDone(pieceStart + pieceLength)
            answerIsFresh = false
        }

        withContext(Dispatchers.IO) {
            Files.move(
                partialFile.toPath(),
                destinationFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE
            )
        }
    }

    private fun bytesAlreadyOnServer(answer: CloudMediaUploadUrls, fileSizeBytes: Long): Long =
        fileSizeBytes - answer.pieces.sumOf { piece -> mediaPieceLength(piece.index, fileSizeBytes).toLong() }

    private fun expiresWithinAMinute(expiresAtMicroseconds: Long?): Boolean {
        if (expiresAtMicroseconds == null) return false
        val nowMicroseconds = Clock.System.now().toEpochMilliseconds() * MICROSECONDS_PER_MILLISECOND
        return expiresAtMicroseconds - nowMicroseconds < ASK_FOR_NEW_URLS_WHEN_MICROSECONDS_LEFT
    }

    private fun readPiece(sourceFile: File, pieceIndex: Int, fileSizeBytes: Long): ByteArray {
        val piece = ByteArray(mediaPieceLength(pieceIndex, fileSizeBytes))
        RandomAccessFile(sourceFile, "r").use { file ->
            file.seek(mediaPieceStart(pieceIndex))
            file.readFully(piece)
        }
        return piece
    }
}
