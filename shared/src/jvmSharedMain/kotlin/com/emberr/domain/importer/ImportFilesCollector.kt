package com.emberr.domain.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Source
import okio.source
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val OLD_ZIP_NAME_CHARSET = "IBM437"

class ImportFilesCollector {

    private val files = mutableListOf<ImportFile>()
    private val skippedNames = mutableListOf<String>()
    private val openZipFiles = mutableListOf<ZipFile>()

    fun addPickedFile(displayName: String, file: File) {
        when {
            ImportPaths.isUnsupportedObsidianFile(displayName) -> skippedNames += displayName
            ImportPaths.isZipFile(displayName) -> addZipFile(displayName, file)
            ImportPaths.isNoteFile(displayName) -> files += LocalImportFile(displayName, file)
            else -> skippedNames += displayName
        }
    }

    fun skip(displayName: String) {
        skippedNames += displayName
    }

    fun finish(pickedNames: List<String>, cleanUp: () -> Unit = {}): PickedImportFiles {
        val zipFilesToClose = openZipFiles.toList()
        return PickedImportFiles(
            pickedNames = pickedNames,
            files = files.toList(),
            skippedNames = skippedNames.toList()
        ) {
            zipFilesToClose.forEach(::closeQuietly)
            cleanUp()
        }
    }

    private fun addZipFile(displayName: String, file: File) {
        val openedZip = openZipFile(file)
        if (openedZip == null) {
            skippedNames += displayName
            return
        }
        openZipFiles += openedZip.zipFile

        for (entry in openedZip.entries) {
            if (ImportPaths.isIgnoredEntry(entry.name)) continue
            val relativePath = ImportPaths.safeRelativePath(entry.name)
            if (relativePath == null) {
                skippedNames += entry.name
                continue
            }
            if (ImportPaths.isUnsupportedObsidianFile(relativePath)) {
                skippedNames += relativePath
                continue
            }
            files += ZipEntryImportFile(relativePath, openedZip.zipFile, entry)
        }
    }

    private fun openZipFile(file: File): OpenedZipFile? =
        openZipFile(file, Charsets.UTF_8) ?: oldZipNameCharset()?.let { charset -> openZipFile(file, charset) }

    private fun openZipFile(file: File, nameCharset: Charset): OpenedZipFile? {
        val zipFile = try {
            ZipFile(file, nameCharset)
        } catch (_: IOException) {
            return null
        }
        return try {
            OpenedZipFile(zipFile, zipFile.entries().toList())
        } catch (_: IllegalArgumentException) {
            closeQuietly(zipFile)
            null
        }
    }

    private fun oldZipNameCharset(): Charset? =
        try {
            Charset.forName(OLD_ZIP_NAME_CHARSET)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun closeQuietly(zipFile: ZipFile) {
        try {
            zipFile.close()
        } catch (_: IOException) {
        }
    }
}

private class OpenedZipFile(val zipFile: ZipFile, val entries: List<ZipEntry>)

private class LocalImportFile(
    override val relativePath: String,
    private val file: File
) : ImportFile {
    override val sizeBytes: Long get() = file.length()

    override val lastModifiedAt: Long? get() = file.lastModified().takeIf { it > 0 }

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) { file.readBytes() }

    override fun openSource(): Source = file.source()
}

private class ZipEntryImportFile(
    override val relativePath: String,
    private val zipFile: ZipFile,
    private val entry: ZipEntry
) : ImportFile {
    override val sizeBytes: Long get() = entry.size.coerceAtLeast(0)

    override val lastModifiedAt: Long? get() = entry.time.takeIf { it > 0 }

    override suspend fun readBytes(): ByteArray = withContext(Dispatchers.IO) {
        zipFile.getInputStream(entry).use { stream -> stream.readBytes() }
    }

    override fun openSource(): Source = zipFile.getInputStream(entry).source()
}
