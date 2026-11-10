// Supplies the list of notes a widget's setup screen offers for selection.
package com.emberr.presentation.widget

import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.presentation.shared.components.loadShownSpaceName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class SelectableWidgetNote(
    val note: NoteMetadataEntity,
    val spaceName: String,
    val spacePosition: Int
)

class WidgetNoteSource(
    private val noteDao: NoteDao,
    private val spaceDao: SpaceDao
) {

    fun observeSelectableNotes(): Flow<List<SelectableWidgetNote>> =
        combine(
            noteDao.getAllNotesAcrossSpacesFlow(),
            spaceDao.getAllSpaces()
        ) { notes, spaces ->
            val spaceNamesById = spaces.associate { space -> space.spaceId to loadShownSpaceName(space.displayName) }
            val spacePositionsById = spaces.withIndex().associate { (position, space) -> space.spaceId to position }
            notes.map { note ->
                SelectableWidgetNote(
                    note = note,
                    spaceName = spaceNamesById[note.spaceId].orEmpty(),
                    spacePosition = spacePositionsById[note.spaceId] ?: Int.MAX_VALUE
                )
            }
        }
            .flowOn(Dispatchers.IO)
            .catch { cause ->
                WidgetLog.e("Could not list notes for a widget setup screen", cause)
                emit(emptyList())
            }

    suspend fun isNoteDeleted(noteId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val note = noteDao.getNoteById(noteId)
                note == null || note.trashedAt != null
            } catch (cause: Exception) {
                WidgetLog.e("Could not check whether note $noteId was deleted", cause)
                false
            }
        }
}
