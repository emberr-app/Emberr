package com.emberr.domain.database

import androidx.compose.runtime.Immutable
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.valueAsText

@Immutable
data class DatabaseRow(
    val noteId: String,
    val title: String,
    val createdAt: Long,
    val cellsByColumn: Map<DatabaseColumnTarget, PropertyBlock>,
    val icon: String? = null,
    val coverImagePath: String? = null,
    val formulaResults: Map<DatabaseColumnTarget, FormulaValue> = emptyMap(),
    val personNamesById: Map<String, String> = emptyMap()
) {
    fun cell(column: DatabaseColumnTarget): PropertyBlock? = cellsByColumn[column]

    fun cellText(column: DatabaseColumnTarget): String = cell(column)?.valueAsText { personNamesById[it] }.orEmpty()

    fun livePersonNoteIdsAt(column: DatabaseColumnTarget): List<String> =
        cell(column)?.personNoteIds.orEmpty().filter { it in personNamesById }

    fun formulaResult(column: DatabaseColumnTarget): FormulaValue? = formulaResults[column]
}

fun buildDatabaseRow(
    noteId: String,
    title: String,
    createdAt: Long,
    blocks: List<NoteBlock>,
    icon: String? = null,
    coverImagePath: String? = null,
    personNamesById: Map<String, String> = emptyMap()
): DatabaseRow {
    val liveCellsByColumn = blocks.mapNotNull { block ->
        val cell = block as? PropertyBlock ?: return@mapNotNull null
        if (cell.isDeleted) return@mapNotNull null
        val column = cell.databaseColumn() ?: return@mapNotNull null
        if (cell.id != databaseCellBlockId(column, noteId)) return@mapNotNull null
        column to cell
    }.toMap()
    return DatabaseRow(
        noteId = noteId,
        title = title,
        createdAt = createdAt,
        cellsByColumn = liveCellsByColumn,
        icon = icon,
        coverImagePath = coverImagePath,
        personNamesById = personNamesById
    )
}

fun PropertyBlock.databaseColumn(): DatabaseColumnTarget? {
    val builtInType = propertyType
    val customId = customPropertyId
    return when {
        builtInType != null -> DatabaseColumnTarget.Property(builtInType)
        customId != null -> DatabaseColumnTarget.CustomProperty(customId)
        else -> null
    }
}
