package com.emberr.domain.cloud.sync

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudSyncPollerTest {

    private fun mediaPass(
        filesNotSentCount: Int = 0,
        filesNotReceivedCount: Int = 0,
        filesWaitingForUploadCount: Int = 0,
        stoppedBeforeTheEnd: Boolean = false
    ) = CloudMediaPassResult(
        finishedAtMillis = 1,
        filesTooLargeCount = 0,
        filesNotSentCount = filesNotSentCount,
        filesNotReceivedCount = filesNotReceivedCount,
        filesWaitingForUploadCount = filesWaitingForUploadCount,
        stoppedBeforeTheEnd = stoppedBeforeTheEnd,
        lastProblem = null
    )

    @Test
    fun aMediaPassThatMissedFilesIsRetried() {
        assertTrue(lastMediaPassLeftFilesBehind(mediaPass(filesNotSentCount = 1)))
        assertTrue(lastMediaPassLeftFilesBehind(mediaPass(filesNotReceivedCount = 1)))
        assertTrue(lastMediaPassLeftFilesBehind(mediaPass(filesWaitingForUploadCount = 1)))
        assertTrue(lastMediaPassLeftFilesBehind(mediaPass(stoppedBeforeTheEnd = true)))
    }

    @Test
    fun aCleanMediaPassOrNoPassIsNotRetried() {
        assertFalse(lastMediaPassLeftFilesBehind(mediaPass()))
        assertFalse(lastMediaPassLeftFilesBehind(null))
    }
}
