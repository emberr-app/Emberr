package com.emberr.data.local.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.search.searchGlobPattern
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteSearchIgnoresCaseAndAccentsTest {

    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val noteDao = database.noteDao()

    @AfterTest
    fun closeDatabase() {
        database.close()
    }

    private fun note(noteId: String, title: String) =
        NoteMetadataEntity(
            noteId = noteId,
            title = title,
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 1L,
            updatedAt = 1L,
            filePath = "",
            spaceId = DEFAULT_SPACE_ID,
            kind = NoteKind.NOTE
        )

    private suspend fun idsFoundBy(query: String): List<String> =
        noteDao.searchNotesByTitleOrSnippet(listOf(DEFAULT_SPACE_ID), searchGlobPattern(query)).map { it.noteId }

    @Test
    fun searchFindsTitlesWithAccentedCapitalsAndWithoutTheAccent() = runTest {
        noteDao.insertOrUpdateMetadata(note("vienna", "Über Wien"))
        noteDao.insertOrUpdateMetadata(note("lodz", "Łódź trip"))
        noteDao.insertOrUpdateMetadata(note("percent", "100% done"))

        assertEquals(listOf("vienna"), idsFoundBy("über"))
        assertEquals(listOf("vienna"), idsFoundBy("uber"))
        assertEquals(listOf("lodz"), idsFoundBy("lodz"))
        assertEquals(listOf("percent"), idsFoundBy("0%"))
        assertEquals(emptyList(), idsFoundBy("ü_er"))
    }
}
