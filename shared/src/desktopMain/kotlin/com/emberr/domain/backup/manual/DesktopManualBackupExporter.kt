package com.emberr.domain.backup.manual

import androidx.room.RoomDatabase
import androidx.room.execSQL
import androidx.room.useWriterConnection
import com.emberr.core.desktop.DesktopAppStorage
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.getDatabaseBuilder
import com.emberr.data.local.room.getRoomDatabase
import java.io.File
import java.io.FileOutputStream

class DesktopManualBackupExporter(
    private val appDatabase: AppDatabase
) {

    suspend fun exportToZip(destinationFile: File, spaceIds: Set<String>) {
        val plainDbFile = File(System.getProperty("java.io.tmpdir"), "emberr_manual_export_temp_${System.nanoTime()}.db")
        val plainDbJournalFile = File(plainDbFile.path + "-journal")
        if (plainDbFile.exists()) plainDbFile.delete()

        try {
            appDatabase.useWriterConnection { connection ->
                connection.execSQL("VACUUM INTO '${plainDbFile.absolutePath}'")
            }
            check(plainDbFile.length() > 0L) { "VACUUM INTO produced an empty file" }
            val mediaFileNames = keepOnlySpacesInCopy(plainDbFile, spaceIds)
            FileOutputStream(destinationFile).use { outputStream ->
                writeBackupZip(outputStream, plainDbFile, DesktopAppStorage.mediaDirectory, mediaFileNames)
            }
        } finally {
            plainDbFile.delete()
            plainDbJournalFile.delete()
        }
    }

    private suspend fun keepOnlySpacesInCopy(plainDbFile: File, spaceIds: Set<String>): Set<String> {
        val copyDatabase = getRoomDatabase(
            getDatabaseBuilder(plainDbFile.absolutePath).setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        )
        return try {
            copyDatabase.keepOnlySpacesForExport(spaceIds)
        } finally {
            copyDatabase.close()
        }
    }
}
