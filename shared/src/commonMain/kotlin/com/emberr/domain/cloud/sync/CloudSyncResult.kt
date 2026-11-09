package com.emberr.domain.cloud.sync

enum class CloudUploadsRefusedReason {
    STORAGE_FULL,
    TRIAL_ENDED
}

sealed interface CloudSyncResult {
    data class Done(val receivedCount: Int, val sentCount: Int, val notSentCount: Int) : CloudSyncResult
    data class UploadsRefused(val reason: CloudUploadsRefusedReason) : CloudSyncResult
    data class CloudCopyRemoved(val removedAtMicroseconds: Long) : CloudSyncResult
    data object NotSignedIn : CloudSyncResult
    data object Busy : CloudSyncResult
    data object UpdateRequired : CloudSyncResult
    data object AnotherSyncMethodIsOn : CloudSyncResult
    data object WaitingForAllowedNetwork : CloudSyncResult
    data class Failed(val message: String) : CloudSyncResult
}
