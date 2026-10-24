package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CloudSpaceDao
import com.emberr.data.local.room.dao.NoteDao
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val FORGET_LOCK_MAX_WAIT = 10.seconds

internal object CloudSyncPassLock {
    val mutex = Mutex()
}

suspend fun forgetCloudSyncRecords(cloudSpaceDao: CloudSpaceDao, cloudItemDao: CloudItemDao, noteDao: NoteDao) {
    while (true) {
        val gotPassLock = withTimeoutOrNull(FORGET_LOCK_MAX_WAIT) { CloudSyncPassLock.mutex.lock() } != null
        if (!gotPassLock) continue
        try {
            withLocalDataLock {
                cloudSpaceDao.deleteAllSpaces()
                cloudItemDao.deleteAllItems()
                noteDao.forgetCloudSyncProgressForAllNotes()
            }
            return
        } catch (_: CloudLocalDataBusyException) {
        } finally {
            CloudSyncPassLock.mutex.unlock()
        }
    }
}
