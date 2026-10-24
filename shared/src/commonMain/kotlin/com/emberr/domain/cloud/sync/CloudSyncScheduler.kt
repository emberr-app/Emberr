package com.emberr.domain.cloud.sync

expect class CloudSyncScheduler {
    fun start()
    fun syncNow()
}
