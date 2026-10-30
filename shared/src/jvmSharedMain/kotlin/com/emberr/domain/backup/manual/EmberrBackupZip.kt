package com.emberr.domain.backup.manual

import com.emberr.data.local.room.APP_DATABASE_VERSION
import com.emberr.data.local.room.AppDatabase
import com.emberr.domain.model.CredentialKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

private const val DATABASE_ENTRY_NAME = "emberr_database.db"
private const val MEDIA_ENTRY_PREFIX = "media/"
private const val CREDENTIAL_KEYS_ENTRY_NAME = "credential_keys.json"
private val credentialKeysJson = Json { ignoreUnknownKeys = true }
private const val SQLITE_HEADER_SIZE = 100
private const val SQLITE_USER_VERSION_OFFSET = 60L
private val SQLITE_FILE_START = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
private const val NOT_AN_EMBERR_BACKUP = "This file is not an Emberr backup."
private const val PARTIAL_MEDIA_FILE_SUFFIX = ".tmp"
private const val COPY_BUFFER_SIZE = 64 * 1024

fun checkBackupDatabaseCanBeRestored(databaseFile: File) {
    val backupDatabaseVersion = readDatabaseVersion(databaseFile)
    check(backupDatabaseVersion <= APP_DATABASE_VERSION) {
        "This backup was made with a newer version of Emberr. Update Emberr on this device, then try again."
    }
}

private fun readDatabaseVersion(databaseFile: File): Int =
    RandomAccessFile(databaseFile, "r").use { file ->
        check(file.length() >= SQLITE_HEADER_SIZE) { NOT_AN_EMBERR_BACKUP }
        val fileStart = ByteArray(SQLITE_FILE_START.size)
        file.readFully(fileStart)
        check(fileStart.contentEquals(SQLITE_FILE_START)) { NOT_AN_EMBERR_BACKUP }
        file.seek(SQLITE_USER_VERSION_OFFSET)
        file.readInt()
    }

fun writeBackupZip(
    outputStream: OutputStream,
    databaseFile: File,
    mediaDirectory: File,
    mediaFileNames: Set<String>,
    credentialKeys: List<CredentialKey>
) {
    ZipOutputStream(outputStream).use { zipOut ->
        zipOut.putNextEntry(ZipEntry(DATABASE_ENTRY_NAME))
        databaseFile.inputStream().use { input -> input.copyTo(zipOut) }
        zipOut.closeEntry()

        zipOut.putNextEntry(ZipEntry(CREDENTIAL_KEYS_ENTRY_NAME))
        zipOut.write(
            credentialKeysJson.encodeToString(ListSerializer(CredentialKey.serializer()), credentialKeys).toByteArray()
        )
        zipOut.closeEntry()

        mediaFileNames.sorted().forEach { fileName ->
            val mediaFile = File(mediaDirectory, fileName)
            if (!mediaFile.isFile) return@forEach
            zipOut.putNextEntry(ZipEntry("${MEDIA_ENTRY_PREFIX}${mediaFile.name}"))
            mediaFile.inputStream().use { input -> input.copyTo(zipOut) }
            zipOut.closeEntry()
        }
    }
}

suspend fun openEmberrBackupZip(
    pickedName: String,
    zipFileOnDisk: File,
    temporaryDatabaseFile: File,
    mediaDirectory: File,
    openDatabaseCopy: (databasePath: String) -> AppDatabase,
    releaseResources: () -> Unit = {}
): EmberrBackupFile = withContext(Dispatchers.IO) {
    val zipFile = try {
        ZipFile(zipFileOnDisk)
    } catch (notAZip: ZipException) {
        releaseResources()
        throw IllegalStateException(NOT_AN_EMBERR_BACKUP, notAZip)
    }
    try {
        val databaseEntry = zipFile.getEntry(DATABASE_ENTRY_NAME) ?: error(NOT_AN_EMBERR_BACKUP)
        zipFile.getInputStream(databaseEntry).use { input ->
            temporaryDatabaseFile.outputStream().use { output -> input.copyTo(output) }
        }
        checkBackupDatabaseCanBeRestored(temporaryDatabaseFile)
        val backupData = readAllData(openDatabaseCopy(temporaryDatabaseFile.absolutePath))
        ZipEmberrBackupFile(pickedName, zipFile, backupData, readCredentialKeys(zipFile), mediaDirectory, releaseResources)
    } catch (error: Throwable) {
        zipFile.close()
        releaseResources()
        throw error
    } finally {
        deleteDatabaseFiles(temporaryDatabaseFile)
    }
}

private suspend fun readAllData(database: AppDatabase): EmberrBackupData =
    try {
        BackupRepositoryImpl(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            blockDao = database.blockDao(),
            calendarTaskDao = database.calendarTaskDao(),
            categoryDao = database.categoryDao(),
            propertyTagDao = database.propertyTagDao(),
            customPropertyDao = database.customPropertyDao(),
            imageBlockDao = database.imageBlockDao(),
            documentBlockDao = database.documentBlockDao(),
            bookmarkBlockDao = database.bookmarkBlockDao(),
            spaceDao = database.spaceDao(),
            chatSessionDao = database.chatSessionDao(),
            calendarEventExceptionDao = database.calendarEventExceptionDao(),
            selfHostDeletedNoteDao = database.selfHostDeletedNoteDao(),
            canvasDao = database.canvasDao()
        ).createBackupData()
    } finally {
        database.close()
    }

private fun readCredentialKeys(zipFile: ZipFile): List<CredentialKey> {
    val credentialKeysEntry = zipFile.getEntry(CREDENTIAL_KEYS_ENTRY_NAME) ?: return emptyList()
    val keysJson = zipFile.getInputStream(credentialKeysEntry).use { input -> input.readBytes().decodeToString() }
    return credentialKeysJson.decodeFromString(ListSerializer(CredentialKey.serializer()), keysJson)
}

private fun deleteDatabaseFiles(databaseFile: File) {
    listOf("", "-journal", "-wal", "-shm").forEach { suffix -> File(databaseFile.path + suffix).delete() }
}

private class ZipEmberrBackupFile(
    override val pickedName: String,
    private val zipFile: ZipFile,
    override val backupData: EmberrBackupData,
    override val credentialKeys: List<CredentialKey>,
    private val mediaDirectory: File,
    private val releaseResources: () -> Unit
) : EmberrBackupFile {

    override suspend fun copyMediaFileIfMissing(fileName: String): BackupMediaCopyOutcome = withContext(Dispatchers.IO) {
        val safeFileName = File(fileName).name
        val destination = File(mediaDirectory, safeFileName)
        if (destination.exists()) return@withContext BackupMediaCopyOutcome.ALREADY_ON_DEVICE
        val entry = zipFile.getEntry("${MEDIA_ENTRY_PREFIX}$safeFileName")
            ?: return@withContext BackupMediaCopyOutcome.NOT_IN_BACKUP

        mediaDirectory.mkdirs()
        val partialFile = File(mediaDirectory, "$safeFileName$PARTIAL_MEDIA_FILE_SUFFIX")
        try {
            zipFile.getInputStream(entry).use { input ->
                partialFile.outputStream().use { output ->
                    val buffer = ByteArray(COPY_BUFFER_SIZE)
                    while (true) {
                        ensureActive()
                        val readCount = input.read(buffer)
                        if (readCount < 0) break
                        output.write(buffer, 0, readCount)
                    }
                }
            }
            Files.move(partialFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (error: Throwable) {
            partialFile.delete()
            throw error
        }
        BackupMediaCopyOutcome.COPIED
    }

    override fun close() {
        zipFile.close()
        releaseResources()
    }
}
