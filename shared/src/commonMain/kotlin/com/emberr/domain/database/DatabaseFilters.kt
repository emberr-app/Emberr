package com.emberr.domain.database

import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseFilter
import com.emberr.domain.model.DatabaseFilterCondition
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyDateRange
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.numberOrNull
import com.emberr.domain.model.withDateRange
import com.emberr.domain.model.withStartDateMovedTo
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

fun newDatabaseFilter(id: String, target: DatabaseColumnTarget, valueType: PropertyValueType): DatabaseFilter =
    DatabaseFilter(id = id, target = target, condition = filterConditionsFor(valueType).first())

fun List<NoteBlock>.withCellsMatchingFilters(database: DatabaseBlock, rowNoteId: String, now: Long, today: LocalDate): List<NoteBlock> =
    database.usableFilters()
        .map { (filter, _) -> filter }
        .filter { it.target != DatabaseColumnTarget.NotesTitle }
        .fold(this) { blocks, filter ->
            val preset = DatabaseCellPreset(filter.target) { cell -> cell.withValueMatching(filter, today) }
            blocks.withCellPreset(database, preset, rowNoteId, now)
        }

fun DatabaseBlock.titleMatchingFilters(title: String): String =
    usableFilters()
        .map { (filter, _) -> filter }
        .filter { it.target == DatabaseColumnTarget.NotesTitle }
        .fold(title) { titleSoFar, filter ->
            if (filter.condition == DatabaseFilterCondition.IS_EMPTY) "" else titleSoFar.textMatching(filter)
        }

private fun PropertyBlock.withValueMatching(filter: DatabaseFilter, today: LocalDate): PropertyBlock = when {
    filter.condition == DatabaseFilterCondition.IS_EMPTY -> copy(text = "", tags = emptyList()).withDateRange(PropertyDateRange())
    filter.condition == DatabaseFilterCondition.IS_NOT_EMPTY -> this
    filter.condition == DatabaseFilterCondition.IS_CHECKED -> copy(isChecked = true)
    filter.condition == DatabaseFilterCondition.IS_UNCHECKED -> copy(isChecked = false)
    valueType.holdsDate -> withDateMatching(filter, today)
    valueType.holdsTags -> withTagsMatching(filter)
    valueType.holdsNumber -> withNumberMatching(filter)
    else -> copy(text = text.textMatching(filter))
}

private fun String.textMatching(filter: DatabaseFilter): String {
    if (textMatches(this, filter)) return this
    return when (filter.condition) {
        DatabaseFilterCondition.CONTAINS -> listOf(this, filter.text.trim()).filter { it.isNotBlank() }.joinToString(" ")
        DatabaseFilterCondition.DOES_NOT_CONTAIN -> ""
        else -> this
    }
}

private fun PropertyBlock.withDateMatching(filter: DatabaseFilter, today: LocalDate): PropertyBlock {
    if (dateMatches(date, filter, today)) return this
    val filterDate = filter.relativeDate?.dateFrom(today) ?: filter.date
    val matchingDate = when (filter.condition) {
        DatabaseFilterCondition.IS_WITHIN -> filter.dateRange?.daysFrom(today)?.let { range -> if (today in range) today else range.start }
        DatabaseFilterCondition.IS_BEFORE -> filterDate?.minus(1, DateTimeUnit.DAY)
        DatabaseFilterCondition.IS_AFTER -> filterDate?.plus(1, DateTimeUnit.DAY)
        else -> filterDate
    } ?: return this
    return withStartDateMovedTo(matchingDate)
}

private fun PropertyBlock.withTagsMatching(filter: DatabaseFilter): PropertyBlock {
    val tagName = filter.tagName ?: return this
    if (tagsMatch(tags, filter)) return this
    return when (filter.condition) {
        DatabaseFilterCondition.IS, DatabaseFilterCondition.CONTAINS ->
            copy(tags = if (valueType.allowsManyTags) tags + tagName else listOf(tagName))
        DatabaseFilterCondition.IS_NOT, DatabaseFilterCondition.DOES_NOT_CONTAIN ->
            copy(tags = tags.filterNot { it.equals(tagName, ignoreCase = true) })
        else -> this
    }
}

private fun PropertyBlock.withNumberMatching(filter: DatabaseFilter): PropertyBlock {
    val filterNumber = filter.text.trim().toDoubleOrNull() ?: return this
    if (numberMatches(numberOrNull(), filter)) return this
    return when (filter.condition) {
        DatabaseFilterCondition.IS -> copy(text = filter.text.trim())
        DatabaseFilterCondition.IS_NOT -> copy(text = "")
        DatabaseFilterCondition.IS_GREATER_THAN -> copy(text = formulaNumberText(filterNumber + 1))
        DatabaseFilterCondition.IS_LESS_THAN -> copy(text = formulaNumberText(filterNumber - 1))
        else -> this
    }
}
