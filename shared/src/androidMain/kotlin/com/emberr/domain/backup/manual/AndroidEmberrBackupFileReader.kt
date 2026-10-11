package com.emberr.domain.backup.manual

import android.content.Context
import android.net.Uri
import androidx.room.RoomDatabase
import com.emberr.data.local.room.getDatabaseBuilder
import com.emberr.data.local.room.getRoomDatabase
import com.emberr.domain.importer.displayNameOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val BACKUP_IMPORT_FOLDER_NAME = "emberr_backup_import"

fun deleteBackupImportCopiesLeftFromEarlierRuns(context: Context) {
    File(context.cacheDir, BACKUP_IMPORT_FOLDER_NAME).deleteRecursively()
}

class AndroidEmberrBackupFileReader(private val context: Context) : EmberrBackupFileReader {

    override suspend fun readBackupFile(location: String): EmberrBackupFile {
        val workFolder = File(File(context.cacheDir, BACKUP_IMPORT_FOLDER_NAME), UUID.randomUUID().toString())
        val releaseWorkFolder = { workFolder.deleteRecursively(); Unit }
        val zipCopy = File(workFolder, "backup.zip")
        val uri = Uri.parse(location)
        val pickedName = withContext(Dispatchers.IO) { displayNameOf(context, uri) }

        try {
            withContext(Dispatchers.IO) {
                workFolder.mkdirs()
                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IOException("Couldn't open the chosen file.")
                input.use { stream -> zipCopy.outputStream().use { output -> stream.copyTo(output) } }
            }
        } catch (error: Throwable) {
            releaseWorkFolder()
            throw error
        }

        return openEmberrBackupZip(
            pickedName = pickedName,
            zipFileOnDisk = zipCopy,
            temporaryDatabaseFile = File(workFolder, "backup.db"),
            mediaDirectory = File(context.filesDir, "media"),
            openDatabaseCopy = { databasePath ->
                getRoomDatabase(getDatabaseBuilder(context, databasePath).setJournalMode(RoomDatabase.JournalMode.TRUNCATE))
            },
            releaseResources = releaseWorkFolder
        )
    }
}
