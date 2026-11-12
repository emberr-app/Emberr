package com.emberr.data.local.room

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * =====================================================================
 * EMBER DATABASE MIGRATION CHEAT SHEET
 * =====================================================================
 * This project uses Room AutoMigrations to safely update the local SQLite
 * database without losing user data. Whenever you change an @Entity,
 * you MUST follow one of the scenarios below.
 * * CRITICAL RULE: NEVER delete old Specs from this file! Room needs the
 * entire history to upgrade users who are several versions behind.
 * =====================================================================
 *
 * SCENARIO 1: ADDING A NEW COLUMN (Safe)
 * Room considers adding data "safe". No Spec class is needed.
 * 1. Add the column to your Entity class.
 * (Use @ColumnInfo(defaultValue = "...") if it is non-nullable).
 * 2. Open AppDatabase.kt.
 * 3. Bump the `version` up by 1.
 * 4. Add the migration to the array: AutoMigration(from = X, to = Y)
 *
 * ---------------------------------------------------------------------
 * * SCENARIO 2: REMOVING A COLUMN (Dangerous)
 * Room panics if a column disappears to prevent accidental data loss.
 * You must provide an explicit permission slip (Spec).
 * 1. Delete the column from your Entity class.
 * 2. Add a new Spec class to THIS file:

 * @DeleteColumn(tableName = "table_name", columnName = "deleted_column")
 * class DropSomethingSpec : AutoMigrationSpec

 * 3. Open AppDatabase.kt.
 * 4. Bump the `version` up by 1.
 * 5. Add the migration: AutoMigration(from = X, to = Y, spec = DropSomethingSpec::class)
 *
 * ---------------------------------------------------------------------
 * * SCENARIO 3: RENAMING A COLUMN OR TABLE
 * Room thinks a rename is a "Delete + Add". You must tell it they are
 * the same column so it carries the user's data over.
 * 1. Rename the column in your Entity class.
 * 2. Add a new Spec class to THIS file:

 * @RenameColumn(tableName = "table_name", fromColumnName = "old", toColumnName = "new")
 * class RenameSomethingSpec : AutoMigrationSpec

 * 3. Open AppDatabase.kt.
 * 4. Bump the `version` up by 1.
 * 5. Add the migration: AutoMigration(from = X, to = Y, spec = RenameSomethingSpec::class)
 * =====================================================================
 */

class RecognizePersonNotesSpec : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            UPDATE notes_metadata SET kind = 'PERSON'
            WHERE kind = 'NOTE' AND spaceId = 'account' AND EXISTS (
                SELECT 1 FROM note_blocks
                WHERE note_blocks.noteId = notes_metadata.noteId
                AND note_blocks.isDeleted = 0
                AND note_blocks.blockDataJson LIKE '%"type":"person_detail"%'
            )
            """.trimIndent()
        )
    }
}

class SeedBlockMergeBasesSpec : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        val textBlockTypes = listOf("text", "heading", "quote", "checkbox", "bullet", "number", "toggle", "callout", "code")
        val holdsText = textBlockTypes.joinToString(" OR ") { type -> """note_blocks.blockDataJson LIKE '{"type":"$type",%'""" }
        connection.execSQL(
            """
            INSERT OR REPLACE INTO block_merge_bases (noteId, blockId, blockDataJson, updatedAt)
            SELECT note_blocks.noteId, note_blocks.blockId, note_blocks.blockDataJson, note_blocks.updatedAt
            FROM note_blocks
            JOIN notes_metadata ON notes_metadata.noteId = note_blocks.noteId
            WHERE note_blocks.isDeleted = 0
            AND (
                (notes_metadata.selfHostSyncedAt > 0 AND notes_metadata.updatedAt <= notes_metadata.selfHostSyncedAt)
                OR (notes_metadata.cloudSyncedAt > 0 AND notes_metadata.updatedAt <= notes_metadata.cloudSyncedAt)
            )
            AND ($holdsText)
            """.trimIndent()
        )
    }
}
