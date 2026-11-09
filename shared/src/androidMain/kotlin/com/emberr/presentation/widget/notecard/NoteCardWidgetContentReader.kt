// Loads one note's title, icon and cover for the card widget to draw.
package com.emberr.presentation.widget.notecard

import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.presentation.widget.WidgetLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoteCardWidgetContentReader(
    private val noteDao: NoteDao,
    private val mediaStorageHelper: MediaStorageHelper
) {

    suspend fun readContentOnce(noteId: String): NoteCardWidgetContent? =
        withContext(Dispatchers.IO) {
            val note = try {
                noteDao.getNoteById(noteId)
            } catch (cause: Exception) {
                WidgetLog.e("Could not read the note behind the card", cause)
                return@withContext null
            }
            if (note == null || note.trashedAt != null) null else buildContent(note)
        }

    private fun buildContent(note: NoteMetadataEntity): NoteCardWidgetContent {
        val isCanvas = note.kind == NoteKind.CANVAS
        return NoteCardWidgetContent(
            noteId = note.noteId,
            title = note.title.trim().ifBlank { "Untitled" },
            icon = note.icon?.trim()?.takeIf { it.isNotEmpty() },
            isFavorite = note.isFavorite,
            isCanvas = isCanvas,
            coverImageAbsolutePath = note.coverImagePath?.let { path -> mediaStorageHelper.getAbsoluteMediaPath(path) }
        )
    }
}
