package com.emberr.domain.backup.manual

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.ACCOUNT_TASKS_INBOX_NOTE_ID
import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasStrokeEntity
import com.emberr.data.local.room.entity.CategoryEntity
import com.emberr.data.local.room.entity.NoteBlockEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.database.holdsSharedSettings
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.LinkedNoteBlock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

const val IMPORTED_SPACE_NAME_SUFFIX = " (Imported)"

private val uuidInText = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")

class PreparedBackupImport(
    val spaces: EmberrBackupData,
    val accountData: AccountDataToImport
)

fun prepareBackupForImport(
    backup: EmberrBackupData,
    choice: BackupContentChoice,
    localCategoryIdByBackupId: Map<String, String>,
    now: Long,
    newId: () -> String
): PreparedBackupImport {
    val spaces = backup.spaces.filter { it.spaceId in choice.spaceIds && !it.isDeleted }
    val spaceIds = spaces.mapTo(HashSet()) { it.spaceId }
    val accountNotes = accountNotesIn(backup)
    val notes = backup.notes.filter { it.spaceId in spaceIds && it.trashedAt == null && it.noteId !in accountNotes.noteIds }
    val noteIds = notes.mapTo(HashSet()) { it.noteId }
    val blocks = backup.blocks
        .filter { it.noteId in noteIds && !it.isDeleted }
        .mapNotNull { block -> blockJsonOrNull(block.blockDataJson)?.let { blockJson -> block to blockJson } }
    val folders = backup.folders.filter { it.spaceId in spaceIds && !it.isDeleted }
    val folderIds = folders.mapTo(HashSet()) { it.folderId }
    val propertyTags = backup.propertyTags.filter { it.spaceId in spaceIds && !it.isDeleted }
    val customProperties = backup.customProperties.filter { it.spaceId in spaceIds && !it.isDeleted }

    val accountBlocksByNoteId = backup.blocks
        .filter { !it.isDeleted && it.noteId in accountNotes.noteIds }
        .mapNotNull { block -> blockJsonOrNull(block.blockDataJson)?.let { blockJson -> block to blockJson } }
        .sortedBy { (block, _) -> block.displayOrder }
        .groupBy { (block, _) -> block.noteId }
    fun accountBlocksOf(noteId: String, keepsBlock: (isCheckbox: Boolean) -> Boolean) =
        accountBlocksByNoteId[noteId].orEmpty().filter { (_, blockJson) -> keepsBlock(blockJson.isCheckbox()) }

    val notesToMerge = mutableListOf<NoteMetadataEntity>()
    val blocksToMerge = mutableListOf<Pair<NoteBlockEntity, JsonObject>>()
    fun addNoteToMerge(note: NoteMetadataEntity, noteBlocks: List<Pair<NoteBlockEntity, JsonObject>>) {
        if (noteBlocks.isEmpty()) return
        notesToMerge += note
        blocksToMerge += noteBlocks
    }
    for (dailyNote in accountNotes.dailyNotes) {
        val noteBlocks = if (dailyNote.dateString == PINNED_BLOCKS_DATE_STRING) {
            if (choice.includesDailyNotes) accountBlocksOf(dailyNote.noteId) { true } else emptyList()
        } else {
            accountBlocksOf(dailyNote.noteId) { isCheckbox -> if (isCheckbox) choice.includesCalendar else choice.includesDailyNotes }
        }
        addNoteToMerge(dailyNote, noteBlocks)
    }
    val tasksInbox = accountNotes.tasksInbox
    if (choice.includesCalendar && tasksInbox != null) addNoteToMerge(tasksInbox, accountBlocksOf(tasksInbox.noteId) { true })
    val subNotes = if (choice.includesDailyNotes) accountNotes.subNotes else emptyList()
    val subNoteIds = subNotes.mapTo(HashSet()) { it.noteId }
    val subNoteBlocks = subNotes.flatMap { subNote -> accountBlocksOf(subNote.noteId) { true } }

    val databaseIds = (blocks + blocksToMerge + subNoteBlocks).mapNotNull { (_, blockJson) -> ownDatabaseIdOf(blockJson) } +
            (notes + subNotes).mapNotNull { it.databaseId }
    val rewriter = IdRewriter(
        oldIds = spaceIds + noteIds + folderIds + propertyTags.map { it.tagId } + customProperties.map { it.propertyId } +
                databaseIds + subNoteIds + notesToMerge.map { it.noteId }.filter { it != ACCOUNT_TASKS_INBOX_NOTE_ID },
        newId = newId
    )

    val newBlockIdsByOldBlockId = HashMap<String, String>()
    fun importedBlock(block: NoteBlockEntity, blockJson: JsonObject): NoteBlockEntity {
        val newBlockId = rewriter.rewrite(block.blockId).takeIf { it != block.blockId } ?: newId()
        newBlockIdsByOldBlockId[block.blockId] = newBlockId
        val blockJsonWithNewId = JsonObject(blockJson + ("id" to JsonPrimitive(newBlockId)) - CATEGORY_ID_FIELD)
        val rewrittenBlockJson = Json.parseToJsonElement(rewriter.rewrite(blockJsonWithNewId.toString())).jsonObject
        return NoteBlockEntity(
            blockId = newBlockId,
            noteId = rewriter.rewrite(block.noteId),
            displayOrder = block.displayOrder,
            blockDataJson = rewrittenBlockJson.withCategoryIdPointedAtLocal(blockJson.categoryIdOrNull(), localCategoryIdByBackupId).toString(),
            updatedAt = block.updatedAt,
            isDeleted = false
        )
    }
    val importedSpaceBlocks = blocks.map { (block, blockJson) -> importedBlock(block, blockJson) }
    val importedBlocksToMerge = blocksToMerge.map { (block, blockJson) -> importedBlock(block, blockJson) }
    val importedSubNoteBlocks = subNoteBlocks.map { (block, blockJson) -> importedBlock(block, blockJson) }

    fun exceptionsFollowing(importedBlocks: List<NoteBlockEntity>): List<CalendarEventExceptionEntity> {
        val importedBlockIds = importedBlocks.mapTo(HashSet()) { it.blockId }
        return backup.calendarEventExceptions.mapNotNull { exception ->
            val newBlockId = newBlockIdsByOldBlockId[exception.blockId]?.takeIf { it in importedBlockIds } ?: return@mapNotNull null
            exception.copy(
                blockId = newBlockId,
                overrideCategoryId = exception.overrideCategoryId?.let { localCategoryIdByBackupId[it] },
                updatedAt = now
            )
        }
    }

    val importedSpaces = EmberrBackupData(
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
        blocks = importedSpaceBlocks,
        calendarEventExceptions = exceptionsFollowing(importedSpaceBlocks),
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

    val importedAccountData = AccountDataToImport(
        notesToMerge = notesToMerge.map { note ->
            note.copy(
                noteId = rewriter.rewrite(note.noteId),
                spaceId = ACCOUNT_SPACE_ID,
                folderId = null,
                updatedAt = now,
                selfHostSyncedAt = 0L,
                filePath = ""
            )
        },
        blocksToMerge = importedBlocksToMerge,
        subNotes = subNotes.map { subNote ->
            subNote.copy(
                noteId = rewriter.rewrite(subNote.noteId),
                spaceId = ACCOUNT_SPACE_ID,
                folderId = null,
                databaseId = subNote.databaseId?.let(rewriter::rewrite),
                updatedAt = now,
                selfHostSyncedAt = 0L
            )
        },
        subNoteBlocks = importedSubNoteBlocks,
        canvasNodes = backup.canvasNodes
            .filter { it.noteId in subNoteIds && !it.isDeleted }
            .map { node -> node.copy(noteId = rewriter.rewrite(node.noteId)) },
        canvasEdges = backup.canvasEdges
            .filter { it.noteId in subNoteIds && !it.isDeleted }
            .map { edge -> edge.copy(noteId = rewriter.rewrite(edge.noteId)) },
        canvasStrokes = backup.canvasStrokes
            .filter { it.noteId in subNoteIds && !it.isDeleted }
            .map { stroke -> stroke.copy(noteId = rewriter.rewrite(stroke.noteId)) },
        calendarEventExceptions = exceptionsFollowing(importedBlocksToMerge + importedSubNoteBlocks)
    )

    return PreparedBackupImport(importedSpaces, importedAccountData)
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

private const val CATEGORY_ID_FIELD = "categoryId"
private const val CHECKBOX_BLOCK_TYPE = "checkbox"

private fun JsonObject.isCheckbox(): Boolean = (this["type"] as? JsonPrimitive)?.contentOrNull == CHECKBOX_BLOCK_TYPE

private fun JsonObject.categoryIdOrNull(): String? = (this[CATEGORY_ID_FIELD] as? JsonPrimitive)?.contentOrNull

private fun JsonObject.withCategoryIdPointedAtLocal(backupCategoryId: String?, localCategoryIdByBackupId: Map<String, String>): JsonObject {
    if (backupCategoryId == null) return this
    val localCategoryId = localCategoryIdByBackupId[backupCategoryId] ?: backupCategoryId
    return JsonObject(this + (CATEGORY_ID_FIELD to JsonPrimitive(localCategoryId)))
}

class CategoriesToImport(
    val localCategoryIdByBackupId: Map<String, String>,
    val newCategories: List<CategoryEntity>
)

fun resolveCategoriesForImport(
    backupCategories: List<CategoryEntity>,
    localCategoriesIncludingDeleted: List<CategoryEntity>,
    now: Long,
    newId: () -> String
): CategoriesToImport {
    val oldestFirst = compareBy<CategoryEntity> { it.createdAt }.thenBy { it.categoryId }
    val localCategoryById = localCategoriesIncludingDeleted.associateBy { it.categoryId }
    val liveCategoryIdByName = localCategoriesIncludingDeleted
        .filter { !it.isDeleted }
        .groupBy { categoryNameKey(it.name) }
        .mapValuesTo(HashMap()) { (_, sameNamed) -> sameNamed.minWith(oldestFirst).categoryId }

    val localCategoryIdByBackupId = HashMap<String, String>()
    val newCategories = mutableListOf<CategoryEntity>()
    for (backupCategory in backupCategories.filter { !it.isDeleted }.sortedWith(oldestFirst)) {
        val localCategory = localCategoryById[backupCategory.categoryId]
        val nameKey = categoryNameKey(backupCategory.name)
        localCategoryIdByBackupId[backupCategory.categoryId] = when {
            localCategory != null && !localCategory.isDeleted -> localCategory.categoryId
            else -> liveCategoryIdByName[nameKey] ?: run {
                val categoryId = if (localCategory == null) backupCategory.categoryId else newId()
                newCategories += backupCategory.copy(categoryId = categoryId, spaceId = ACCOUNT_SPACE_ID, updatedAt = now)
                liveCategoryIdByName[nameKey] = categoryId
                categoryId
            }
        }
    }
    return CategoriesToImport(localCategoryIdByBackupId, newCategories)
}

private fun categoryNameKey(name: String): String = name.trim().lowercase()

class AccountDataToImport(
    val notesToMerge: List<NoteMetadataEntity>,
    val blocksToMerge: List<NoteBlockEntity>,
    val subNotes: List<NoteMetadataEntity>,
    val subNoteBlocks: List<NoteBlockEntity>,
    val canvasNodes: List<CanvasNodeEntity>,
    val canvasEdges: List<CanvasEdgeEntity>,
    val canvasStrokes: List<CanvasStrokeEntity>,
    val calendarEventExceptions: List<CalendarEventExceptionEntity>
)

class AccountContentInBackup(val hasDailyNotes: Boolean, val hasCalendar: Boolean)

fun accountContentIn(backup: EmberrBackupData): AccountContentInBackup {
    val accountNotes = accountNotesIn(backup)
    val pinnedBlocksNoteIds = accountNotes.dailyNotes.filter { it.dateString == PINNED_BLOCKS_DATE_STRING }.mapTo(HashSet()) { it.noteId }
    val datedDailyNoteIds = accountNotes.dailyNotes.mapTo(HashSet()) { it.noteId } - pinnedBlocksNoteIds
    var hasDailyNotes = accountNotes.subNotes.isNotEmpty()
    var hasCalendar = false
    for (block in backup.blocks) {
        if (hasDailyNotes && hasCalendar) break
        if (block.isDeleted) continue
        val isInTasksInbox = block.noteId == accountNotes.tasksInbox?.noteId
        if (block.noteId !in pinnedBlocksNoteIds && block.noteId !in datedDailyNoteIds && !isInTasksInbox) continue
        val blockJson = blockJsonOrNull(block.blockDataJson) ?: continue
        when {
            isInTasksInbox -> hasCalendar = true
            block.noteId in pinnedBlocksNoteIds -> hasDailyNotes = true
            blockJson.isCheckbox() -> hasCalendar = true
            else -> hasDailyNotes = true
        }
    }
    return AccountContentInBackup(hasDailyNotes, hasCalendar)
}

private class AccountNotesInBackup(
    val dailyNotes: List<NoteMetadataEntity>,
    val tasksInbox: NoteMetadataEntity?,
    val subNotes: List<NoteMetadataEntity>
) {
    val noteIds: Set<String> = (dailyNotes + listOfNotNull(tasksInbox) + subNotes).mapTo(HashSet()) { it.noteId }
}

private sealed interface SubNoteGroup {
    data class SingleNote(val noteId: String) : SubNoteGroup
    data class Database(val databaseId: String) : SubNoteGroup
}

private fun accountNotesIn(backup: EmberrBackupData): AccountNotesInBackup {
    val liveNotes = backup.notes.filter { it.trashedAt == null }
    val liveNoteById = liveNotes.associateBy { it.noteId }
    val dailyNotes = liveNotes
        .filter { it.isDaily && it.dateString != null }
        .sortedWith(compareBy<NoteMetadataEntity> { it.createdAt }.thenBy { it.noteId })
    val tasksInbox = liveNoteById[ACCOUNT_TASKS_INBOX_NOTE_ID]
    val accountNoteIds = (dailyNotes + listOfNotNull(tasksInbox) + liveNotes.filter { it.spaceId == ACCOUNT_SPACE_ID })
        .mapTo(HashSet()) { it.noteId }

    fun groupHolding(noteId: String): SubNoteGroup? {
        val note = liveNoteById[noteId] ?: return null
        return when {
            !note.isSubNote || note.kind == NoteKind.DATABASE -> null
            note.databaseId != null -> SubNoteGroup.Database(note.databaseId)
            else -> SubNoteGroup.SingleNote(noteId)
        }
    }

    val hostNoteIdsByGroup = backup.blocks
        .filter { !it.isDeleted && it.noteId in liveNoteById && SUB_NOTE_POINTER_FIELDS.any { field -> field in it.blockDataJson } }
        .mapNotNull { entity ->
            val group = when (val block = decodeBlockOrNull(entity.blockDataJson)?.takeUnless { it.isDeleted }) {
                is CanvasBlock -> groupHolding(block.canvasNoteId)
                is LinkedNoteBlock -> groupHolding(block.linkedNoteId)
                is DatabaseBlock -> if (block.holdsSharedSettings) null else SubNoteGroup.Database(block.databaseId)
                else -> null
            }
            group?.let { it to entity.noteId }
        }
        .groupBy({ it.first }, { it.second })

    fun membersOf(group: SubNoteGroup): List<NoteMetadataEntity> = when (group) {
        is SubNoteGroup.SingleNote -> listOfNotNull(liveNoteById[group.noteId])
        is SubNoteGroup.Database -> liveNotes.filter { it.noteId == databaseNoteId(group.databaseId) || it.databaseId == group.databaseId }
    }

    do {
        var foundAnotherGroup = false
        for ((group, hostNoteIds) in hostNoteIdsByGroup) {
            val members = membersOf(group)
            if (members.isEmpty() || members.any { !it.isSubNote }) continue
            val memberIds = members.mapTo(HashSet()) { it.noteId }
            if (memberIds.all { it in accountNoteIds }) continue
            if (hostNoteIds.none { it in accountNoteIds }) continue
            if (hostNoteIds.any { it !in memberIds && it !in accountNoteIds }) continue
            accountNoteIds += memberIds
            foundAnotherGroup = true
        }
    } while (foundAnotherGroup)

    return AccountNotesInBackup(
        dailyNotes = dailyNotes,
        tasksInbox = tasksInbox,
        subNotes = liveNotes.filter { it.noteId in accountNoteIds && !it.isDaily && it.noteId != ACCOUNT_TASKS_INBOX_NOTE_ID }
    )
}

private val SUB_NOTE_POINTER_FIELDS = listOf("canvasNoteId", "linkedNoteId", "databaseId")
