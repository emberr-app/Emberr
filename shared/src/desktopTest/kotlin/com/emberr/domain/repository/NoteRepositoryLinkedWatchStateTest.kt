package com.emberr.domain.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.database.EmberrDatabase
import com.emberr.domain.ai.LocalAiEngine
import com.emberr.domain.ai.NoteIndexer
import com.emberr.domain.ai.external.AiSettingsRepository
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.space.ActiveSpaceStore
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteRepositoryLinkedWatchStateTest {

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
        canvasDao = appDatabase.canvasDao(),
        movieBlockDao = appDatabase.movieBlockDao()
    )

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

    private val interstellar = MovieDetails(tmdbId = 157336, kind = MovieKind.MOVIE, title = "Interstellar")

    private fun note(noteId: String) = NoteMetadataEntity(
        noteId = noteId,
        title = noteId,
        folderId = null,
        isDaily = false,
        dateString = null,
        createdAt = 1L,
        updatedAt = 1L,
        filePath = ""
    )

    private suspend fun saveNoteWithInterstellar(
        spaceId: String,
        noteId: String,
        status: WatchStatus,
        ratingHalfStars: Int?,
        changedAt: Long
    ) {
        val block = MovieBlock(
            id = "$noteId-movie",
            details = interstellar,
            watchStatus = status,
            ratingHalfStars = ratingHalfStars,
            watchStateChangedAt = changedAt,
            updatedAt = changedAt
        )
        repository.saveNoteInSpace(spaceId, note(noteId), NoteContent(blocks = listOf(block)))
    }

    private suspend fun interstellarIn(noteId: String): MovieBlock =
        repository.getNoteContent(noteId)!!.blocks.filterIsInstance<MovieBlock>().single()

    @Test
    fun aNewerStatusReachesTheSameTitleInItsSpaceButNotInAnotherSpace() = runTest {
        saveNoteWithInterstellar("space-one", "older-note", WatchStatus.NOT_WATCHED, ratingHalfStars = null, changedAt = 100L)
        saveNoteWithInterstellar("space-two", "other-space-note", WatchStatus.NOT_WATCHED, ratingHalfStars = null, changedAt = 100L)

        saveNoteWithInterstellar("space-one", "newer-note", WatchStatus.WATCHED, ratingHalfStars = 9, changedAt = 200L)

        val linked = interstellarIn("older-note")
        assertEquals(WatchStatus.WATCHED, linked.watchStatus)
        assertEquals(9, linked.ratingHalfStars)
        assertEquals(200L, linked.watchStateChangedAt)
        assertEquals(WatchStatus.NOT_WATCHED, interstellarIn("other-space-note").watchStatus)
    }

    @Test
    fun aNoteSavedWithAnOlderStatusTakesTheNewerOneFromItsSpace() = runTest {
        saveNoteWithInterstellar("space-one", "newer-note", WatchStatus.WATCHING, ratingHalfStars = 6, changedAt = 200L)

        saveNoteWithInterstellar("space-one", "older-note", WatchStatus.NOT_WATCHED, ratingHalfStars = null, changedAt = 100L)

        val adopted = interstellarIn("older-note")
        assertEquals(WatchStatus.WATCHING, adopted.watchStatus)
        assertEquals(6, adopted.ratingHalfStars)
        assertEquals(WatchStatus.WATCHING, interstellarIn("newer-note").watchStatus)
    }
}
