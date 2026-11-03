package com.emberr.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.importer.TextFileDecoder
import com.emberr.domain.people.ContactToImport
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.contactPhotoExtensionAndMimeType
import com.emberr.domain.people.readContactsFromVCardText
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.system.COULD_NOT_MOVE_TO_TRASH_MESSAGE
import com.emberr.domain.util.system.movedToTrashMessage
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.home.note.NoteEditorViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val mediaStorageHelper: MediaStorageHelper,
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

    fun createPerson(name: String, onCreated: (personNoteId: String) -> Unit) {
        viewModelScope.launch {
            try {
                val personNoteId = peopleRepository.createPerson(name)
                if (name.isBlank()) blankNewPersonNoteId.value = personNoteId
                onCreated(personNoteId)
            } catch (e: Exception) {
                e.printStackTrace()
                showNativeToast("Couldn't create the person")
            }
        }
    }

    fun importContactFiles(fileContents: List<ByteArray>, onOnePersonImported: (personNoteId: String) -> Unit) {
        viewModelScope.launch {
            val contacts = withContext(Dispatchers.Default) {
                fileContents.flatMap { bytes -> readContactsFromVCardText(TextFileDecoder.decode(bytes)) }
            }
            if (contacts.isEmpty()) showNativeToast("No contacts found in the file") else importContacts(contacts, onOnePersonImported)
        }
    }

    fun importContacts(contacts: List<ContactToImport>, onOnePersonImported: (personNoteId: String) -> Unit) {
        if (contacts.isEmpty()) return
        viewModelScope.launch {
            val importedPersonNoteIds = mutableListOf<String>()
            var copiesSkipped = 0
            var failures = 0
            for (contact in contacts) {
                try {
                    val photoFileName = savePhotoOrNull(contact.photoBytes)
                    val personNoteId = peopleRepository.createPersonFromContactUnlessCopy(contact, photoFileName)
                    if (personNoteId != null) {
                        importedPersonNoteIds += personNoteId
                    } else {
                        copiesSkipped++
                        photoFileName?.let { mediaStorageHelper.deleteMediaFile(it) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failures++
                }
            }
            val addedOnlyOnePerson = importedPersonNoteIds.size == 1 && copiesSkipped == 0 && failures == 0
            if (addedOnlyOnePerson) {
                onOnePersonImported(importedPersonNoteIds.first())
            } else {
                showNativeToast(contactImportMessage(added = importedPersonNoteIds.size, copiesSkipped = copiesSkipped, failures = failures))
            }
        }
    }

    private fun contactImportMessage(added: Int, copiesSkipped: Int, failures: Int): String {
        if (added == 0 && failures > 0) return "Couldn't add the contacts"
        if (added == 0) return if (copiesSkipped == 1) "Already in People" else "All $copiesSkipped are already in People"
        val parts = mutableListOf(if (added == 1) "Added 1 person" else "Added $added people")
        if (copiesSkipped > 0) parts += "skipped $copiesSkipped already in People"
        if (failures > 0) parts += "couldn't add $failures"
        return parts.joinToString(", ")
    }

    private suspend fun savePhotoOrNull(photoBytes: ByteArray?): String? {
        if (photoBytes == null || photoBytes.isEmpty()) return null
        val (extension, mimeType) = contactPhotoExtensionAndMimeType(photoBytes)
        return mediaStorageHelper.saveBytesToInternalStorage(photoBytes, extension, mimeType)?.localFileName
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
