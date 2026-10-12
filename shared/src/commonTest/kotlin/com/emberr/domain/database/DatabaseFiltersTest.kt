package com.emberr.domain.database

import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseDateRange
import com.emberr.domain.model.DatabaseFilter
import com.emberr.domain.model.DatabaseFilterCondition
import com.emberr.domain.model.DatabaseRelativeDate
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.DatabaseViewType
import com.emberr.domain.model.DEFAULT_VIEW_ID
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.columnKey
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseFiltersTest {

    @Test
    fun aNewFilterStartsWithTheFirstConditionForItsColumn() {
        assertEquals(DatabaseFilterCondition.CONTAINS, newDatabaseFilter("f1", DatabaseColumnTarget.NotesTitle, PropertyValueType.TEXT).condition)
        assertEquals(
            DatabaseFilterCondition.IS,
            newDatabaseFilter("f2", DatabaseColumnTarget.Property(PropertyType.DUE_DATE), PropertyValueType.DATE).condition
        )
        assertEquals(
            DatabaseFilterCondition.IS,
            newDatabaseFilter("f3", DatabaseColumnTarget.CustomProperty("client-id"), PropertyValueType.SINGLE_CHOICE).condition
        )
    }

    private val rowNoteId = "row-1"
    private val now = 100L
    private val today = LocalDate(2026, 9, 30)
    private val nameColumn = DatabaseColumnTarget.Property(PropertyType.NAME)
    private val statusColumn = DatabaseColumnTarget.Property(PropertyType.STATUS)
    private val tagsColumn = DatabaseColumnTarget.Property(PropertyType.TAGS)
    private val dueDateColumn = DatabaseColumnTarget.Property(PropertyType.DUE_DATE)
    private val checkboxColumn = DatabaseColumnTarget.Property(PropertyType.CHECKBOX)
    private val numberColumn = DatabaseColumnTarget.Property(PropertyType.NUMBER)

    private fun databaseFilteredBy(vararg filters: DatabaseFilter) = DatabaseBlock(
        id = "block-1",
        databaseId = "database-1",
        columns = listOf(nameColumn, statusColumn, tagsColumn, dueDateColumn, checkboxColumn, numberColumn),
        views = listOf(DatabaseView(id = DEFAULT_VIEW_ID, name = "Table", type = DatabaseViewType.TABLE, filters = filters.toList()))
    )

    private fun filter(
        target: DatabaseColumnTarget,
        condition: DatabaseFilterCondition,
        text: String = "",
        date: LocalDate? = null,
        relativeDate: DatabaseRelativeDate? = null,
        dateRange: DatabaseDateRange? = null,
        tagName: String? = null
    ) = DatabaseFilter(
        id = "${target.columnKey}-${condition.name}",
        target = target,
        condition = condition,
        text = text,
        date = date,
        relativeDate = relativeDate,
        dateRange = dateRange,
        tagName = tagName
    )

    private fun newRowIn(database: DatabaseBlock, startingBlocks: List<NoteBlock> = database.newRowBlocks(rowNoteId, now)): DatabaseRow {
        val blocks = startingBlocks.withCellsMatchingFilters(database, rowNoteId, now, today)
        return buildDatabaseRow(rowNoteId, database.titleMatchingFilters(""), createdAt = now, blocks = blocks)
    }

    private fun assertNewRowIsShown(database: DatabaseBlock, row: DatabaseRow) {
        assertEquals(listOf(rowNoteId), applyFiltersAndSort(listOf(row), database, today).map { it.noteId })
    }

    @Test
    fun aNewRowGetsTheValuesThatMatchEachFilterSoItStaysShown() {
        val database = databaseFilteredBy(
            filter(nameColumn, DatabaseFilterCondition.CONTAINS, text = " Report "),
            filter(statusColumn, DatabaseFilterCondition.IS, tagName = "Doing"),
            filter(tagsColumn, DatabaseFilterCondition.CONTAINS, tagName = "Work"),
            filter(dueDateColumn, DatabaseFilterCondition.IS, relativeDate = DatabaseRelativeDate.TOMORROW),
            filter(checkboxColumn, DatabaseFilterCondition.IS_CHECKED),
            filter(numberColumn, DatabaseFilterCondition.IS, text = "5")
        )

        val row = newRowIn(database)

        assertEquals("Report", row.cell(nameColumn)?.text)
        assertEquals(listOf("Doing"), row.cell(statusColumn)?.tags)
        assertEquals(listOf("Work"), row.cell(tagsColumn)?.tags)
        assertEquals(LocalDate(2026, 10, 1), row.cell(dueDateColumn)?.date)
        assertEquals(true, row.cell(checkboxColumn)?.isChecked)
        assertEquals("5", row.cell(numberColumn)?.text)
        assertNewRowIsShown(database, row)
    }

    @Test
    fun aNewRowGetsAValueJustPastAStrictBoundary() {
        val database = databaseFilteredBy(
            filter(dueDateColumn, DatabaseFilterCondition.IS_AFTER, date = LocalDate(2026, 10, 5)),
            filter(numberColumn, DatabaseFilterCondition.IS_LESS_THAN, text = "3")
        )

        val row = newRowIn(database)

        assertEquals(LocalDate(2026, 10, 6), row.cell(dueDateColumn)?.date)
        assertEquals("2", row.cell(numberColumn)?.text)
        assertNewRowIsShown(database, row)
    }

    @Test
    fun aNewRowGetsTodayForADateRangeThatIncludesTodayAndTheRangeStartOtherwise() {
        val thisMonth = databaseFilteredBy(filter(dueDateColumn, DatabaseFilterCondition.IS_WITHIN, dateRange = DatabaseDateRange.THIS_MONTH))
        val nextWeek = databaseFilteredBy(filter(dueDateColumn, DatabaseFilterCondition.IS_WITHIN, dateRange = DatabaseDateRange.NEXT_7_DAYS))

        assertEquals(today, newRowIn(thisMonth).cell(dueDateColumn)?.date)
        assertEquals(today, newRowIn(nextWeek).cell(dueDateColumn)?.date)
        assertNewRowIsShown(thisMonth, newRowIn(thisMonth))
    }

    @Test
    fun aTemplateValueThatBreaksAFilterIsChangedAndOneThatFitsIsKept() {
        val database = databaseFilteredBy(
            filter(statusColumn, DatabaseFilterCondition.IS_NOT, tagName = "Done"),
            filter(tagsColumn, DatabaseFilterCondition.CONTAINS, tagName = "Work"),
            filter(nameColumn, DatabaseFilterCondition.IS_EMPTY),
            filter(dueDateColumn, DatabaseFilterCondition.IS_ON_OR_AFTER, relativeDate = DatabaseRelativeDate.TODAY)
        )
        val templateBlocks = listOf(
            PropertyBlock(id = databaseCellBlockId(nameColumn, rowNoteId), propertyType = PropertyType.NAME, text = "From template"),
            PropertyBlock(id = databaseCellBlockId(statusColumn, rowNoteId), propertyType = PropertyType.STATUS, tags = listOf("Done")),
            PropertyBlock(id = databaseCellBlockId(tagsColumn, rowNoteId), propertyType = PropertyType.TAGS, tags = listOf("Home")),
            PropertyBlock(id = databaseCellBlockId(dueDateColumn, rowNoteId), propertyType = PropertyType.DUE_DATE, date = LocalDate(2026, 12, 24))
        )

        val row = newRowIn(database, templateBlocks)

        assertEquals("", row.cell(nameColumn)?.text)
        assertEquals(emptyList(), row.cell(statusColumn)?.tags)
        assertEquals(listOf("Home", "Work"), row.cell(tagsColumn)?.tags)
        assertEquals(LocalDate(2026, 12, 24), row.cell(dueDateColumn)?.date)
        assertNewRowIsShown(database, row)
    }

    @Test
    fun aNewRowTitleGetsTheTextATitleFilterLooksFor() {
        val database = databaseFilteredBy(filter(DatabaseColumnTarget.NotesTitle, DatabaseFilterCondition.CONTAINS, text = "Weekly"))

        assertEquals("Weekly", database.titleMatchingFilters(""))
        assertEquals("Plan Weekly", database.titleMatchingFilters("Plan"))
        assertEquals("My weekly plan", database.titleMatchingFilters("My weekly plan"))
        assertNewRowIsShown(database, newRowIn(database))
    }

    @Test
    fun filtersWithoutAValueDoNotChangeANewRow() {
        val database = databaseFilteredBy(
            filter(statusColumn, DatabaseFilterCondition.IS),
            filter(numberColumn, DatabaseFilterCondition.IS_GREATER_THAN, text = "abc")
        )

        val row = newRowIn(database)

        assertTrue(row.cell(statusColumn)?.tags.orEmpty().isEmpty())
        assertEquals("", row.cell(numberColumn)?.text)
    }
}
