package com.emberr.domain.ai

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.model.*
import com.emberr.database.EmberrDatabase
import java.text.SimpleDateFormat
import java.util.*

private const val MAX_CHUNK_HEADER_CHARACTERS = 200

internal fun embeddingSafeChunkTexts(header: String, body: String): List<String> {
    val chunkHeader = header.take(MAX_CHUNK_HEADER_CHARACTERS)
    val bodyCharacterLimit = MAX_EMBEDDING_INPUT_CHARACTERS - chunkHeader.length - 1
    return splitIntoChunks(body, bodyCharacterLimit).map { chunk -> "$chunkHeader\n$chunk" }
}

private fun splitIntoChunks(text: String, characterLimit: Int): List<String> {
    if (text.length <= characterLimit) return listOf(text)

    val chunks = mutableListOf<String>()
    var startIndex = 0
    while (startIndex < text.length) {
        var endIndex = (startIndex + characterLimit).coerceAtMost(text.length)
        if (endIndex < text.length) {
            val paragraphBreak = text.lastIndexOf('\n', endIndex)
            val wordBreak = text.lastIndexOf(' ', endIndex)
            val breakPoint = maxOf(paragraphBreak, wordBreak)
            if (breakPoint > startIndex) endIndex = breakPoint
        }
        val chunk = text.substring(startIndex, endIndex).trim()
        if (chunk.isNotBlank()) chunks.add(chunk)
        startIndex = endIndex
    }
    return chunks
}

