package com.emberr.domain.backup.manual

import com.emberr.data.local.room.APP_DATABASE_VERSION
import java.io.File
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BackupRestoreFilesTest {

    private val workingDirectory = Files.createTempDirectory("emberr-restore-files").toFile()

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    private fun fakeDatabaseFile(databaseVersion: Int): File {
        val header = ByteBuffer.allocate(100)
        header.put("SQLite format 3\u0000".toByteArray(Charsets.US_ASCII))
        header.putInt(60, databaseVersion)
        return File(workingDirectory, "backup.db").apply { writeBytes(header.array()) }
    }

    @Test
    fun aBackupFromThisVersionCanBeRestored() {
        checkBackupDatabaseCanBeRestored(fakeDatabaseFile(APP_DATABASE_VERSION))
    }

    @Test
    fun aBackupFromAnOlderVersionCanBeRestored() {
        checkBackupDatabaseCanBeRestored(fakeDatabaseFile(1))
    }

    @Test
    fun aBackupFromANewerVersionIsRefused() {
        assertFailsWith<IllegalStateException> {
            checkBackupDatabaseCanBeRestored(fakeDatabaseFile(APP_DATABASE_VERSION + 1))
        }
    }

    @Test
    fun aFileThatIsNotABackupIsRefused() {
        val notADatabase = File(workingDirectory, "notes.txt").apply { writeText("x".repeat(200)) }

        assertFailsWith<IllegalStateException> { checkBackupDatabaseCanBeRestored(notADatabase) }
    }
}
