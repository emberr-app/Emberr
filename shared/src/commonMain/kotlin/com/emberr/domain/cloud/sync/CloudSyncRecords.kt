package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CloudMediaLockDao
import com.emberr.data.local.room.dao.CloudSpaceDao
import com.emberr.data.local.room.dao.NoteDao
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val FORGET_LOCK_MAX_WAIT = 10.seconds
private val MEDIA_PASS_STOP_RETRY_WAIT = 50.milliseconds

internal object CloudSyncPassLock {
    val mutex = Mutex()
}

internal object CloudMediaPassLock {
    val mutex = Mutex()

    @Volatile
    var runningPass: Job? = null

    suspend fun stopRunningPassAndLock() {
        while (true) {
            runningPass?.cancelAndJoin()
            if (mutex.tryLock()) return
            delay(MEDIA_PASS_STOP_RETRY_WAIT)
        }
    }
}

suspend fun forgetCloudSyncRecords(
    cloudSpaceDao: CloudSpaceDao,
    cloudItemDao: CloudItemDao,
    cloudMediaLockDao: CloudMediaLockDao,
    noteDao: NoteDao
) {
    while (true) {
        val gotPassLock = withTimeoutOrNull(FORGET_LOCK_MAX_WAIT) { CloudSyncPassLock.mutex.lock() } != null
        if (!gotPassLock) continue
        try {
            CloudMediaPassLock.stopRunningPassAndLock()
            try {
                withLocalDataLock {
                    cloudSpaceDao.deleteAllSpaces()
                    cloudItemDao.deleteAllItems()
                    cloudMediaLockDao.deleteAllLocks()
                    noteDao.forgetCloudSyncProgressForAllNotes()
                }
                return
            } finally {
                CloudMediaPassLock.mutex.unlock()
            }
        } catch (_: CloudLocalDataBusyException) {
        } finally {
            CloudSyncPassLock.mutex.unlock()
        }
    }
}
