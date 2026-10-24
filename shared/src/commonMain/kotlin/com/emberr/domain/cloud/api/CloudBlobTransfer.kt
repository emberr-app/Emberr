package com.emberr.domain.cloud.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.io.IOException

private const val BLOB_REQUEST_TIMEOUT_MS = 10L * 60 * 1000
private const val BLOB_CONNECT_TIMEOUT_MS = 15_000L
private const val BLOB_SOCKET_TIMEOUT_MS = 2L * 60 * 1000

internal fun HttpClientConfig<*>.configureForCloudBlobTransfer() {
    expectSuccess = false
    install(HttpTimeout) {
        requestTimeoutMillis = BLOB_REQUEST_TIMEOUT_MS
        connectTimeoutMillis = BLOB_CONNECT_TIMEOUT_MS
        socketTimeoutMillis = BLOB_SOCKET_TIMEOUT_MS
    }
}

class CloudBlobTransferException(val httpStatus: Int) :
    Exception("Emberr Cloud storage refused the transfer (HTTP $httpStatus)")

class CloudBlobTransfer(private val httpClient: HttpClient) {

    suspend fun upload(uploadUrl: String, bytes: ByteArray) {
        send(HttpMethod.Put, uploadUrl) {
            setBody(ByteArrayContent(bytes, ContentType.Application.OctetStream))
        }
    }

    suspend fun download(downloadUrl: String): ByteArray =
        send(HttpMethod.Get, downloadUrl).readRawBytes()

    private suspend fun send(
        method: HttpMethod,
        signedUrl: String,
        configureRequest: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        val response = try {
            httpClient.request {
                this.method = method
                url.takeFrom(signedUrl)
                configureRequest()
            }
        } catch (_: IOException) {
            throw CloudServerUnreachableException()
        }
        if (!response.status.isSuccess()) throw CloudBlobTransferException(response.status.value)
        return response
    }
}
