package com.emberr.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap

enum class MediaTransferPhase { DOWNLOADING, UPLOADING }

data class MediaTransferBytes(val done: Long, val total: Long)

// Tracks real-time media transfer states (downloading/uploading/failed) keyed by file name
// so UI components can reflect in-flight status directly.
object MediaTransferStatusBus {
    // Requires multiple consecutive failures before flagging a file as failed in the UI
    // to prevent transient transfer delays or race conditions from flashing false errors.
    private const val CONSECUTIVE_FAILURES_BEFORE_SHOWING_FAILED = 2
    private val consecutiveFailureCounts = ConcurrentHashMap<String, Int>()

    private val _inProgress = MutableStateFlow<Map<String, MediaTransferPhase>>(emptyMap())
    val inProgress: StateFlow<Map<String, MediaTransferPhase>> = _inProgress.asStateFlow()

    private val _failed = MutableStateFlow<Set<String>>(emptySet())
    val failed: StateFlow<Set<String>> = _failed.asStateFlow()

    private val _bytes = MutableStateFlow<Map<String, MediaTransferBytes>>(emptyMap())
    val bytes: StateFlow<Map<String, MediaTransferBytes>> = _bytes.asStateFlow()

    private val _waitingForUpload = MutableStateFlow<Set<String>>(emptySet())
    val waitingForUpload: StateFlow<Set<String>> = _waitingForUpload.asStateFlow()

    fun markStarted(fileName: String, phase: MediaTransferPhase) {
        _waitingForUpload.update { it - fileName }
        _inProgress.update { it + (fileName to phase) }
    }

    fun reportBytes(fileName: String, done: Long, total: Long) {
        _bytes.update { it + (fileName to MediaTransferBytes(done, total)) }
    }

    fun markWaitingForUpload(fileName: String) {
        markDeferred(fileName)
        _waitingForUpload.update { it + fileName }
    }

    fun markFinished(fileName: String, succeeded: Boolean) {
        _inProgress.update { it - fileName }
        _bytes.update { it - fileName }
        if (succeeded) {
            consecutiveFailureCounts.remove(fileName)
            _failed.update { it - fileName }
            return
        }
        val failureCount = consecutiveFailureCounts.merge(fileName, 1) { previous, one -> previous + one } ?: 1
        if (failureCount >= CONSECUTIVE_FAILURES_BEFORE_SHOWING_FAILED) {
            _failed.update { it + fileName }
        }
    }

    // Handles deferred transfers (e.g. peer still uploading via HTTP 425),
    // resetting the failure streak so the UI maintains active transfer status.
    fun markDeferred(fileName: String) {
        _inProgress.update { it - fileName }
        _bytes.update { it - fileName }
        consecutiveFailureCounts.remove(fileName)
    }
}