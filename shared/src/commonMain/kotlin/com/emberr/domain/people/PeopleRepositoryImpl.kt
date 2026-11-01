package com.emberr.domain.people

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.model.markDeleted
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.sync.SyncCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PeopleRepositoryImpl(
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val calendarTaskDao: CalendarTaskDao,
    private val noteRepository: NoteRepository
) : PeopleRepository {

    private val blockJson = Json { ignoreUnknownKeys = true }

    override fun observePeople(): Flow<List<NoteMetadataEntity>> = noteDao.observePeople(ACCOUNT_SPACE_ID)

    override fun observePerson(personNoteId: String): Flow<PersonWithDetails?> =
        combine(noteDao.observeNoteById(personNoteId), blockDao.observeBlocksForNote(personNoteId)) { note, blockRows ->
            val person = note?.takeIf { it.kind == NoteKind.PERSON } ?: return@combine null
            val details = blockRows.mapNotNull { row -> decodeBlockOrNull(row.blockDataJson) as? PersonDetailBlock }
            PersonWithDetails(person = person, details = details.filterNot { it.isDeleted })
        }.flowOn(Dispatchers.Default)

    override fun observeEventsWithPerson(personNoteId: String): Flow<EventsWithPerson> =
        calendarTaskDao.observeTasksWithPerson(personNoteId).map { events ->
            splitEventsWithPerson(events, Clock.System.now(), TimeZone.currentSystemDefault())
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

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun decodeBlockOrNull(blockDataJson: String): NoteBlock? =
        try {
            blockJson.decodeFromString(NoteBlockSerializer, blockDataJson)
        } catch (_: SerializationException) {
            null
        }
}
