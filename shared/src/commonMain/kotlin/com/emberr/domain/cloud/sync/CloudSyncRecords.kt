package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CloudSpaceDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.domain.util.sync.SyncCoordinator
import kotlinx.coroutines.sync.withLock

suspend fun forgetCloudSyncRecords(cloudSpaceDao: CloudSpaceDao, cloudItemDao: CloudItemDao, noteDao: NoteDao) {
    SyncCoordinator.mutex.withLock {
        cloudSpaceDao.deleteAllSpaces()
        cloudItemDao.deleteAllItems()
        noteDao.forgetCloudSyncProgressForAllNotes()
    }
}
