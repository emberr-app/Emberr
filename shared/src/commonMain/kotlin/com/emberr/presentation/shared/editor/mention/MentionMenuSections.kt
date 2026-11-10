package com.emberr.presentation.shared.editor.mention

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.people.personDisplayName
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.doc_text
import emberr.shared.generated.resources.doc_text_plus
import emberr.shared.generated.resources.user2
import androidx.compose.runtime.Composable
import com.emberr.presentation.shared.editor.slash.SlashMenuIcon
import com.emberr.presentation.shared.editor.slash.SlashMenuItemData
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData
import emberr.shared.generated.resources.common_untitled
import emberr.shared.generated.resources.editor_create_new_note
import emberr.shared.generated.resources.editor_load_more
import emberr.shared.generated.resources.editor_mention_section_notes
import emberr.shared.generated.resources.editor_mention_section_people
import emberr.shared.generated.resources.editor_new_note_named
import org.jetbrains.compose.resources.stringResource

const val MENTION_RESULTS_PER_STEP = 5

data class MentionMenuLabels(
    val createNewNote: String,
    val untitled: String,
    val newNoteForQuery: String,
    val notes: String,
    val people: String,
    val loadMore: String
)

@Composable
fun mentionMenuLabels(query: String): MentionMenuLabels = MentionMenuLabels(
    createNewNote = stringResource(Res.string.editor_create_new_note),
    untitled = stringResource(Res.string.common_untitled),
    newNoteForQuery = stringResource(Res.string.editor_new_note_named, query),
    notes = stringResource(Res.string.editor_mention_section_notes),
    people = stringResource(Res.string.editor_mention_section_people),
    loadMore = stringResource(Res.string.editor_load_more)
)

fun mentionMenuSections(
    labels: MentionMenuLabels,
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
        add(SlashMenuItemData(labels.createNewNote, SlashMenuIcon.Drawable(Res.drawable.doc_text_plus), onCreateBlankNote))
        notes.take(shownNoteCount).forEach { note ->
            val icon = note.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Drawable(Res.drawable.doc_text)
            add(SlashMenuItemData(note.title.ifEmpty { labels.untitled }, icon) { onPick(note) })
        }
        if (notes.size > shownNoteCount) add(loadMoreEntry(labels.loadMore, onShowMoreNotes))
        if (query.isNotBlank()) {
            add(SlashMenuItemData(labels.newNoteForQuery, SlashMenuIcon.Vector(Icons.Default.Add)) { onCreateNote(query) })
        }
    }

    val personEntries = buildList {
        people.take(shownPeopleCount).forEach { person ->
            val icon = person.icon?.let { SlashMenuIcon.Label(it) } ?: SlashMenuIcon.Drawable(Res.drawable.user2)
            add(SlashMenuItemData(personDisplayName(person.title), icon) { onPick(person) })
        }
        if (people.size > shownPeopleCount) add(loadMoreEntry(labels.loadMore, onShowMorePeople))
    }

    return listOfNotNull(
        SlashMenuSectionData(labels.notes, noteEntries),
        personEntries.takeIf { it.isNotEmpty() }?.let { SlashMenuSectionData(labels.people, it) }
    )
}

private fun loadMoreEntry(label: String, onShowMore: () -> Unit): SlashMenuItemData =
    SlashMenuItemData(label, SlashMenuIcon.Vector(Icons.Default.Add), onShowMore)
