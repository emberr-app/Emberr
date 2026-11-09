package com.emberr.domain.cloud.sync

import com.emberr.domain.cloud.api.CloudSubscription
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class CloudCopyRemovalCheck(
    private val latestSubscription: () -> CloudSubscription?,
    private val forgetCloudSyncRecords: suspend () -> Unit
) {

    private val forgetLock = Mutex()
    private var removalAlreadyForgotten: Long? = null

    fun isCloudCopyRemoved(): Boolean = latestSubscription()?.cloudCopyRemovedAtMicroseconds != null

    suspend fun forgetRecordsIfCloudCopyRemoved(): CloudSyncResult.CloudCopyRemoved? {
        val removedAtMicroseconds = latestSubscription()?.cloudCopyRemovedAtMicroseconds ?: return null
        forgetLock.withLock {
            if (removalAlreadyForgotten != removedAtMicroseconds) {
                forgetCloudSyncRecords()
                removalAlreadyForgotten = removedAtMicroseconds
            }
        }
        return CloudSyncResult.CloudCopyRemoved(removedAtMicroseconds)
    }
}
