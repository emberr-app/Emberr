package com.emberr.domain.cloud.sync

sealed interface CloudSyncResult {
    data class Done(val receivedCount: Int, val sentCount: Int, val notSentCount: Int) : CloudSyncResult
    data object NotSignedIn : CloudSyncResult
    data object Busy : CloudSyncResult
    data object UpdateRequired : CloudSyncResult
    data class Failed(val message: String) : CloudSyncResult
}
