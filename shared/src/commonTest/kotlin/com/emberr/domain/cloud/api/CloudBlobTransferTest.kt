package com.emberr.domain.cloud.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CloudBlobTransferTest {

    @Test
    fun uploadPutsTheBytesToTheSignedUrlWithTheirExactLengthAndNoCloudHeaders() = runTest {
        val signedUrl = "http://192.168.1.5:3900/emberr/items/7/note-1/ABC" +
            "?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=GK1%2F20261024%2Fgarage%2Fs3%2Faws4_request" +
            "&X-Amz-Expires=900&X-Amz-SignedHeaders=content-length%3Bhost&X-Amz-Signature=abc123"
        var methodSent: HttpMethod? = null
        var urlSent: String? = null
        var tokenSent: String? = null
        var clientVersionSent: String? = null
        var contentLengthSent: Long? = null
        var bytesSent: ByteArray? = null
        val transfer = CloudBlobTransfer(
            HttpClient(
                MockEngine { request ->
                    methodSent = request.method
                    urlSent = request.url.toString()
                    tokenSent = request.headers["X-Auth-Token"]
                    clientVersionSent = request.headers["X-Client-Version"]
                    contentLengthSent = request.body.contentLength
                    bytesSent = request.body.toByteArray()
                    respondOk()
                }
            ) { configureForCloudBlobTransfer() }
        )

        transfer.upload(signedUrl, byteArrayOf(1, 2, 3, 4, 5))

        assertEquals(HttpMethod.Put, methodSent)
        assertEquals(signedUrl, urlSent)
        assertNull(tokenSent)
        assertNull(clientVersionSent)
        assertEquals(5L, contentLengthSent)
        assertContentEquals(byteArrayOf(1, 2, 3, 4, 5), bytesSent)
    }
}
