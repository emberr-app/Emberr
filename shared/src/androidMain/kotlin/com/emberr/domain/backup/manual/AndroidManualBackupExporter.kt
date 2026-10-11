package com.emberr.domain.backup.manual

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.getDatabaseBuilder
import com.emberr.data.local.room.getRoomDatabase
import com.emberr.domain.backup.exportPlainSqliteCopy
import java.io.File
import java.io.IOException

class AndroidManualBackupExporter(
    private val context: Context,
    private val appDatabase: AppDatabase
) {

    suspend fun exportToZip(uri: Uri, spaceIds: Set<String>) {
        exportSpacesToZip(uri, spaceIds, tempDatabaseFileName = "emberr_manual_export_temp.db")
    }

    suspend fun exportAllSpacesToZip(uri: Uri) {
        val allSpaceIds = appDatabase.spaceDao().getAllSpacesOnce().map { it.spaceId }.toSet()
        exportSpacesToZip(uri, allSpaceIds, tempDatabaseFileName = "emberr_export_temp.db")
    }

    private suspend fun exportSpacesToZip(uri: Uri, spaceIds: Set<String>, tempDatabaseFileName: String) {
        val plainDbFile = File(context.cacheDir, tempDatabaseFileName)
        val plainDbJournalFile = File(context.cacheDir, "$tempDatabaseFileName-journal")
        plainDbFile.delete()
        plainDbJournalFile.delete()

        try {
            exportPlainSqliteCopy(appDatabase, plainDbFile)
            check(plainDbFile.length() > 0L) { "sqlcipher_export produced an empty file" }
            val mediaFileNames = keepOnlySpacesInCopy(plainDbFile, spaceIds)
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IOException("Couldn't open the chosen file for saving.")
            outputStream.use { writeBackupZip(it, plainDbFile, File(context.filesDir, "media"), mediaFileNames) }
        } finally {
            plainDbFile.delete()
            plainDbJournalFile.delete()
        }
    }

    private suspend fun keepOnlySpacesInCopy(plainDbFile: File, spaceIds: Set<String>): Set<String> {
        val copyDatabase = getRoomDatabase(
            getDatabaseBuilder(context, plainDbFile.absolutePath).setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
        )
        return try {
            copyDatabase.keepOnlySpacesForExport(spaceIds)
        } finally {
            copyDatabase.close()
        }
    }
}
