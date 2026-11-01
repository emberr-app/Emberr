package com.emberr.data.local.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonNotesHiddenFromNoteListsTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val noteDao = database.noteDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun noteInAccountSpace(noteId: String, kind: NoteKind) =
        NoteMetadataEntity(
            noteId = noteId,
            title = "Maya",
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 1L,
            updatedAt = 1L,
            filePath = "",
            spaceId = ACCOUNT_SPACE_ID,
            kind = kind
        )

    @Test
    fun aPersonIsLeftOutOfTheNoteListAndSearchButShowsInPeople() = runTest {
        noteDao.insertOrUpdateMetadata(noteInAccountSpace("note", NoteKind.NOTE))
        noteDao.insertOrUpdateMetadata(noteInAccountSpace("person", NoteKind.PERSON))

        assertEquals(listOf("note"), noteDao.getAllNotes(ACCOUNT_SPACE_ID).first().map { it.noteId })
        assertEquals(
            listOf("note"),
            noteDao.searchNotesByTitleOrSnippet(listOf(DEFAULT_SPACE_ID, ACCOUNT_SPACE_ID), "Maya").map { it.noteId }
        )
        assertEquals(listOf("person"), noteDao.observePeople(ACCOUNT_SPACE_ID).first().map { it.noteId })
    }
}
