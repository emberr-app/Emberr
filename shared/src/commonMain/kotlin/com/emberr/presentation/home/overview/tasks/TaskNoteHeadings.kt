package com.emberr.presentation.home.overview.tasks

import com.emberr.data.local.room.entity.NoteMetadataEntity

fun taskHeadingsByNoteId(notes: List<NoteMetadataEntity>, spaceNamesById: Map<String, String>): Map<String, String> {
    val noteCountByTitle = notes.groupingBy { it.title.trim().lowercase() }.eachCount()
    return notes.associate { note ->
        val title = note.title.trim().ifBlank { "Note" }
        val spaceName = spaceNamesById[note.spaceId]
        val sharesTitleWithAnotherNote = (noteCountByTitle[note.title.trim().lowercase()] ?: 0) > 1
        note.noteId to if (sharesTitleWithAnotherNote && spaceName != null) "$title · $spaceName" else title
    }
}
