package com.emberr.domain.cloud.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

internal fun lastMediaPassLeftFilesBehind(lastMediaPass: CloudMediaPassResult?): Boolean {
    if (lastMediaPass == null) return false
    return lastMediaPass.stoppedBeforeTheEnd ||
        lastMediaPass.filesNotSentCount > 0 ||
        lastMediaPass.filesNotReceivedCount > 0 ||
        lastMediaPass.filesWaitingForUploadCount > 0
}

class CloudSyncPoller(private val cloudSyncEngine: CloudSyncEngine) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var pollingJob: Job? = null

    fun start() {
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL)
                val result = cloudSyncEngine.syncAutomaticallyIfThereIsWork()
                if (result == null && lastMediaPassLeftFilesBehind(cloudSyncEngine.status.value.lastMediaPass)) {
                    cloudSyncEngine.startMediaPassUnlessRunning()
                }
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private companion object {
        val POLL_INTERVAL = 2.minutes
    }
}
