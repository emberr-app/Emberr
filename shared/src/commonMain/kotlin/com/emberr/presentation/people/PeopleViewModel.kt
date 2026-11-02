package com.emberr.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.util.system.COULD_NOT_MOVE_TO_TRASH_MESSAGE
import com.emberr.domain.util.system.movedToTrashMessage
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.home.note.NoteEditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val STOP_LISTENING_AFTER_MILLIS = 5_000L

data class PersonListRow(
    val person: NoteMetadataEntity,
    val nextEvent: CalendarTaskEntity?,
    val eventCount: Int
)

class PeopleViewModel(
    private val peopleRepository: PeopleRepository,
    private val appScope: CoroutineScope
) : ViewModel() {

    val rows: StateFlow<List<PersonListRow>?> = combine(
        peopleRepository.observePeople(),
        peopleRepository.observeEventsWithEachPerson()
    ) { people, eventsByPerson ->
        people
            .map { person ->
                val events = eventsByPerson[person.noteId]
                PersonListRow(
                    person = person,
                    nextEvent = events?.upcoming?.firstOrNull(),
                    eventCount = events?.let { it.upcoming.size + it.past.size } ?: 0
                )
            }
            .sortedWith(compareBy({ row -> row.person.title.isBlank() }, { row -> row.person.title.trim().lowercase() }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_LISTENING_AFTER_MILLIS), null)

    fun createPerson(name: String, onCreated: (personNoteId: String) -> Unit) {
        viewModelScope.launch {
            try {
                onCreated(peopleRepository.createPerson(name))
            } catch (e: Exception) {
                e.printStackTrace()
                showNativeToast("Couldn't create the person")
            }
        }
    }

    fun moveToTrash(person: NoteMetadataEntity) {
        viewModelScope.launch {
            try {
                peopleRepository.moveToTrash(person.noteId)
                showNativeToast(movedToTrashMessage(person.title))
            } catch (e: Exception) {
                e.printStackTrace()
                showNativeToast(COULD_NOT_MOVE_TO_TRASH_MESSAGE)
            }
        }
    }

    fun deletePersonIfLeftEmpty(personNoteId: String, notesEditor: NoteEditorViewModel) {
        appScope.launch {
            try {
                withContext(Dispatchers.Main) { notesEditor.flushPendingSave() }
                val wasDeleted = peopleRepository.deletePersonIfLeftEmpty(personNoteId)
                if (wasDeleted) withContext(Dispatchers.Main) { notesEditor.discardPendingWrites() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