class NoteIndexer(
    lazyDatabase: Lazy<EmberrDatabase>,
    private val aiEngine: LocalAiEngine,
    private val settingsManager: SettingsManager
) {

    private val database by lazyDatabase

    private data class PendingIndex(
        val blockId: String,
        val chunkText: String,
        val embeddingString: String
    )

    private val dateOnlyFormatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val dateTimeFormatter = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
    private val isoInputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun isoToHuman(isoDate: String?): String? {
        if (isoDate == null) return null
        return try {
            val parsed = isoInputFormatter.parse(isoDate) ?: return isoDate
            dateOnlyFormatter.format(parsed)
        } catch (_: Exception) {
            isoDate
        }
    }

    private fun timestampToDateTime(millis: Long): String =
        dateTimeFormatter.format(Date(millis))

    private fun timestampToDate(millis: Long): String =
        dateOnlyFormatter.format(Date(millis))

    suspend fun indexNote(metadata: NoteMetadataEntity, content: NoteContent, rowLocation: DatabaseRowLocation? = null) {
        if (settingsManager.isAiFeaturesDisabled()) return
        if (aiEngine.unsupportedHardwareReason != null) return
        if (metadata.noteId == "global_pinned") return

        val baseContext = if (metadata.databaseId != null) databaseRowIndexContext(metadata, rowLocation) else buildContextString(metadata)

        val blockIds = mutableListOf<String>()
        val chunkTexts = mutableListOf<String>()

        for (block in content.blocks) {
            if (block.isDeleted) continue

            val blockText = extractTextFromBlock(
                block = block,
                allBlocks = content.blocks,
                isDaily = metadata.isDaily,
                noteDate = metadata.dateString
            )

            if (!blockText.isNullOrBlank()) {
                val subChunkTexts = embeddingSafeChunkTexts(baseContext, blockText)
                subChunkTexts.forEachIndexed { index, subChunkText ->
                    val chunkBlockId = if (subChunkTexts.size == 1) block.id else "${block.id}#$index"
                    blockIds.add(chunkBlockId)
                    chunkTexts.add(subChunkText)
                }
            }
        }

        replaceIndexForNote(metadata, blockIds, chunkTexts)
    }

    suspend fun indexCanvas(metadata: NoteMetadataEntity, canvas: CanvasContent, ownerNoteTitle: String?) {
        if (settingsManager.isAiFeaturesDisabled()) return
        if (aiEngine.unsupportedHardwareReason != null) return

        val context = buildString {
            appendLine("[Source: Canvas]")
            if (metadata.title.isNotBlank()) appendLine("Title: ${metadata.title}")
            if (!ownerNoteTitle.isNullOrBlank()) appendLine("Inside note: $ownerNoteTitle")
        }
        val canvasText = canvasTextForIndexing(canvas)
        val chunkTexts = if (canvasText.isBlank()) emptyList() else embeddingSafeChunkTexts(context, canvasText)
        val blockIds = chunkTexts.indices.map { index -> "canvas:${metadata.noteId}#$index" }
        replaceIndexForNote(metadata, blockIds, chunkTexts)
    }

    private suspend fun replaceIndexForNote(metadata: NoteMetadataEntity, blockIds: List<String>, chunkTexts: List<String>) {
        if (chunkTexts.isEmpty()) {
            database.transaction {
                database.vectorStoreQueries.deleteBlocksForNote(metadata.noteId)
            }
            return
        }

        val vectors = aiEngine.generateEmbeddings(chunkTexts)

        val pendingIndexes = chunkTexts.indices.map { i ->
            PendingIndex(
                blockId = blockIds[i],
                chunkText = chunkTexts[i],
                embeddingString = vectors[i].joinToString(prefix = "[", postfix = "]")
            )
        }

        database.transaction {
            database.vectorStoreQueries.deleteBlocksForNote(metadata.noteId)
            for (pending in pendingIndexes) {
                database.vectorStoreQueries.insertMetadata(
                    block_id = pending.blockId,
                    note_id = metadata.noteId,
                    space_id = metadata.spaceId,
                    chunk_text = pending.chunkText,
                    embedding = pending.embeddingString
                )
            }
        }
    }

    private fun buildContextString(metadata: NoteMetadataEntity): String {
        return buildString {
            if (metadata.isDaily) {
                appendLine("[Source: Daily Note]")
                // Convert "2026-06-12" → "June 12, 2026"
                appendLine("Date: ${isoToHuman(metadata.dateString) ?: metadata.dateString}")
            } else {
                appendLine("[Source: Note]")
                appendLine("Title: ${metadata.title}")
                if (metadata.snippet.isNotBlank()) {
                    appendLine("Description: ${metadata.snippet}")
                }
            }
        }
    }

    /**
     * Returns null for block types with no useful text (images, documents, voice).
     *
     * Checkbox deadline logic:
     * Any note  + reminder set  → full date+time from the timestamp (user set it explicitly)
     * Daily     + no reminder   → date-only from the note's own date (implied deadline)
     * Note  + no reminder  → "None"
     */
    private fun extractTextFromBlock(
        block: NoteBlock,
        allBlocks: List<NoteBlock>,
        isDaily: Boolean,
        noteDate: String?
    ): String? = when (block) {

        is TextBlock -> block.text.trim().ifBlank { null }

        is HeadingBlock -> when (block.level) {
            1 -> "Main Topic: ${block.text}"
            2 -> "Section: ${block.text}"
            else -> "Sub-section: ${block.text}"
        }

        is QuoteBlock -> "Quote: \"${block.text}\""

        is BulletedListBlock -> "• ${block.text}"
        is NumberedListBlock -> "${block.number}. ${block.text}"

        is ToggleBlock -> buildString {
            append("Toggle: ${block.text}")
            val children = allBlocks.filter { child ->
                !child.isDeleted &&
                        child.id != block.id &&
                        child.indentationLevel > block.indentationLevel
            }
            if (children.isNotEmpty()) {
                appendLine()
                children.forEach { child ->
                    val childText = extractTextFromBlock(child, allBlocks, isDaily, noteDate)
                    if (!childText.isNullOrBlank()) appendLine("  $childText")
                }
            }
        }

        is CalloutBlock -> block.text.trim().ifBlank { null }?.let { title -> "${block.calloutType.label}: $title" }

        is CodeBlock -> buildString {
            append("Code")
            if (block.language.isNotBlank() && block.language != "plaintext") {
                append(" (${block.language})")
            }
            append(":\n${block.code}")
        }

        is CheckboxBlock -> buildString {
            val status = if (block.isChecked) "Completed Task" else "Pending Task"
            append("$status: ${block.text}")

            val deadlineStr: String = when {
                block.reminderTimestamp != null -> {
                    "Deadline: ${timestampToDateTime(block.reminderTimestamp)}"
                }
                isDaily && noteDate != null -> {
                    "Deadline: ${isoToHuman(noteDate) ?: noteDate}"
                }
                else -> "Deadline: None"
            }
            append(" | $deadlineStr")

            if (block.isChecked && block.completedAt != null) {
                append(" | Completed on: ${timestampToDate(block.completedAt)}")
            }
        }

        is BookmarkBlock -> buildString {
            append("Bookmark: ${block.title ?: block.url}")
            if (!block.description.isNullOrBlank()) append("\nSummary: ${block.description}")
            append("\nURL: ${block.url}")
        }

        is TableBlock -> buildString {
            appendLine("Table:")
            block.rows.forEach { row ->
                val rowText = row.filter { it.isNotBlank() }.joinToString(" | ")
                if (rowText.isNotBlank()) appendLine("  - $rowText")
            }
        }.trim().ifBlank { null }

        is ImageBlock    -> null
        is DocumentBlock -> null
        is VoiceBlock    -> null
        is SolidDividerBlock    -> null
        is ThreeDotDividerBlock    -> null
        is LinkedNoteBlock -> null
        is CanvasBlock -> null
        is PropertyBlock -> block.valueAsText().takeIf { it.isNotBlank() }?.let { "${block.label}: $it" }
        is DatabaseBlock -> block.title.takeIf { it.isNotBlank() }?.let { "Database: $it" }
        is MovieBlock -> block.details?.let { details ->
            buildString {
                append("${details.kind.label}: ${block.summaryLine()}")
                if (details.genres.isNotEmpty()) append("\nGenres: ${details.genres.joinToString()}")
                if (details.creditNames.isNotEmpty()) append("\n${details.creditLabel}: ${details.creditNames.joinToString()}")
                if (details.overview.isNotBlank()) append("\nOverview: ${details.overview}")
            }
        }
        is WatchlistBlock -> block.title.takeIf { it.isNotBlank() }?.let { "Watchlist: $it" }
        is UnknownBlock -> null
    }

    fun deleteNoteFromIndex(noteId: String) {
        database.vectorStoreQueries.deleteBlocksForNote(noteId)
    }

    fun moveNoteToSpace(noteId: String, spaceId: String) {
        database.vectorStoreQueries.moveNoteToSpace(spaceId, noteId)
    }
}