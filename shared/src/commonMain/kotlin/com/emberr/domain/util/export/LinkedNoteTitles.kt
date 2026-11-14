package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock

suspend fun linkedNoteTitlesIn(blocks: List<NoteBlock>, findNote: suspend (noteId: String) -> NoteMetadataEntity?): Map<String, String> =
    blocks.filterIsInstance<LinkedNoteBlock>()
        .map { it.linkedNoteId }
        .distinct()
        .mapNotNull { noteId -> findNote(noteId)?.let { note -> noteId to note.title.ifBlank { UNTITLED_NOTE_TITLE } } }
        .toMap()
