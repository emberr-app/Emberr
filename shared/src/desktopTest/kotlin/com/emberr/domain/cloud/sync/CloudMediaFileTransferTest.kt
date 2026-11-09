package com.emberr.domain.cloud.sync

import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudBlobTransfer
import com.emberr.domain.cloud.api.CloudMediaDownloadUrls
import com.emberr.domain.cloud.api.CloudMediaPieceDownloadUrl
import com.emberr.domain.cloud.api.configureForCloudBlobTransfer
import com.emberr.domain.cloud.crypto.CloudCrypto
import com.emberr.domain.cloud.keys.CloudMediaCrypto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Clock

class CloudMediaFileTransferTest {

    private val workingDirectory = Files.createTempDirectory("emberr-cloud-media-transfer").toFile()
    private val mediaCrypto = CloudMediaCrypto(CloudCrypto())

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    @Test
    fun aDownloadWithOneFullPieceAlreadyOnDiskFetchesOnlyTheRestAndWritesTheWholeFile() = runTest {
        val mediaId = "media_test"
        val mediaKey = Random(1).nextBytes(32)
        val plainFile = Random(2).nextBytes(8 * 1024 * 1024 + 100)
        val firstPiece = plainFile.copyOfRange(0, 8 * 1024 * 1024)
        val secondPiece = plainFile.copyOfRange(8 * 1024 * 1024, plainFile.size)
        val encryptedPiecesByUrl = mapOf(
            "http://storage/piece0" to mediaCrypto.encryptPiece(firstPiece, mediaKey, mediaId, 0, 2),
            "http://storage/piece1" to mediaCrypto.encryptPiece(secondPiece, mediaKey, mediaId, 1, 2)
        )
        val destinationFile = File(workingDirectory, "photo.jpg")
        File(workingDirectory, "photo.jpg.partial.tmp").writeBytes(firstPiece + byteArrayOf(9, 9, 9))

        val requestedUrls = mutableListOf<String>()
        val transfer = CloudMediaFileTransfer(
            apiClient = CloudApiClient(
                httpClient = HttpClient(MockEngine { error("No API call was expected") }),
                serverAddress = { "http://cloud" },
                appVersion = "1.0"
            ),
            blobTransfer = CloudBlobTransfer(
                HttpClient(
                    MockEngine { request ->
                        val url = request.url.toString()
                        requestedUrls += url
                        respond(encryptedPiecesByUrl.getValue(url))
                    }
                ) { configureForCloudBlobTransfer() }
            ),
            mediaCrypto = mediaCrypto
        )
        val fifteenMinutesFromNow = (Clock.System.now().toEpochMilliseconds() + 15 * 60 * 1000) * 1000
        val answer = CloudMediaDownloadUrls(
            pieces = encryptedPiecesByUrl.entries.mapIndexed { index, (url, encryptedPiece) ->
                CloudMediaPieceDownloadUrl(index = index, downloadUrl = url, sizeBytes = encryptedPiece.size.toLong())
            },
            expiresAtMicroseconds = fifteenMinutesFromNow
        )

        transfer.download("token", mediaId, mediaKey, answer, destinationFile) {}

        assertEquals(listOf("http://storage/piece1"), requestedUrls)
        assertContentEquals(plainFile, destinationFile.readBytes())
        assertFalse(File(workingDirectory, "photo.jpg.partial.tmp").exists())
    }
}
