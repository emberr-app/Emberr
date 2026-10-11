package com.emberr.domain.util.media

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val LARGEST_WEB_IMAGE_BYTES = 25L * 1024 * 1024

fun webImageAddressOrNull(text: String): String? {
    val address = text.trim()
    val startsLikeAWebAddress = address.startsWith("https://", ignoreCase = true) || address.startsWith("http://", ignoreCase = true)
    return address.takeIf { startsLikeAWebAddress && address.none(Char::isWhitespace) }
}

interface WebImageFetcher {
    suspend fun fetchIntoMedia(url: String): MediaInfo?
}

class KtorWebImageFetcher(private val mediaStorageHelper: MediaStorageHelper) : WebImageFetcher {

    private val httpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }
    }

    override suspend fun fetchIntoMedia(url: String): MediaInfo? =
        try {
            saveImageFrom(httpClient.get(url))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }

    private suspend fun saveImageFrom(response: HttpResponse): MediaInfo? {
        if (!response.status.isSuccess()) return null
        val mimeType = response.contentType()?.let { "${it.contentType}/${it.contentSubtype}" }?.lowercase() ?: return null
        val extension = imageExtensionForMimeType(mimeType) ?: return null
        if ((response.contentLength() ?: 0L) > LARGEST_WEB_IMAGE_BYTES) return null

        val bytes = response.body<ByteArray>()
        if (bytes.isEmpty() || bytes.size > LARGEST_WEB_IMAGE_BYTES) return null
        return withContext(NonCancellable) { mediaStorageHelper.saveBytesToInternalStorage(bytes, extension, mimeType) }
    }
}
