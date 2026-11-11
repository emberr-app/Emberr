package com.emberr.presentation.ai

import androidx.compose.runtime.Composable
import com.emberr.domain.ai.AiReplyPiece
import com.emberr.domain.ai.LocalAiUnsupportedReason
import com.emberr.domain.ai.external.ExternalAiException
import com.emberr.domain.ai.models.ModelDownloadFailure
import com.emberr.domain.ai.models.ModelDownloadProblem
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_ai_api_key_rejected
import emberr.shared.generated.resources.message_ai_engine_not_bundled
import emberr.shared.generated.resources.message_ai_needs_64_bit_processor
import emberr.shared.generated.resources.message_ai_no_api_key
import emberr.shared.generated.resources.message_ai_no_connection
import emberr.shared.generated.resources.message_ai_no_indexed_notes
import emberr.shared.generated.resources.message_ai_no_processor_build
import emberr.shared.generated.resources.message_ai_nothing_relevant
import emberr.shared.generated.resources.message_ai_provider_error
import emberr.shared.generated.resources.message_ai_provider_unavailable
import emberr.shared.generated.resources.message_ai_rate_limited
import emberr.shared.generated.resources.message_ai_rate_limited_for_seconds
import emberr.shared.generated.resources.message_ai_request_too_large
import emberr.shared.generated.resources.message_ai_timed_out
import emberr.shared.generated.resources.message_model_download_cancelled
import emberr.shared.generated.resources.message_model_download_did_not_complete
import emberr.shared.generated.resources.message_model_download_failed
import emberr.shared.generated.resources.message_model_download_missing_name
import emberr.shared.generated.resources.message_model_download_paused
import emberr.shared.generated.resources.message_model_download_stopped_before_finish
import emberr.shared.generated.resources.message_model_download_stopped_unexpectedly
import emberr.shared.generated.resources.message_model_download_timed_out
import emberr.shared.generated.resources.message_model_download_unknown_model
import emberr.shared.generated.resources.message_model_file_not_usable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

private val LocalAiUnsupportedReason.phrase: StringResource
    get() = when (this) {
        is LocalAiUnsupportedReason.NoBuildForProcessor -> Res.string.message_ai_no_processor_build
        is LocalAiUnsupportedReason.EngineNotBundledForSystem -> Res.string.message_ai_engine_not_bundled
        is LocalAiUnsupportedReason.Needs64BitProcessor -> Res.string.message_ai_needs_64_bit_processor
    }

private val LocalAiUnsupportedReason.phraseValue: String
    get() = when (this) {
        is LocalAiUnsupportedReason.NoBuildForProcessor -> processorName
        is LocalAiUnsupportedReason.EngineNotBundledForSystem -> operatingSystemName
        is LocalAiUnsupportedReason.Needs64BitProcessor -> processorName
    }

@Composable
fun LocalAiUnsupportedReason.text(): String = stringResource(phrase, phraseValue)

suspend fun LocalAiUnsupportedReason.message(): String = getString(phrase, phraseValue)

private val ModelDownloadFailure.phrase: StringResource
    get() = when (this) {
        ModelDownloadFailure.TIMED_OUT -> Res.string.message_model_download_timed_out
        ModelDownloadFailure.DOWNLOAD_FAILED -> Res.string.message_model_download_failed
        ModelDownloadFailure.CANCELLED -> Res.string.message_model_download_cancelled
        ModelDownloadFailure.MISSING_FILE_NAME -> Res.string.message_model_download_missing_name
        ModelDownloadFailure.UNKNOWN_MODEL -> Res.string.message_model_download_unknown_model
        ModelDownloadFailure.DID_NOT_COMPLETE -> Res.string.message_model_download_did_not_complete
        ModelDownloadFailure.STOPPED_UNEXPECTEDLY -> Res.string.message_model_download_stopped_unexpectedly
        ModelDownloadFailure.STOPPED_BEFORE_FINISH -> Res.string.message_model_download_stopped_before_finish
        ModelDownloadFailure.PAUSED -> Res.string.message_model_download_paused
        ModelDownloadFailure.FILE_NOT_USABLE -> Res.string.message_model_file_not_usable
    }

@Composable
fun ModelDownloadProblem.text(): String = when (this) {
    is ModelDownloadFailure -> stringResource(phrase)
    is ModelDownloadProblem.LocalAiCannotRun -> reason.text()
}

suspend fun ExternalAiException.friendlyMessage(): String = when (this) {
    is ExternalAiException.NotConfigured -> getString(Res.string.message_ai_no_api_key, provider)
    is ExternalAiException.InvalidApiKey -> getString(Res.string.message_ai_api_key_rejected, provider)
    is ExternalAiException.RateLimited -> if (retryAfterSeconds != null) {
        getString(Res.string.message_ai_rate_limited_for_seconds, provider, retryAfterSeconds)
    } else {
        getString(Res.string.message_ai_rate_limited, provider)
    }
    is ExternalAiException.RequestTooLarge -> getString(Res.string.message_ai_request_too_large, provider)
    is ExternalAiException.ProviderUnavailable ->
        getString(Res.string.message_ai_provider_unavailable, provider, statusCode)
    is ExternalAiException.ProviderError ->
        getString(Res.string.message_ai_provider_error, provider, statusCode, detail)
    is ExternalAiException.TimedOut -> getString(Res.string.message_ai_timed_out, provider)
    is ExternalAiException.NoConnection -> getString(Res.string.message_ai_no_connection, provider)
}

suspend fun AiReplyPiece.replyText(): String = when (this) {
    is AiReplyPiece.Text -> text
    AiReplyPiece.NoIndexedNotes -> getString(Res.string.message_ai_no_indexed_notes)
    AiReplyPiece.NothingRelevantInNotes -> getString(Res.string.message_ai_nothing_relevant)
}
