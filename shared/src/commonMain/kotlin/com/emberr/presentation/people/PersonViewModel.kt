package com.emberr.presentation.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.people.EventsWithPerson
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.system.showNativeToast
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.people_could_not_remove_detail
import emberr.shared.generated.resources.people_could_not_save_detail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

private const val STOP_LISTENING_AFTER_MILLIS = 5_000L

class PersonViewModel(
    private val personNoteId: String,
    private val peopleRepository: PeopleRepository,
    noteRepository: NoteRepository,
    private val appScope: CoroutineScope
) : ViewModel() {

    val person: StateFlow<PersonWithDetails?> = peopleRepository.observePerson(personNoteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_LISTENING_AFTER_MILLIS), null)

    val events: StateFlow<EventsWithPerson?> = peopleRepository.observeEventsWithPerson(personNoteId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_LISTENING_AFTER_MILLIS), null)

    val everyone: StateFlow<List<NoteMetadataEntity>> = peopleRepository.observePeople()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_LISTENING_AFTER_MILLIS), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = noteRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_LISTENING_AFTER_MILLIS), emptyList())

    fun saveDetail(kind: PersonDetailKind, editedDetail: PersonDetailBlock?, label: String?, value: String) {
        changeDetails(Res.string.people_could_not_save_detail) {
            if (editedDetail == null) {
                peopleRepository.addDetail(personNoteId, kind, label, value)
            } else {
                peopleRepository.updateDetail(personNoteId, editedDetail.copy(label = label, value = value))
            }
        }
    }

    fun removeDetail(detail: PersonDetailBlock) {
        changeDetails(Res.string.people_could_not_remove_detail) { peopleRepository.removeDetail(personNoteId, detail.id) }
    }

    private fun changeDetails(failureMessage: StringResource, change: suspend () -> Unit) {
        appScope.launch {
            try {
                change()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                val message = getString(failureMessage)
                withContext(Dispatchers.Main) { showNativeToast(message) }
            }
        }
    }
}
