package com.emberr.domain.ai.external

sealed class ExternalAiException(message: String) : Exception(message) {

    class NotConfigured(val provider: String) : ExternalAiException("$provider: no API key")

    class InvalidApiKey(val provider: String) : ExternalAiException("$provider: API key rejected")

    class RateLimited(
        val provider: String,
        val retryAfterSeconds: Int?
    ) : ExternalAiException("$provider: rate limited, retry after $retryAfterSeconds s")

    class RequestTooLarge(val provider: String) : ExternalAiException("$provider: request too large")

    class ProviderUnavailable(
        val provider: String,
        val statusCode: Int
    ) : ExternalAiException("$provider: unavailable, HTTP $statusCode")

    class ProviderError(
        val provider: String,
        val statusCode: Int,
        val detail: String
    ) : ExternalAiException("$provider: HTTP $statusCode: $detail")

    class TimedOut(val provider: String) : ExternalAiException("$provider: timed out")

    class NoConnection(val provider: String) : ExternalAiException("$provider: no connection")
}
