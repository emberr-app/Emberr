package com.emberr.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.people.ContactImportProgress
import com.emberr.domain.people.ContactImportResult
import com.emberr.domain.people.ContactImporter
import com.emberr.domain.people.ContactToImport
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.util.system.COULD_NOT_MOVE_TO_TRASH_MESSAGE
import com.emberr.domain.util.system.movedToTrashMessage
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.home.note.NoteEditorViewModel
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.people_already_importing_contacts
import emberr.shared.generated.resources.people_could_not_create_person
import emberr.shared.generated.resources.people_moved_people_to_trash
import emberr.shared.generated.resources.people_moved_some_people_to_trash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

private const val STOP_LISTENING_AFTER_MILLIS = 5_000L

data class PersonListRow(
    val person: NoteMetadataEntity,
    val nextEvent: CalendarTaskEntity?,
    val eventCount: Int
)

class PeopleViewModel(
    private val peopleRepository: PeopleRepository,
    private val contactImporter: ContactImporter,
    private val appScope: CoroutineScope
) : ViewModel() {

    private val blankNewPersonNoteId = MutableStateFlow<String?>(null)

    val rows: StateFlow<List<PersonListRow>?> = combine(
        peopleRepository.observePeople(),
        peopleRepository.observeEventsWithEachPerson(),
        blankNewPersonNoteId
    ) { people, eventsByPerson, hiddenPersonNoteId ->
        people
            .filterNot { person -> person.noteId == hiddenPersonNoteId && person.title.isBlank() }
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

    val contactImportProgress: StateFlow<ContactImportProgress> = contactImporter.progress

    private val _selectedPersonNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedPersonNoteIds: StateFlow<Set<String>> = _selectedPersonNoteIds.asStateFlow()

    fun createPerson(name: String, onCreated: (personNoteId: String) -> Unit) {
        viewModelScope.launch {
            try {
                val personNoteId = peopleRepository.createPerson(name)
                if (name.isBlank()) blankNewPersonNoteId.value = personNoteId
                onCreated(personNoteId)
            } catch (e: Exception) {
                e.printStackTrace()
                showNativeToast(getString(Res.string.people_could_not_create_person))
            }
        }
    }

    fun importContactFiles(fileContents: List<ByteArray>) {
        if (!contactImporter.startImportFromFiles(fileContents)) showAlreadyImportingToast()
    }

    fun importContacts(contacts: List<ContactToImport>) {
        if (contacts.isEmpty()) return
        if (!contactImporter.startImport(contacts)) showAlreadyImportingToast()
    }

    private fun showAlreadyImportingToast() {
        viewModelScope.launch { showNativeToast(getString(Res.string.people_already_importing_contacts)) }
    }

    fun takeFinishedImportResult(): ContactImportResult? = contactImporter.takeFinishedResult()

    fun togglePersonSelection(personNoteId: String) {
        _selectedPersonNoteIds.update { selected -> if (personNoteId in selected) selected - personNoteId else selected + personNoteId }
    }

    fun selectPeople(personNoteIds: Set<String>) {
        _selectedPersonNoteIds.value = personNoteIds
    }

    fun clearSelection() {
        _selectedPersonNoteIds.value = emptySet()
    }

    fun moveSelectedToTrash() {
        val selectedIds = _selectedPersonNoteIds.value
        if (selectedIds.isEmpty()) return
        val selectedPeople = rows.value.orEmpty().map { row -> row.person }.filter { person -> person.noteId in selectedIds }
        clearSelection()
        viewModelScope.launch {
            val trashedCount = appScope.async {
                selectedPeople.count { person ->
                    try {
                        peopleRepository.moveToTrash(person.noteId)
                        true
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                }
            }.await()
            showNativeToast(
                when {
                    trashedCount == 0 -> COULD_NOT_MOVE_TO_TRASH_MESSAGE
                    selectedPeople.size == 1 -> movedToTrashMessage(selectedPeople.first().title)
                    trashedCount == selectedPeople.size -> getPluralString(Res.plurals.people_moved_people_to_trash, trashedCount, trashedCount)
                    else -> getPluralString(Res.plurals.people_moved_some_people_to_trash, selectedPeople.size, trashedCount, selectedPeople.size)
                }
            )
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
            } finally {
                blankNewPersonNoteId.compareAndSet(personNoteId, null)
            }
        }
    }
}
