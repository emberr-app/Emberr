// Loads a note from storage and turns its blocks into content the widget can draw.
package com.emberr.presentation.widget.note

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.domain.database.withDatabasesAsTables
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.repository.NoteRepository
import com.emberr.presentation.shared.components.loadNoteTitleNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.emberr.presentation.widget.WidgetLog

@Serializable
data class WidgetNoteContent(
    val title: String,
    val elements: List<WidgetElement>
)

class WidgetContentReader(
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val noteRepository: NoteRepository
) {
    private val blockJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun readNoteContentOnce(noteId: String): WidgetNoteContent? =
        withContext(Dispatchers.IO) {
            val blocks = noteRepository.getNoteContent(noteId)?.blocks
                ?: blockDao.getBlocksForNote(noteId).mapNotNull { entity -> decodeBlock(entity.blockDataJson) }
            buildContentFromBlocks(noteId, blocks)
        }

    suspend fun buildContentFromBlocks(noteId: String, blocks: List<NoteBlock>): WidgetNoteContent? =
        withContext(Dispatchers.Default) {
            val metadata = noteDao.getNoteById(noteId)
            if (metadata == null || metadata.trashedAt != null) return@withContext null

            val blocksWithTables = blocks.withDatabasesAsTables(noteRepository)
            WidgetNoteContent(
                title = loadNoteTitleNames().shown(metadata.title.trim()),
                elements = buildElementsFromBlocks(blocksWithTables, resolveLinkedNoteTitles(blocksWithTables))
            )
        }

    private suspend fun resolveLinkedNoteTitles(blocks: List<NoteBlock>): Map<String, String> {
        val linkedNoteIds = blocks.filterIsInstance<LinkedNoteBlock>().map { it.linkedNoteId }.distinct()
        if (linkedNoteIds.isEmpty()) return emptyMap()
        val noteTitleNames = loadNoteTitleNames()
        return noteDao.getNotesByIdsIncludingTemplates(linkedNoteIds)
            .associate { note -> note.noteId to noteTitleNames.shown(note.title.trim()) }
    }

    private fun decodeBlock(blockDataJson: String): NoteBlock? =
        try {
            blockJson.decodeFromString(NoteBlockSerializer, blockDataJson)
        } catch (cause: Exception) {
            WidgetLog.e("Skipped a block that could not be decoded", cause)
            null
        }
}
