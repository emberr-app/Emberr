package com.emberr.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.domain.repository.NoteRepository
import com.emberr.presentation.shared.components.couldNotRestoreMessage
import com.emberr.presentation.shared.components.restoredMessage
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Backs the Trash screen.
 * Observes all soft-deleted notes and handles restoring them to their original folders
 * or permanently wiping their data and associated files from the device.
 */
class TrashViewModel constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val trashedNotes = repository.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val parentTitlesByNoteId = trashedNotes
        .map { notes -> repository.parentTitlesOfSubNotes(notes) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun restoreNote(noteId: String) {
        val title = trashedNotes.value.firstOrNull { it.noteId == noteId }?.title.orEmpty()
        viewModelScope.launch {
            try {
                repository.restoreNote(noteId)
                showNativeToast(restoredMessage(title))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                showNativeToast(couldNotRestoreMessage())
            }
        }
    }

    fun permanentlyDelete(noteId: String, filePath: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId, filePath)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            val currentTrash = trashedNotes.value
            currentTrash.forEach { note ->
                repository.deleteNote(note.noteId, note.filePath)
            }
        }
    }
}