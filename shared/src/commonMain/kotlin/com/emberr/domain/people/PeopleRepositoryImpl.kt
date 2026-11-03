package com.emberr.domain.people

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.personNoteIdList
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.markDeleted
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.sync.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val DAYS_OF_REPEATING_EVENTS_LOOKED_AT_EACH_WAY = 366

@OptIn(ExperimentalUuidApi::class)
class PeopleRepositoryImpl(
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val noteRepository: NoteRepository,
    private val clock: Clock = Clock.System
) : PeopleRepository {

    private val blockJson = Json { ignoreUnknownKeys = true }

    override fun observePeople(): Flow<List<NoteMetadataEntity>> = noteDao.observePeople(ACCOUNT_SPACE_ID)

    override fun observePerson(personNoteId: String): Flow<PersonWithDetails?> =
        combine(noteDao.observeNoteById(personNoteId), blockDao.observeBlocksForNote(personNoteId)) { note, blockRows ->
            val person = note?.takeIf { it.kind == NoteKind.PERSON } ?: return@combine null
            val details = blockRows.mapNotNull { row -> decodeBlockOrNull(row.blockDataJson) as? PersonDetailBlock }
            PersonWithDetails(person = person, details = details.filterNot { it.isDeleted })
        }.flowOn(Dispatchers.Default)

    override fun observePeopleWithDetails(): Flow<List<PersonWithDetails>> =
        combine(
            noteDao.observePeople(ACCOUNT_SPACE_ID),
            blockDao.observeLiveDetailBlocksOfPeople(ACCOUNT_SPACE_ID)
        ) { people, detailRows ->
            val detailsByPersonNoteId = detailRows.groupBy(
                keySelector = { row -> row.noteId },
                valueTransform = { row -> decodeBlockOrNull(row.blockDataJson) as? PersonDetailBlock }
            )
            people.map { person ->
                val details = detailsByPersonNoteId[person.noteId].orEmpty().filterNotNull().filterNot { it.isDeleted }
                PersonWithDetails(person = person, details = details)
            }
        }.flowOn(Dispatchers.Default)

    override fun observeEventsWithPerson(personNoteId: String): Flow<EventsWithPerson> =
        observeEventsWithAnyone()
            .map { (events, now) -> eventsWithPerson(personNoteId, events, now, TimeZone.currentSystemDefault()) }
            .flowOn(Dispatchers.Default)

    override fun observeEventsWithEachPerson(): Flow<Map<String, EventsWithPerson>> =
        observeEventsWithAnyone()
            .map { (events, now) ->
                val personNoteIds = events.flatMap { event -> event.personNoteIdList() }.toSet()
                personNoteIds.associateWith { personNoteId ->
                    eventsWithPerson(personNoteId, events, now, TimeZone.currentSystemDefault())
                }
            }
            .flowOn(Dispatchers.Default)

    private fun observeEventsWithAnyone(): Flow<Pair<List<CalendarTaskEntity>, Instant>> = flow {
        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val firstDateLookedAt = today.minus(DAYS_OF_REPEATING_EVENTS_LOOKED_AT_EACH_WAY, DateTimeUnit.DAY)
        val lastDateLookedAt = today.plus(DAYS_OF_REPEATING_EVENTS_LOOKED_AT_EACH_WAY, DateTimeUnit.DAY)
        val eventsAndOccurrences = combine(
            noteRepository.getAllTasksFlow(),
            noteRepository.getCalendarTasksInRange(firstDateLookedAt.toString(), lastDateLookedAt.toString()),
            everyMinute()
        ) { allEvents, occurrencesNearToday, now ->
            val oneOffEvents = allEvents.filter { event -> event.recurrenceFrequency == null && event.personNoteIds != null }
            val repeatingOccurrences = occurrencesNearToday.filter { event -> event.recurrenceFrequency != null && event.personNoteIds != null }
            (oneOffEvents + repeatingOccurrences) to now
        }
        emitAll(eventsAndOccurrences)
    }

    private fun everyMinute(): Flow<Instant> = flow {
        while (true) {
            emit(clock.now())
            delay(1.minutes)
        }
    }

    override suspend fun createPerson(name: String): String = withContext(Dispatchers.IO) {
        val now = nowMillis()
        val person = NoteMetadataEntity(
            noteId = Uuid.random().toString(),
            title = name.trim(),
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = now,
            updatedAt = now,
            filePath = "",
            spaceId = ACCOUNT_SPACE_ID,
            kind = NoteKind.PERSON,
            titleUpdatedAt = now
        )
        SyncCoordinator.mutex.withLock {
            noteRepository.saveNoteInSpace(ACCOUNT_SPACE_ID, person, NoteContent(blocks = emptyList()))
        }
        person.noteId
    }

    override suspend fun createPersonFromContactUnlessCopy(contact: ContactToImport, photoFileName: String?): String? = withContext(Dispatchers.IO) {
        val now = nowMillis()
        val details = contact.details
            .filter { detail -> detail.value.isNotBlank() }
            .map { detail ->
                PersonDetailBlock(
                    id = Uuid.random().toString(),
                    kind = detail.kind,
                    label = detail.label?.trim()?.takeIf { it.isNotEmpty() },
                    value = detail.value.trim(),
                    updatedAt = now
                )
            }
        val noteLines = contact.note.lines()
            .map { line -> line.trimEnd() }
            .filter { line -> line.isNotBlank() }
            .map { line -> TextBlock(id = Uuid.random().toString(), text = line, updatedAt = now) }
        SyncCoordinator.mutex.withLock {
            val people = noteDao.observePeople(ACCOUNT_SPACE_ID).first()
            if (isCopyOfSomeone(contact, people)) return@withLock null
            val takenNames = people.map { person -> person.title }
            val person = NoteMetadataEntity(
                noteId = Uuid.random().toString(),
                title = nameNotTakenYet(contact.name, takenNames),
                folderId = null,
                isDaily = false,
                dateString = null,
                createdAt = now,
                updatedAt = now,
                filePath = "",
                coverImagePath = photoFileName,
                spaceId = ACCOUNT_SPACE_ID,
                kind = NoteKind.PERSON,
                titleUpdatedAt = now,
                coverImageUpdatedAt = if (photoFileName != null) now else 0L
            )
            noteRepository.saveNoteInSpace(ACCOUNT_SPACE_ID, person, NoteContent(blocks = details + noteLines))
            person.noteId
        }
    }

    private suspend fun isCopyOfSomeone(contact: ContactToImport, people: List<NoteMetadataEntity>): Boolean {
        val sameNamePeople = people.filter { person -> hasSamePersonName(person.title, contact.name) }
        if (sameNamePeople.isEmpty()) return false
        val sameNamePersonIds = sameNamePeople.map { person -> person.noteId }.toSet()
        val detailsByPersonId = blockDao.observeLiveDetailBlocksOfPeople(ACCOUNT_SPACE_ID).first()
            .filter { row -> row.noteId in sameNamePersonIds }
            .groupBy(
                keySelector = { row -> row.noteId },
                valueTransform = { row -> decodeBlockOrNull(row.blockDataJson) as? PersonDetailBlock }
            )
        return sameNamePeople.any { person ->
            val details = detailsByPersonId[person.noteId].orEmpty()
                .filterNotNull()
                .filterNot { it.isDeleted }
                .map { detail -> ContactDetailToImport(kind = detail.kind, label = detail.label, value = detail.value) }
            contactAddsNothingNewTo(contact, person.title, details)
        }
    }

    override suspend fun renamePerson(personNoteId: String, newName: String) {
        val trimmedName = newName.trim()
        changePerson(personNoteId) { person, blocks ->
            if (person.title == trimmedName) null
            else person.copy(title = trimmedName, titleUpdatedAt = nowMillis()) to blocks
        }
    }

    override suspend fun addDetail(
        personNoteId: String,
        kind: PersonDetailKind,
        label: String?,
        value: String
    ): PersonDetailBlock? {
        val detail = PersonDetailBlock(
            id = Uuid.random().toString(),
            kind = kind,
            label = label?.trim()?.takeIf { it.isNotEmpty() },
            value = value.trim(),
            updatedAt = nowMillis()
        )
        val wasAdded = changePerson(personNoteId) { person, blocks ->
            val positionAfterLastDetail = blocks.indexOfLast { it is PersonDetailBlock && !it.isDeleted } + 1
            person to blocks.toMutableList().apply { add(positionAfterLastDetail, detail) }
        }
        return detail.takeIf { wasAdded }
    }

    override suspend fun updateDetail(personNoteId: String, detail: PersonDetailBlock) {
        changePerson(personNoteId) { person, blocks ->
            if (blocks.none { it.id == detail.id && it is PersonDetailBlock }) return@changePerson null
            val updatedDetail = detail.copy(
                label = detail.label?.trim()?.takeIf { it.isNotEmpty() },
                value = detail.value.trim(),
                updatedAt = nowMillis()
            )
            person to blocks.map { block -> if (block.id == detail.id) updatedDetail else block }
        }
    }

    override suspend fun removeDetail(personNoteId: String, detailId: String) {
        changePerson(personNoteId) { person, blocks ->
            if (blocks.none { it.id == detailId && it is PersonDetailBlock && !it.isDeleted }) return@changePerson null
            person to blocks.map { block -> if (block.id == detailId) block.markDeleted() else block }
        }
    }

    override suspend fun moveToTrash(personNoteId: String) {
        withContext(Dispatchers.IO) {
            SyncCoordinator.mutex.withLock {
                val person = noteDao.getNoteById(personNoteId)
                    ?.takeIf { it.kind == NoteKind.PERSON && it.trashedAt == null }
                    ?: return@withLock
                val content = noteRepository.getNoteContent(personNoteId) ?: NoteContent(blocks = emptyList())
                val now = nowMillis()
                noteRepository.saveNoteInSpace(ACCOUNT_SPACE_ID, person.copy(trashedAt = now, updatedAt = now), content)
            }
        }
    }

    override suspend fun deletePersonIfLeftEmpty(personNoteId: String): Boolean = withContext(Dispatchers.IO) {
        val isLeftEmpty = SyncCoordinator.mutex.withLock {
            val person = noteDao.getNoteById(personNoteId) ?: return@withLock false
            val blocks = noteRepository.getNoteContent(personNoteId)?.blocks.orEmpty()
            isEmptyPerson(person, blocks)
        }
        if (isLeftEmpty) noteRepository.deleteNote(personNoteId, filePath = "")
        isLeftEmpty
    }

    private fun isEmptyPerson(person: NoteMetadataEntity, blocks: List<NoteBlock>): Boolean =
        person.kind == NoteKind.PERSON &&
            person.trashedAt == null &&
            person.title.isBlank() &&
            blocks.filterNot { it.isDeleted }.all { block -> block is TextBlock && block.text.isBlank() }

    private suspend fun changePerson(
        personNoteId: String,
        change: (NoteMetadataEntity, List<NoteBlock>) -> Pair<NoteMetadataEntity, List<NoteBlock>>?
    ): Boolean = withContext(Dispatchers.IO) {
        SyncCoordinator.mutex.withLock {
            val person = noteDao.getNoteById(personNoteId)?.takeIf { it.kind == NoteKind.PERSON } ?: return@withLock false
            val blocks = noteRepository.getNoteContent(personNoteId)?.blocks.orEmpty()
            val (changedPerson, changedBlocks) = change(person, blocks) ?: return@withLock false
            noteRepository.saveNoteInSpace(ACCOUNT_SPACE_ID, changedPerson, NoteContent(blocks = changedBlocks))
            true
        }
    }

    private fun nowMillis(): Long = clock.now().toEpochMilliseconds()

    private fun decodeBlockOrNull(blockDataJson: String): NoteBlock? =
        try {
            blockJson.decodeFromString(NoteBlockSerializer, blockDataJson)
        } catch (_: SerializationException) {
            null
        }
}
