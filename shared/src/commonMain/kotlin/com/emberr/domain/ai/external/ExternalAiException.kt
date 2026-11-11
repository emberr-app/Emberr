package com.emberr.domain.ai.external

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_ai_api_key_rejected
import emberr.shared.generated.resources.message_ai_no_api_key
import emberr.shared.generated.resources.message_ai_no_connection
import emberr.shared.generated.resources.message_ai_provider_error
import emberr.shared.generated.resources.message_ai_provider_unavailable
import emberr.shared.generated.resources.message_ai_rate_limited
import emberr.shared.generated.resources.message_ai_rate_limited_for_seconds
import emberr.shared.generated.resources.message_ai_request_too_large
import emberr.shared.generated.resources.message_ai_timed_out
import org.jetbrains.compose.resources.getString

sealed class ExternalAiException(message: String) : Exception(message) {

    abstract suspend fun friendlyMessage(): String

    class NotConfigured(private val provider: String) : ExternalAiException("$provider: no API key") {
        override suspend fun friendlyMessage(): String = getString(Res.string.message_ai_no_api_key, provider)
    }

    class InvalidApiKey(private val provider: String) : ExternalAiException("$provider: API key rejected") {
        override suspend fun friendlyMessage(): String = getString(Res.string.message_ai_api_key_rejected, provider)
    }

    class RateLimited(
        private val provider: String,
        private val retryAfterSeconds: Int?
    ) : ExternalAiException("$provider: rate limited, retry after $retryAfterSeconds s") {
        override suspend fun friendlyMessage(): String =
            if (retryAfterSeconds != null) {
                getString(Res.string.message_ai_rate_limited_for_seconds, provider, retryAfterSeconds)
            } else {
                getString(Res.string.message_ai_rate_limited, provider)
            }
    }

    class RequestTooLarge(private val provider: String) : ExternalAiException("$provider: request too large") {
        override suspend fun friendlyMessage(): String = getString(Res.string.message_ai_request_too_large, provider)
    }

    class ProviderUnavailable(
        private val provider: String,
        private val statusCode: Int
    ) : ExternalAiException("$provider: unavailable, HTTP $statusCode") {
        override suspend fun friendlyMessage(): String =
            getString(Res.string.message_ai_provider_unavailable, provider, statusCode)
    }

    class ProviderError(
        private val provider: String,
        private val statusCode: Int,
        private val detail: String
    ) : ExternalAiException("$provider: HTTP $statusCode: $detail") {
        override suspend fun friendlyMessage(): String =
            getString(Res.string.message_ai_provider_error, provider, statusCode, detail)
    }

    class TimedOut(private val provider: String) : ExternalAiException("$provider: timed out") {
        override suspend fun friendlyMessage(): String = getString(Res.string.message_ai_timed_out, provider)
    }

    class NoConnection(private val provider: String) : ExternalAiException("$provider: no connection") {
        override suspend fun friendlyMessage(): String = getString(Res.string.message_ai_no_connection, provider)
    }
}
