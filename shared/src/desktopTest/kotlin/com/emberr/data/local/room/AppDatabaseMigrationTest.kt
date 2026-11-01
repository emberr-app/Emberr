package com.emberr.data.local.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppDatabaseMigrationTest {

    private val schemaDirectory = Path.of("schemas")
    private val workingDirectory = Files.createTempDirectory("emberr-database-migration")

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.toFile().deleteRecursively()
    }

    @Test
    fun everyExportedSchemaUpgradesToTheNewestOne() {
        val exportedVersions = exportedSchemaVersions()
        assertTrue(exportedVersions.isNotEmpty(), "No exported schemas in ${schemaDirectory.toAbsolutePath()}")
        val newestVersion = exportedVersions.max()

        exportedVersions.forEach { oldVersion ->
            val migrationTestHelper = MigrationTestHelper(
                schemaDirectoryPath = schemaDirectory,
                databasePath = workingDirectory.resolve("upgraded-from-$oldVersion.db"),
                driver = BundledSQLiteDriver(),
                databaseClass = AppDatabase::class
            )
            migrationTestHelper.createDatabase(oldVersion).close()
            migrationTestHelper.runMigrationsAndValidate(newestVersion, emptyList()).close()
        }
    }

    @Test
    fun upgradingToVersion7KeepsCalendarTasksAndLeavesThemWithoutPeople() {
        val migrationTestHelper = migrationTestHelperFor("calendar-tasks.db")
        migrationTestHelper.createDatabase(6).apply {
            execSQL(
                "INSERT INTO calendar_tasks (blockId, noteId, text, isChecked, targetDate, sourceType, spaceId) " +
                    "VALUES ('event-1', 'note-1', 'Lunch', 0, '2026-11-02', 'NOTE', 'default')"
            )
            close()
        }

        val upgraded = migrationTestHelper.runMigrationsAndValidate(7, emptyList())

        assertEquals("Lunch", upgraded.textInFirstRow("SELECT text FROM calendar_tasks WHERE blockId = 'event-1'"))
        assertEquals("missing", upgraded.textInFirstRow("SELECT IFNULL(personNoteIds, 'missing') FROM calendar_tasks WHERE blockId = 'event-1'"))
        upgraded.close()
    }

    @Test
    fun upgradingToVersion7TurnsOnlyAccountNotesWithALivePersonDetailIntoPeople() {
        val migrationTestHelper = migrationTestHelperFor("person-notes.db")
        val personDetailJson = """{"type":"person_detail","id":"detail","kind":"PHONE","value":"+1 617 555 0142"}"""
        migrationTestHelper.createDatabase(6).apply {
            insertNote(noteId = "person", spaceId = "account")
            insertBlock(blockId = "person-detail", noteId = "person", blockDataJson = personDetailJson, isDeleted = false)
            insertNote(noteId = "deleted-detail-only", spaceId = "account")
            insertBlock(blockId = "deleted-detail", noteId = "deleted-detail-only", blockDataJson = personDetailJson, isDeleted = true)
            insertNote(noteId = "other-space", spaceId = "default")
            insertBlock(blockId = "other-space-detail", noteId = "other-space", blockDataJson = personDetailJson, isDeleted = false)
            close()
        }

        val upgraded = migrationTestHelper.runMigrationsAndValidate(7, emptyList())

        assertEquals("PERSON", upgraded.textInFirstRow("SELECT kind FROM notes_metadata WHERE noteId = 'person'"))
        assertEquals("NOTE", upgraded.textInFirstRow("SELECT kind FROM notes_metadata WHERE noteId = 'deleted-detail-only'"))
        assertEquals("NOTE", upgraded.textInFirstRow("SELECT kind FROM notes_metadata WHERE noteId = 'other-space'"))
        upgraded.close()
    }

    private fun migrationTestHelperFor(databaseFileName: String) = MigrationTestHelper(
        schemaDirectoryPath = schemaDirectory,
        databasePath = workingDirectory.resolve(databaseFileName),
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class
    )

    private fun SQLiteConnection.insertNote(noteId: String, spaceId: String) {
        execSQL(
            "INSERT INTO notes_metadata (noteId, title, isDaily, createdAt, updatedAt, filePath, snippet, isFavorite, " +
                "isSubNote, showWordCount, sortOrder, isTemplate, selfHostSyncedAt, spaceId, kind, titleUpdatedAt, " +
                "folderUpdatedAt, iconUpdatedAt, favoriteUpdatedAt, coverImageUpdatedAt, wordCountUpdatedAt) " +
                "VALUES ('$noteId', 'Maya', 0, 1, 1, '', '', 0, 0, 0, 0, 0, 0, '$spaceId', 'NOTE', 0, 0, 0, 0, 0, 0)"
        )
    }

    private fun SQLiteConnection.insertBlock(blockId: String, noteId: String, blockDataJson: String, isDeleted: Boolean) {
        execSQL(
            "INSERT INTO note_blocks (blockId, noteId, displayOrder, blockDataJson, updatedAt, isDeleted) " +
                "VALUES ('$blockId', '$noteId', 0, '$blockDataJson', 1, ${if (isDeleted) 1 else 0})"
        )
    }

    private fun SQLiteConnection.textInFirstRow(sql: String): String =
        prepare(sql).use { statement ->
            check(statement.step()) { "No row for: $sql" }
            statement.getText(0)
        }

    private fun exportedSchemaVersions(): List<Int> =
        schemaDirectory
            .resolve(checkNotNull(AppDatabase::class.qualifiedName))
            .listDirectoryEntries("*.json")
            .mapNotNull { schemaFile -> schemaFile.fileName.toString().removeSuffix(".json").toIntOrNull() }
}
