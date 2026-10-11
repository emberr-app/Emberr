package com.emberr.domain.backup.manual

import com.emberr.data.local.room.entity.NoteBlockEntity
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

const val IMPORTED_SPACE_NAME_SUFFIX = " (Imported)"

private val uuidInText = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

fun prepareSpacesForImport(
    backup: EmberrBackupData,
    spaceIdsToImport: Set<String>,
    now: Long,
    newId: () -> String
): EmberrBackupData {
    val spaces = backup.spaces.filter { it.spaceId in spaceIdsToImport && !it.isDeleted }
    val spaceIds = spaces.mapTo(HashSet()) { it.spaceId }
    val notes = backup.notes.filter { it.spaceId in spaceIds && it.trashedAt == null }
    val noteIds = notes.mapTo(HashSet()) { it.noteId }
    val blocks = backup.blocks
        .filter { it.noteId in noteIds && !it.isDeleted }
        .mapNotNull { block -> blockJsonOrNull(block.blockDataJson)?.let { blockJson -> block to blockJson } }
    val folders = backup.folders.filter { it.spaceId in spaceIds && !it.isDeleted }
    val folderIds = folders.mapTo(HashSet()) { it.folderId }
    val categories = backup.categories.filter { it.spaceId in spaceIds && !it.isDeleted }
    val categoryIds = categories.mapTo(HashSet()) { it.categoryId }
    val propertyTags = backup.propertyTags.filter { it.spaceId in spaceIds && !it.isDeleted }
    val customProperties = backup.customProperties.filter { it.spaceId in spaceIds && !it.isDeleted }
    val databaseIds = blocks.mapNotNull { (_, blockJson) -> ownDatabaseIdOf(blockJson) } + notes.mapNotNull { it.databaseId }

    val rewriter = IdRewriter(
        oldIds = spaceIds + noteIds + folderIds + categoryIds + propertyTags.map { it.tagId } +
                customProperties.map { it.propertyId } + databaseIds,
        newId = newId
    )

    val importedBlocks = blocks.map { (block, blockJson) ->
        val newBlockId = rewriter.rewrite(block.blockId).takeIf { it != block.blockId } ?: newId()
        val blockJsonWithNewId = JsonObject(blockJson + ("id" to JsonPrimitive(newBlockId)))
        block to NoteBlockEntity(
            blockId = newBlockId,
            noteId = rewriter.rewrite(block.noteId),
            displayOrder = block.displayOrder,
            blockDataJson = rewriter.rewrite(blockJsonWithNewId.toString()),
            updatedAt = block.updatedAt,
            isDeleted = false
        )
    }
    val newBlockIdsByOldBlockId = importedBlocks.associate { (oldBlock, newBlock) -> oldBlock.blockId to newBlock.blockId }

    return EmberrBackupData(
        version = backup.version,
        exportTimestamp = backup.exportTimestamp,
        spaces = spaces.map { space ->
            space.copy(
                spaceId = rewriter.rewrite(space.spaceId),
                displayName = space.displayName + IMPORTED_SPACE_NAME_SUFFIX,
                updatedAt = now
            )
        },
        notes = notes.map { note ->
            note.copy(
                noteId = rewriter.rewrite(note.noteId),
                folderId = note.folderId?.takeIf { it in folderIds }?.let(rewriter::rewrite),
                spaceId = rewriter.rewrite(note.spaceId),
                databaseId = note.databaseId?.let(rewriter::rewrite),
                updatedAt = now,
                selfHostSyncedAt = 0L
            )
        },
        folders = folders.map { folder ->
            folder.copy(
                folderId = rewriter.rewrite(folder.folderId),
                parentFolderId = folder.parentFolderId?.takeIf { it in folderIds }?.let(rewriter::rewrite),
                spaceId = rewriter.rewrite(folder.spaceId),
                updatedAt = now
            )
        },
        categories = categories.map { category ->
            category.copy(
                categoryId = rewriter.rewrite(category.categoryId),
                spaceId = rewriter.rewrite(category.spaceId),
                updatedAt = now
            )
        },
        propertyTags = propertyTags.map { tag ->
            tag.copy(
                tagId = rewriter.rewrite(tag.tagId),
                propertyKey = rewriter.rewrite(tag.propertyKey),
                spaceId = rewriter.rewrite(tag.spaceId),
                updatedAt = now
            )
        },
        customProperties = customProperties.map { property ->
            property.copy(
                propertyId = rewriter.rewrite(property.propertyId),
                spaceId = rewriter.rewrite(property.spaceId),
                updatedAt = now
            )
        },
        blocks = importedBlocks.map { (_, newBlock) -> newBlock },
        calendarEventExceptions = backup.calendarEventExceptions.mapNotNull { exception ->
            val newBlockId = newBlockIdsByOldBlockId[exception.blockId] ?: return@mapNotNull null
            exception.copy(
                blockId = newBlockId,
                overrideCategoryId = exception.overrideCategoryId?.takeIf { it in categoryIds }?.let(rewriter::rewrite),
                updatedAt = now
            )
        },
        canvasNodes = backup.canvasNodes
            .filter { it.noteId in noteIds && !it.isDeleted }
            .map { node -> node.copy(noteId = rewriter.rewrite(node.noteId)) },
        canvasEdges = backup.canvasEdges
            .filter { it.noteId in noteIds && !it.isDeleted }
            .map { edge -> edge.copy(noteId = rewriter.rewrite(edge.noteId)) },
        canvasStrokes = backup.canvasStrokes
            .filter { it.noteId in noteIds && !it.isDeleted }
            .map { stroke -> stroke.copy(noteId = rewriter.rewrite(stroke.noteId)) }
    )
}

private class IdRewriter(oldIds: Collection<String>, newId: () -> String) {

    private val newIdsByOldId = mutableMapOf<String, String>()
    private val oldIdPattern: Regex

    init {
        oldIds.flatMap { oldId -> uuidInText.findAll(oldId).map { it.value } }
            .forEach { uuid -> newIdsByOldId.getOrPut(uuid, newId) }

        val idsWithoutUuid = oldIds.filterNot { uuidInText.containsMatchIn(it) }.toSet()
        val baseIdsWithoutUuid = idsWithoutUuid.filter { id ->
            idsWithoutUuid.none { other -> other != id && wholeIdPattern(Regex.escape(other)).containsMatchIn(id) }
        }
        baseIdsWithoutUuid.forEach { baseId -> newIdsByOldId.getOrPut(baseId, newId) }

        val baseIdAlternatives = baseIdsWithoutUuid.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        oldIdPattern = if (baseIdAlternatives.isEmpty()) {
            uuidInText
        } else {
            Regex("${wholeIdPattern(baseIdAlternatives).pattern}|${uuidInText.pattern}")
        }
    }

    private fun wholeIdPattern(alternatives: String): Regex =
        Regex("(?<![^\"\\-_:])(?:$alternatives)(?![^\"\\-_:])")

    fun rewrite(text: String): String =
        oldIdPattern.replace(text) { match -> newIdsByOldId[match.value] ?: match.value }
}

private fun blockJsonOrNull(blockDataJson: String): JsonObject? =
    try {
        Json.parseToJsonElement(blockDataJson).jsonObject
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

private fun ownDatabaseIdOf(blockJson: JsonObject): String? {
    val blockType = (blockJson["type"] as? JsonPrimitive)?.contentOrNull
    val isLinkedDatabase = (blockJson["isLinkedDatabase"] as? JsonPrimitive)?.booleanOrNull == true
    if (blockType != "database" || isLinkedDatabase) return null
    return (blockJson["databaseId"] as? JsonPrimitive)?.contentOrNull
}
