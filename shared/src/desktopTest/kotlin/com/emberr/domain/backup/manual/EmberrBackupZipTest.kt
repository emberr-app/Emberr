package com.emberr.domain.backup.manual

import androidx.room.RoomDatabase
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.data.local.room.getDatabaseBuilder
import com.emberr.data.local.room.getRoomDatabase
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EmberrBackupZipTest {

    private val workFolder = Files.createTempDirectory("emberr-backup-zip-test").toFile()
    private val sourceMediaFolder = File(workFolder, "source-media").apply { mkdirs() }
    private val deviceMediaFolder = File(workFolder, "device-media")
    private val zipFile = File(workFolder, "backup.emberr")
    private val temporaryDatabaseFile = File(workFolder, "reading.db")
    private val blockJson = Json { encodeDefaults = true }

    @AfterTest
    fun deleteWorkFolder() {
        workFolder.deleteRecursively()
    }

    private fun openDatabase(path: String): AppDatabase =
        getRoomDatabase(getDatabaseBuilder(path).setJournalMode(RoomDatabase.JournalMode.TRUNCATE))

    private fun note(noteId: String, spaceId: String) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 1L,
        updatedAt = 1L,
        filePath = "",
        spaceId = spaceId
    )

    private fun stored(noteId: String, block: NoteBlock) = NoteBlockEntity(
        blockId = block.id,
        noteId = noteId,
        displayOrder = 0,
        blockDataJson = blockJson.encodeToString(NoteBlockSerializer, block),
        updatedAt = 1L,
        isDeleted = false
    )

    private suspend fun exportBackupOfSpaceA() {
        val databaseFile = File(workFolder, "export.db")
        val database = openDatabase(databaseFile.absolutePath)
        val mediaFileNames = try {
            val dao = database.backupDao()
            dao.insertSpaces(
                listOf(
                    SpaceEntity(spaceId = "space-a", displayName = "A", createdAt = 1L),
                    SpaceEntity(spaceId = "space-b", displayName = "B", createdAt = 1L)
                )
            )
            dao.insertNotes(listOf(note("note-a", "space-a"), note("note-b", "space-b")))
            dao.insertBlocks(
                listOf(
                    stored("note-a", ImageBlock(id = "image-a", localFilePath = "/media/a.jpg")),
                    stored("note-a", ImageBlock(id = "image-lost", localFilePath = "/media/lost.jpg")),
                    stored("note-b", ImageBlock(id = "image-b", localFilePath = "/media/b.jpg"))
                )
            )
            database.keepOnlySpacesForExport(setOf("space-a"))
        } finally {
            database.close()
        }
        File(sourceMediaFolder, "a.jpg").writeText("picture of a")
        File(sourceMediaFolder, "b.jpg").writeText("picture of b")
        zipFile.outputStream().use { output -> writeBackupZip(output, databaseFile, sourceMediaFolder, mediaFileNames) }
    }

    private suspend fun openBackup() =
        openEmberrBackupZip(zipFile.name, zipFile, temporaryDatabaseFile, deviceMediaFolder, openDatabaseCopy = ::openDatabase)

    @Test
    fun aBackupOfOneSpaceIsReadBackWithOnlyThatSpace() = runTest {
        exportBackupOfSpaceA()

        openBackup().use { backupFile ->
            assertEquals(listOf("A"), backupFile.backupData.spaces.map { it.displayName })
            assertEquals(listOf("note-a"), backupFile.backupData.notes.map { it.noteId })
            assertEquals(setOf("image-a", "image-lost"), backupFile.backupData.blocks.map { it.blockId }.toSet())
        }
        assertFalse(temporaryDatabaseFile.exists())
    }

    @Test
    fun mediaIsCopiedOnlyWhenItIsMissingOnTheDevice() = runTest {
        exportBackupOfSpaceA()

        openBackup().use { backupFile ->
            assertEquals(BackupMediaCopyOutcome.COPIED, backupFile.copyMediaFileIfMissing("a.jpg"))
            assertEquals("picture of a", File(deviceMediaFolder, "a.jpg").readText())
            assertEquals(BackupMediaCopyOutcome.ALREADY_ON_DEVICE, backupFile.copyMediaFileIfMissing("a.jpg"))
            assertEquals(BackupMediaCopyOutcome.NOT_IN_BACKUP, backupFile.copyMediaFileIfMissing("lost.jpg"))
            assertEquals(BackupMediaCopyOutcome.NOT_IN_BACKUP, backupFile.copyMediaFileIfMissing("b.jpg"))
        }
        assertEquals(listOf("a.jpg"), deviceMediaFolder.list()?.toList())
    }

    @Test
    fun aFileThatIsNotAZipIsRejected() = runTest {
        zipFile.writeText("just some notes")

        val error = assertFailsWith<IllegalStateException> { openBackup() }

        assertEquals("This file is not an Emberr backup.", error.message)
    }

    @Test
    fun aZipWithoutTheDatabaseIsRejected() = runTest {
        ZipOutputStream(zipFile.outputStream()).use { zipOut ->
            zipOut.putNextEntry(ZipEntry("notes/hello.md"))
            zipOut.write("hello".toByteArray())
            zipOut.closeEntry()
        }

        val error = assertFailsWith<IllegalStateException> { openBackup() }

        assertEquals("This file is not an Emberr backup.", error.message)
    }
}
