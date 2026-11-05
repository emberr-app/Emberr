package com.emberr.presentation.shared.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.people.personDisplayName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.doc_text_plus
import emberr.shared.generated.resources.user2
import com.emberr.presentation.shared.editor.slash.SlashMenuIcon
import com.emberr.presentation.shared.editor.slash.SlashMenuItemData
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData

const val MENTION_RESULTS_PER_STEP = 5

fun mentionMenuSections(
    query: String,
    mentionableNotes: List<NoteMetadataEntity>,
    shownNoteCount: Int,
    shownPeopleCount: Int,
    onShowMoreNotes: () -> Unit,
    onShowMorePeople: () -> Unit,
    onCreateBlankNote: () -> Unit,
    onCreateNote: (title: String) -> Unit,
    onPick: (NoteMetadataEntity) -> Unit
): List<SlashMenuSectionData> {
    val (people, notes) = mentionableNotes
        .filter { note -> note.title.contains(query, ignoreCase = true) }
        .partition { note -> note.kind == NoteKind.PERSON }

    val noteEntries = buildList {
        add(SlashMenuItemData("Create new note", SlashMenuIcon.Drawable(Res.drawable.doc_text_plus), onCreateBlankNote))
        notes.take(shownNoteCount).forEach { note ->
            val icon = note.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Drawable(Res.drawable.doc_text)
            add(SlashMenuItemData(note.title.ifEmpty { "Untitled" }, icon) { onPick(note) })
        }
        if (notes.size > shownNoteCount) add(loadMoreEntry(onShowMoreNotes))
        if (query.isNotBlank()) {
            add(SlashMenuItemData("New \"$query\" note", SlashMenuIcon.Vector(Icons.Default.Add)) { onCreateNote(query) })
        }
    }

    val personEntries = buildList {
        people.take(shownPeopleCount).forEach { person ->
            val icon = person.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Drawable(Res.drawable.user2)
            add(SlashMenuItemData(personDisplayName(person.title), icon) { onPick(person) })
        }
        if (people.size > shownPeopleCount) add(loadMoreEntry(onShowMorePeople))
    }

    return listOfNotNull(
        SlashMenuSectionData("Notes", noteEntries),
        personEntries.takeIf { it.isNotEmpty() }?.let { SlashMenuSectionData("People", it) }
    )
}

private fun loadMoreEntry(onShowMore: () -> Unit): SlashMenuItemData =
    SlashMenuItemData("Load more", SlashMenuIcon.Vector(Icons.Default.Add), onShowMore)
