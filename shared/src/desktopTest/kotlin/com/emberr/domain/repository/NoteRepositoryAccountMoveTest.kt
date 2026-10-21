package com.emberr.domain.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.database.EmberrDatabase
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.NoteIndexer
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.database.databaseSettingsBlockId
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.TextBlock
import com.emberr.domain.space.ActiveSpaceStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NoteRepositoryAccountMoveTest {

    private val appDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val aiIndexDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { EmberrDatabase.Schema.create(it) }

    private val settings: SettingsManager = stubOf { methodName ->
        when (methodName) {
            "getActiveSpaceId" -> DEFAULT_SPACE_ID
            "getActiveSpaceIdFlow" -> flowOf(DEFAULT_SPACE_ID)
            "isAiFeaturesDisabled" -> true
            else -> error("These tests do not expect SettingsManager.$methodName to be called")
        }
    }
    private val aiSettingsThatAreNeverRead: AiSettingsRepository = stubOf { methodName ->
        error("These tests do not expect AiSettingsRepository.$methodName to be called")
    }

    private val repository = NoteRepositoryImpl(
        activeSpaceStore = ActiveSpaceStore(settings),
        noteDao = appDatabase.noteDao(),
        folderDao = appDatabase.folderDao(),
        blockDao = appDatabase.blockDao(),
        noteIndexer = NoteIndexer(lazyOf(EmberrDatabase(aiIndexDriver)), LocalAiEngine(aiSettingsThatAreNeverRead), settings),
        calendarTaskDao = appDatabase.calendarTaskDao(),
        calendarEventExceptionDao = appDatabase.calendarEventExceptionDao(),
        imageBlockDao = appDatabase.imageBlockDao(),
        documentBlockDao = appDatabase.documentBlockDao(),
        bookmarkBlockDao = appDatabase.bookmarkBlockDao(),
        categoryDao = appDatabase.categoryDao(),
        propertyTagDao = appDatabase.propertyTagDao(),
        customPropertyDao = appDatabase.customPropertyDao(),
        selfHostDeletedNoteDao = appDatabase.selfHostDeletedNoteDao(),
        mediaReferenceDao = appDatabase.mediaReferenceDao(),
        canvasDao = appDatabase.canvasDao()
    )

    private val workSpaceId = "work-space"
    private val date = "2026-10-21"

    @AfterTest
    fun closeDatabases() {
        appDatabase.close()
        aiIndexDriver.close()
    }

    private inline fun <reified T : Any> stubOf(crossinline answer: (methodName: String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> answer(method.name)
            }
        } as T

    private fun noteMetadata(noteId: String, createdAt: Long, dailyDate: String?) = NoteMetadataEntity(
        noteId = noteId,
        title = dailyDate?.let { "Daily: $it" } ?: "Plans",
        folderId = null,
        isDaily = dailyDate != null,
        dateString = dailyDate,
        createdAt = createdAt,
        updatedAt = createdAt,
        filePath = ""
    )

    @Test
    fun dailyNotesOnTheSameDateInTwoSpacesBecomeOneAccountNoteWithTheOlderNotesBlocksFirst() = runTest {
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("work-daily", createdAt = 2L, dailyDate = date),
            NoteContent(blocks = listOf(TextBlock(id = "work-text", text = "Standup", updatedAt = 1L)))
        )
        repository.saveNoteInSpace(
            DEFAULT_SPACE_ID,
            noteMetadata("personal-daily", createdAt = 1L, dailyDate = date),
            NoteContent(
                blocks = listOf(
                    TextBlock(id = "personal-first", text = "Run", updatedAt = 1L),
                    TextBlock(id = "personal-second", text = "Read", updatedAt = 1L)
                )
            )
        )

        repository.moveDailyNotesAndCategoriesToAccount(backgroundScope)

        val mergedNote = appDatabase.noteDao().getAllDailyNoteMetadataAcrossSpaces().single()
        assertEquals("personal-daily", mergedNote.noteId)
        assertEquals(ACCOUNT_SPACE_ID, mergedNote.spaceId)
        assertEquals(
            listOf("personal-first", "personal-second", "work-text"),
            repository.getDailyNote(date)?.blocks?.map { it.id }
        )

        val blocksAfterFirstMerge = appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted("personal-daily")
        repository.moveDailyNotesAndCategoriesToAccount(backgroundScope)

        assertEquals(listOf(mergedNote), appDatabase.noteDao().getAllDailyNoteMetadataAcrossSpaces())
        assertEquals(blocksAfterFirstMerge, appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted("personal-daily"))
    }

    @Test
    fun aBlockInBothCopiesOfADailyNoteKeepsTheNewerVersionEvenWhenThatVersionIsADeletion() = runTest {
        val dateWithDeletion = "2026-10-22"
        repository.saveNoteInSpace(
            DEFAULT_SPACE_ID,
            noteMetadata("personal-daily", createdAt = 1L, dailyDate = date),
            NoteContent(blocks = listOf(TextBlock(id = "shared", text = "Old text", updatedAt = 1L)))
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("work-daily", createdAt = 2L, dailyDate = date),
            NoteContent(blocks = listOf(TextBlock(id = "shared", text = "New text", updatedAt = 2L)))
        )
        repository.saveNoteInSpace(
            DEFAULT_SPACE_ID,
            noteMetadata("personal-daily-with-deletion", createdAt = 1L, dailyDate = dateWithDeletion),
            NoteContent(blocks = listOf(TextBlock(id = "removed", text = "Errand", updatedAt = 1L)))
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("work-daily-with-deletion", createdAt = 2L, dailyDate = dateWithDeletion),
            NoteContent(blocks = listOf(TextBlock(id = "removed", text = "Errand", updatedAt = 2L, isDeleted = true)))
        )

        repository.mergeDailyNotesAndCategoriesIntoAccount()

        val blockDao = appDatabase.blockDao()
        assertEquals("New text", (repository.getDailyNote(date)?.blocks?.single() as TextBlock).text)
        assertTrue(blockDao.getAllBlocksForNoteIncludingDeleted("personal-daily-with-deletion").single().isDeleted)

        val survivorIds = listOf("personal-daily", "personal-daily-with-deletion")
        val notesAfterFirstMerge = appDatabase.noteDao().getAllNotesForBackup()
        val blocksAfterFirstMerge = survivorIds.map { blockDao.getAllBlocksForNoteIncludingDeleted(it) }
        repository.mergeDailyNotesAndCategoriesIntoAccount()

        assertEquals(notesAfterFirstMerge, appDatabase.noteDao().getAllNotesForBackup())
        assertEquals(blocksAfterFirstMerge, survivorIds.map { blockDao.getAllBlocksForNoteIncludingDeleted(it) })
    }

    @Test
    fun sameNamedCategoriesInTwoSpacesMergeAndACheckboxInARegularNotePointsToTheSurvivor() = runTest {
        val categoryDao = appDatabase.categoryDao()
        categoryDao.insertOrUpdateCategory(
            CategoryEntity(categoryId = "personal-work", name = "Work", colorHex = "#111111", createdAt = 1L, spaceId = DEFAULT_SPACE_ID)
        )
        categoryDao.insertOrUpdateCategory(
            CategoryEntity(categoryId = "work-work", name = " work ", colorHex = "#222222", createdAt = 2L, spaceId = workSpaceId)
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(CheckboxBlock(id = "meeting", text = "Meeting", categoryId = "work-work", updatedAt = 1L)))
        )

        repository.moveDailyNotesAndCategoriesToAccount(backgroundScope)

        assertEquals(listOf("personal-work"), repository.getAllCategories().first().map { it.categoryId })
        assertTrue(categoryDao.getCategoryById("work-work")?.isDeleted == true)
        val storedJson = appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted("plans").single().blockDataJson
        val storedCheckbox = Json { ignoreUnknownKeys = true }.decodeFromString(NoteBlockSerializer, storedJson) as CheckboxBlock
        assertEquals("personal-work", storedCheckbox.categoryId)
    }

    @Test
    fun aCheckboxStillPointingAtACategoryThatArrivedDeletedMovesToTheLiveCategoryWithTheSameName() = runTest {
        val categoryDao = appDatabase.categoryDao()
        categoryDao.insertOrUpdateCategory(
            CategoryEntity(categoryId = "a", name = "Work", colorHex = "#111111", createdAt = 1L, spaceId = ACCOUNT_SPACE_ID)
        )
        categoryDao.insertOrUpdateCategory(
            CategoryEntity(categoryId = "b", name = " work ", colorHex = "#222222", createdAt = 2L, isDeleted = true, spaceId = ACCOUNT_SPACE_ID)
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(CheckboxBlock(id = "meeting", text = "Meeting", categoryId = "b", updatedAt = 1L)))
        )

        repository.mergeDailyNotesAndCategoriesIntoAccount()

        val blocksAfterFirstMerge = appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted("plans")
        val storedCheckbox = Json { ignoreUnknownKeys = true }
            .decodeFromString(NoteBlockSerializer, blocksAfterFirstMerge.single().blockDataJson) as CheckboxBlock
        assertEquals("a", storedCheckbox.categoryId)

        val secondMerge = repository.mergeDailyNotesAndCategoriesIntoAccount()

        assertEquals(false, secondMerge.categoriesChanged)
        assertEquals(blocksAfterFirstMerge, appDatabase.blockDao().getAllBlocksForNoteIncludingDeleted("plans"))
    }

    @Test
    fun aMovedCategoryIsNewerThanItsOldSpaceCopySoSyncCannotMoveItBack() = runTest {
        val oldSpaceCopy = CategoryEntity(
            categoryId = "errands", name = "Errands", colorHex = "#333333", createdAt = 1L, updatedAt = 5L, spaceId = workSpaceId
        )
        appDatabase.categoryDao().insertOrUpdateCategory(oldSpaceCopy)

        repository.mergeDailyNotesAndCategoriesIntoAccount()

        val movedCategory = appDatabase.categoryDao().getCategoryById("errands")
        assertEquals(ACCOUNT_SPACE_ID, movedCategory?.spaceId)
        assertTrue(movedCategory!!.updatedAt > oldSpaceCopy.updatedAt)
    }

    @Test
    fun searchInARegularSpaceFindsWordsInsideADailyNoteButNotInAnotherSpace() = runTest {
        val textLongerThanTheSnippet = "Morning pages ".repeat(10)
        repository.saveDailyNote(
            date,
            NoteContent(
                blocks = listOf(
                    TextBlock(id = "opening", text = textLongerThanTheSnippet, updatedAt = 1L),
                    TextBlock(id = "budget", text = "Quarterly budget review", updatedAt = 1L)
                )
            )
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("work-plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(TextBlock(id = "work-budget", text = "Quarterly budget for work", updatedAt = 1L)))
        )

        val results = repository.searchNotes("Quarterly")

        val dailyNote = appDatabase.noteDao().getAllDailyNoteMetadataAcrossSpaces().single()
        assertEquals(listOf(dailyNote.noteId), results.map { it.note.noteId })
        assertEquals("Quarterly budget review", results.single().matchedText)
    }

    private fun subNoteMetadata(noteId: String, kind: NoteKind = NoteKind.NOTE, databaseId: String? = null) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 1L,
        updatedAt = 1L,
        filePath = "",
        isSubNote = true,
        kind = kind,
        databaseId = databaseId
    )

    private suspend fun saveDatabaseNoteInWork(databaseId: String) {
        repository.saveNoteInSpace(
            workSpaceId,
            subNoteMetadata(databaseNoteId(databaseId), kind = NoteKind.DATABASE),
            NoteContent(blocks = listOf(DatabaseBlock(id = databaseSettingsBlockId(databaseId), databaseId = databaseId, updatedAt = 1L)))
        )
    }

    @Test
    fun subNotesShownOnlyInADailyNoteMoveToAccountButADatabaseAlsoShownInARegularNoteStays() = runTest {
        repository.saveDailyNote(
            date,
            NoteContent(
                blocks = listOf(
                    CanvasBlock(id = "sketch-block", canvasNoteId = "sketch", updatedAt = 1L),
                    DatabaseBlock(id = "reading-view", databaseId = "reading", updatedAt = 1L),
                    DatabaseBlock(id = "budget-daily-view", databaseId = "budget", updatedAt = 1L)
                )
            )
        )
        repository.saveNoteInSpace(workSpaceId, subNoteMetadata("sketch", kind = NoteKind.CANVAS), NoteContent(blocks = emptyList()))
        saveDatabaseNoteInWork("reading")
        repository.saveNoteInSpace(workSpaceId, subNoteMetadata("first-book", databaseId = "reading"), NoteContent(blocks = emptyList()))
        repository.saveNoteInSpace(workSpaceId, subNoteMetadata("second-book", databaseId = "reading"), NoteContent(blocks = emptyList()))
        saveDatabaseNoteInWork("budget")
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(DatabaseBlock(id = "budget-plans-view", databaseId = "budget", updatedAt = 1L)))
        )

        val firstMerge = repository.mergeDailyNotesAndCategoriesIntoAccount()

        val noteDao = appDatabase.noteDao()
        val subNotesOfTheDailyNote = listOf("sketch", databaseNoteId("reading"), "first-book", "second-book")
        assertEquals(subNotesOfTheDailyNote.toSet(), firstMerge.movedSubNoteIds)
        assertEquals(subNotesOfTheDailyNote.map { ACCOUNT_SPACE_ID }, subNotesOfTheDailyNote.map { noteDao.getNoteById(it)?.spaceId })
        assertEquals(workSpaceId, noteDao.getNoteById(databaseNoteId("budget"))?.spaceId)

        val notesAfterFirstMerge = noteDao.getAllNotesForBackup()
        val secondMerge = repository.mergeDailyNotesAndCategoriesIntoAccount()

        assertTrue(secondMerge.movedSubNoteIds.isEmpty())
        assertEquals(notesAfterFirstMerge, noteDao.getAllNotesForBackup())
    }

    @Test
    fun aDatabaseFollowsTheNoteThatOwnsItAndIgnoresLinksFromOtherNotes() = runTest {
        repository.saveDailyNote(
            date,
            NoteContent(
                blocks = listOf(
                    DatabaseBlock(id = "reading-daily-view", databaseId = "reading", updatedAt = 1L),
                    DatabaseBlock(id = "budget-daily-link", databaseId = "budget", isLinkedDatabase = true, updatedAt = 1L)
                )
            )
        )
        saveDatabaseNoteInWork("reading")
        repository.saveNoteInSpace(workSpaceId, subNoteMetadata("first-book", databaseId = "reading"), NoteContent(blocks = emptyList()))
        saveDatabaseNoteInWork("budget")
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("books", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(DatabaseBlock(id = "reading-books-link", databaseId = "reading", isLinkedDatabase = true, updatedAt = 1L)))
        )
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(DatabaseBlock(id = "budget-plans-view", databaseId = "budget", updatedAt = 1L)))
        )

        val firstMerge = repository.mergeDailyNotesAndCategoriesIntoAccount()

        val noteDao = appDatabase.noteDao()
        val readingDatabase = listOf(databaseNoteId("reading"), "first-book")
        assertEquals(readingDatabase.toSet(), firstMerge.movedSubNoteIds)
        assertEquals(readingDatabase.map { ACCOUNT_SPACE_ID }, readingDatabase.map { noteDao.getNoteById(it)?.spaceId })
        assertEquals(workSpaceId, noteDao.getNoteById(databaseNoteId("budget"))?.spaceId)

        val notesAfterFirstMerge = noteDao.getAllNotesForBackup()
        val secondMerge = repository.mergeDailyNotesAndCategoriesIntoAccount()

        assertTrue(secondMerge.movedSubNoteIds.isEmpty())
        assertEquals(notesAfterFirstMerge, noteDao.getAllNotesForBackup())
    }

    @Test
    fun deletingASpaceKeepsADatabaseThatADailyNoteStillShows() = runTest {
        repository.saveDailyNote(
            date,
            NoteContent(blocks = listOf(DatabaseBlock(id = "budget-daily-view", databaseId = "budget", updatedAt = 1L)))
        )
        saveDatabaseNoteInWork("budget")
        repository.saveNoteInSpace(workSpaceId, subNoteMetadata("budget-row", databaseId = "budget"), NoteContent(blocks = emptyList()))
        repository.saveNoteInSpace(
            workSpaceId,
            noteMetadata("plans", createdAt = 1L, dailyDate = null),
            NoteContent(blocks = listOf(DatabaseBlock(id = "budget-plans-view", databaseId = "budget", updatedAt = 1L)))
        )

        repository.deleteAllContentInSpace(workSpaceId)

        val noteDao = appDatabase.noteDao()
        assertEquals(null, noteDao.getNoteById("plans"))
        val databaseAndRow = listOf(databaseNoteId("budget"), "budget-row")
        assertEquals(databaseAndRow.map { ACCOUNT_SPACE_ID }, databaseAndRow.map { noteDao.getNoteById(it)?.spaceId })
    }
}
