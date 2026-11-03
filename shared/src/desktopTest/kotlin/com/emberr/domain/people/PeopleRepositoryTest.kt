package com.emberr.domain.people

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
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.model.RecurrenceEditScope
import com.emberr.domain.model.RecurrenceFrequency
import com.emberr.domain.model.RecurrenceRule
import com.emberr.domain.repository.NoteRepositoryImpl
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class PeopleRepositoryTest {

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

    private val noteRepository = NoteRepositoryImpl(
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

    private val sundayAfternoon = object : Clock {
        override fun now(): Instant = LocalDateTime(2026, 11, 1, 15, 0).toInstant(TimeZone.currentSystemDefault())
    }

    private val peopleRepository = PeopleRepositoryImpl(
        noteDao = appDatabase.noteDao(),
        blockDao = appDatabase.blockDao(),
        noteRepository = noteRepository,
        clock = sundayAfternoon
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

    private fun weeklyEventFromMondayOctober19(id: String, hour: Int, personNoteIds: List<String>) = CheckboxBlock(
        id = id,
        text = id,
        reminderTimestamp = timestampAt(2026, 10, 19, hour),
        recurrenceRule = RecurrenceRule(RecurrenceFrequency.WEEKLY),
        personNoteIds = personNoteIds,
        updatedAt = 1L
    )

    private suspend fun changePeopleOnOneOccurrence(blockId: String, occurrenceDate: String, hour: Int, personNoteIds: List<String>) {
        val day = occurrenceDate.split("-").map { it.toInt() }
        noteRepository.applyRecurrenceScopedEdit(
            blockId = blockId,
            occurrenceDate = occurrenceDate,
            scope = RecurrenceEditScope.THIS_EVENT,
            text = blockId,
            timestamp = timestampAt(day[0], day[1], day[2], hour),
            categoryId = null,
            durationMinutes = 30,
            url = null,
            description = null,
            allDay = null,
            personNoteIds = personNoteIds
        )
    }

    @Test
    fun eventsWithAPersonIncludeAnOccurrenceTheyWereAddedToAndLeaveOutOneTheyWereRemovedFrom() = runTest {
        val note = NoteMetadataEntity(
            noteId = "plans",
            title = "Plans",
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 1L,
            updatedAt = 1L,
            filePath = ""
        )
        noteRepository.saveNote(
            note,
            NoteContent(
                blocks = listOf(
                    weeklyEventFromMondayOctober19("lunch", hour = 12, personNoteIds = listOf("maya")),
                    weeklyEventFromMondayOctober19("standup", hour = 9, personNoteIds = emptyList())
                )
            )
        )
        changePeopleOnOneOccurrence("lunch", "2026-10-26", hour = 12, personNoteIds = emptyList())
        changePeopleOnOneOccurrence("standup", "2026-11-02", hour = 9, personNoteIds = listOf("maya"))

        val events = peopleRepository.observeEventsWithPerson("maya").first()

        assertEquals(
            listOf("standup" to "2026-11-02", "lunch" to "2026-11-02"),
            events.upcoming.map { it.blockId to it.targetDate }
        )
        assertEquals(listOf("lunch" to "2026-10-19"), events.past.map { it.blockId to it.targetDate })
    }

    @Test
    fun aNewPersonLeftEmptyIsDeletedForGoodButOneWithANameIsKept() = runTest {
        val emptyPersonId = peopleRepository.createPerson("")
        val namedPersonId = peopleRepository.createPerson("Maya")

        assertTrue(peopleRepository.deletePersonIfLeftEmpty(emptyPersonId))
        assertFalse(peopleRepository.deletePersonIfLeftEmpty(namedPersonId))

        assertNull(noteRepository.getNoteById(emptyPersonId))
        assertNotNull(noteRepository.getNoteById(namedPersonId))
    }

    @Test
    fun aBatchSkipsACardThatAddsNothingNewAndNumbersASameNamedCardWithANewPhone() = runTest {
        fun mayaWithPhone(phone: String) = ContactToImport(
            name = "Maya",
            details = listOf(ContactDetailToImport(kind = PersonDetailKind.PHONE, label = null, value = phone)),
            note = "",
            photoBytes = null
        )

        val result = peopleRepository.createPeopleFromContacts(
            contacts = listOf(mayaWithPhone("555 123 4567"), mayaWithPhone("5551234567"), mayaWithPhone("555 987 6543")),
            savePhoto = { error("These cards have no photo") },
            onContactHandled = {}
        )

        assertEquals(2, result.importedPersonNoteIds.size)
        assertEquals(1, result.copiesSkipped)
        assertEquals(0, result.failures)
        assertEquals(listOf("Maya", "Maya (1)"), result.importedPersonNoteIds.map { noteRepository.getNoteById(it)?.title })
    }
}
