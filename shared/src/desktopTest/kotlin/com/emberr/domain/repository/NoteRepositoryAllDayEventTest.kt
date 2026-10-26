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
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.space.ActiveSpaceStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteRepositoryAllDayEventTest {

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

    private fun timestampAt(year: Int, month: Int, day: Int, hour: Int): Long =
        LocalDateTime(year, month, day, hour, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test
    fun repeatingThreeDayEventShowsOnEveryDayItCoversIncludingDaysInTheNextMonth() = runTest {
        val monthlyTrip = CheckboxBlock(
            id = "trip",
            text = "Trip",
            reminderTimestamp = timestampAt(2026, 10, 30, 9),
            recurrenceRule = RecurrenceRule(RecurrenceFrequency.MONTHLY),
            allDay = AllDaySpan(lengthInDays = 3),
            updatedAt = 1L
        )
        val note = NoteMetadataEntity(
            noteId = "note-1",
            title = "Travel",
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 1L,
            updatedAt = 1L,
            filePath = ""
        )
        repository.saveNote(note, NoteContent(blocks = listOf(monthlyTrip)))

        val tripOnLastDay = repository.getCalendarTasksForDate("2026-11-01").first().single { it.blockId == "trip" }
        assertEquals("2026-10-30", tripOnLastDay.targetDate)
        assertEquals(0, repository.getCalendarTasksForDate("2026-11-02").first().count { it.blockId == "trip" })
        assertEquals(
            listOf("2026-10-30", "2026-11-30"),
            repository.getCalendarTasksForMonth("2026-11").first().filter { it.blockId == "trip" }.map { it.targetDate }
        )
    }
}
