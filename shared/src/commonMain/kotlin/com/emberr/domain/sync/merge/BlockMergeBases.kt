package com.emberr.domain.sync.merge

import com.emberr.data.local.room.dao.BlockMergeBaseDao
import com.emberr.data.local.room.entity.BlockMergeBaseEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.editableTextOrNull
import kotlinx.serialization.json.Json

private val baseBlockJson = Json { ignoreUnknownKeys = true }

private const val MAX_BLOCK_IDS_PER_DELETE = 500

suspend fun BlockMergeBaseDao.basesByBlockId(noteId: String): Map<String, BlockMergeBaseEntity> =
    getBasesOfNote(noteId).associateBy { it.blockId }

suspend fun BlockMergeBaseDao.rememberBlocksBothSidesHave(noteId: String, blocks: List<NoteBlockEntity>) {
    val (deletedBlocks, liveBlocks) = blocks.filter { it.noteId == noteId }.partition { it.isDeleted }
    val textBlocks = liveBlocks.filter { holdsText(it.blockDataJson) }
    if (textBlocks.isNotEmpty()) {
        upsertBases(textBlocks.map { BlockMergeBaseEntity(noteId, it.blockId, it.blockDataJson, it.updatedAt) })
    }
    if (deletedBlocks.isEmpty()) return
    val savedBlockIds = getBasesOfNote(noteId).mapTo(HashSet()) { it.blockId }
    deletedBlocks.map { it.blockId }
        .filter { it in savedBlockIds }
        .chunked(MAX_BLOCK_IDS_PER_DELETE)
        .forEach { blockIds -> deleteBasesOfBlocks(noteId, blockIds) }
}

private fun holdsText(blockDataJson: String): Boolean =
    try {
        baseBlockJson.decodeFromString(NoteBlockSerializer, blockDataJson).editableTextOrNull() != null
    } catch (_: Exception) {
        false
    }
