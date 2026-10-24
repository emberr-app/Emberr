package com.emberr.domain.cloud.sync

data class CloudSyncStatus(
    val isSyncing: Boolean = false,
    val lastSyncedAtMillis: Long? = null,
    val lastProblem: CloudSyncResult? = null
)
