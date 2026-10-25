package com.emberr.domain.selfhost.sync

import kotlinx.coroutines.flow.StateFlow

expect class SelfHostSyncScheduler {
    val isSyncActive: StateFlow<Boolean>
    val syncError: StateFlow<String?>
    fun scheduleBackgroundSync()
    fun scheduleDeferredSyncAfterAppClose()
    fun syncNow()
    fun cancelSync()
    fun cancelAll()
}
