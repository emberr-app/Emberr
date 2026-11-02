package com.emberr.domain.people

import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.PropertyBlock

fun personDisplayName(title: String): String = title.trim().ifEmpty { "Untitled" }

fun List<NoteMetadataEntity>.personNamesById(): Map<String, String> =
    filter { note -> note.kind == NoteKind.PERSON && note.trashedAt == null }
        .associate { person -> person.noteId to personDisplayName(person.title) }

suspend fun personNamesLinkedIn(blocks: List<NoteBlock>, findNote: suspend (noteId: String) -> NoteMetadataEntity?): Map<String, String> =
    blocks.filterIsInstance<PropertyBlock>()
        .flatMap { it.personNoteIds }
        .distinct()
        .mapNotNull { personNoteId -> findNote(personNoteId) }
        .personNamesById()
