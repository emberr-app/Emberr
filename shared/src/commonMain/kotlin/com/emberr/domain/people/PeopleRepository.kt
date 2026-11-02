package com.emberr.domain.people

import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import kotlinx.coroutines.flow.Flow

data class PersonWithDetails(
    val person: NoteMetadataEntity,
    val details: List<PersonDetailBlock>
)

interface PeopleRepository {
    fun observePeople(): Flow<List<NoteMetadataEntity>>
    fun observePerson(personNoteId: String): Flow<PersonWithDetails?>
    fun observePeopleWithDetails(): Flow<List<PersonWithDetails>>
    fun observeEventsWithPerson(personNoteId: String): Flow<EventsWithPerson>
    fun observeEventsWithEachPerson(): Flow<Map<String, EventsWithPerson>>
    suspend fun createPerson(name: String): String
    suspend fun renamePerson(personNoteId: String, newName: String)
    suspend fun addDetail(personNoteId: String, kind: PersonDetailKind, label: String?, value: String): PersonDetailBlock?
    suspend fun updateDetail(personNoteId: String, detail: PersonDetailBlock)
    suspend fun removeDetail(personNoteId: String, detailId: String)
    suspend fun moveToTrash(personNoteId: String)
    suspend fun deletePersonIfLeftEmpty(personNoteId: String): Boolean
}
