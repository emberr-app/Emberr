package com.emberr.domain.backup.manual

import androidx.room.RoomDatabase
import com.emberr.core.desktop.DesktopAppStorage
import com.emberr.data.local.room.getDatabaseBuilder
import com.emberr.data.local.room.getRoomDatabase
import java.io.File

class DesktopEmberrBackupFileReader : EmberrBackupFileReader {

    override suspend fun readBackupFile(location: String): EmberrBackupFile =
        openEmberrBackupZip(
            pickedName = File(location).name,
            zipFileOnDisk = File(location),
            temporaryDatabaseFile = File(System.getProperty("java.io.tmpdir"), "emberr_backup_import_${System.nanoTime()}.db"),
            mediaDirectory = DesktopAppStorage.mediaDirectory,
            openDatabaseCopy = { databasePath ->
                getRoomDatabase(getDatabaseBuilder(databasePath).setJournalMode(RoomDatabase.JournalMode.TRUNCATE))
            }
        )
}
