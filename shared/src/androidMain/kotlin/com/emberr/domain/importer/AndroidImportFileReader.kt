package com.emberr.domain.importer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

private const val IMPORT_COPIES_FOLDER_NAME = "notes_import"
private const val FALLBACK_DISPLAY_NAME = "file"

fun deleteImportCopiesLeftFromEarlierRuns(context: Context) {
    File(context.cacheDir, IMPORT_COPIES_FOLDER_NAME).deleteRecursively()
}

internal fun displayNameOf(context: Context, uri: Uri): String {
    val nameFromProvider = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
    return nameFromProvider?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: FALLBACK_DISPLAY_NAME
}

class AndroidImportFileReader(private val context: Context) : ImportFileReader {

    override suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles {
        val copiesFolder = File(File(context.cacheDir, IMPORT_COPIES_FOLDER_NAME), UUID.randomUUID().toString())
        val collector = ImportFilesCollector()
        val pickedNames = mutableListOf<String>()

        try {
            withContext(Dispatchers.IO) {
                pickedPaths.forEachIndexed { index, pickedPath ->
                    ensureActive()
                    val uri = Uri.parse(pickedPath)
                    val displayName = displayNameOf(context, uri)
                    pickedNames += displayName

                    if (!ImportPaths.isSupportedPickedFile(displayName)) {
                        collector.skip(displayName)
                        return@forEachIndexed
                    }

                    val copy = copyIntoFolder(uri, copiesFolder, copyName = index.toString())
                    if (copy == null) collector.skip(displayName) else collector.addPickedFile(displayName, copy)
                }
            }
        } catch (error: Throwable) {
            collector.finish(pickedNames) { copiesFolder.deleteRecursively() }.close()
            throw error
        }
        return collector.finish(pickedNames) { copiesFolder.deleteRecursively() }
    }

    private fun lastModifiedOf(uri: Uri): Long? =
        try {
            context.contentResolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getLong(0).takeIf { it > 0 } else null
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun copyIntoFolder(uri: Uri, folder: File, copyName: String): File? {
        return try {
            folder.mkdirs()
            val copy = File(folder, copyName)
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { stream -> copy.outputStream().use { output -> stream.copyTo(output) } }
            lastModifiedOf(uri)?.let(copy::setLastModified)
            copy
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
