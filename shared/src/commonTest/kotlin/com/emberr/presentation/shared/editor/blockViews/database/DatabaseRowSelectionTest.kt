package com.emberr.presentation.shared.editor.blockViews.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabaseRowSelectionTest {

    @Test
    fun selectingARowStartsSelecting() {
        val selection = DatabaseRowSelection()

        selection.select("row-1")

        assertTrue(selection.isSelecting)
        assertEquals(setOf("row-1"), selection.selectedRowIds)
    }

    @Test
    fun unselectingEveryRowKeepsSelectingOn() {
        val selection = DatabaseRowSelection()
        selection.select("row-1")
        selection.select("row-2")

        selection.toggle("row-1")
        selection.toggle("row-2")

        assertTrue(selection.isSelecting)
        assertEquals(emptySet(), selection.selectedRowIds)
    }

    @Test
    fun unselectingAllFromTheHeaderKeepsSelectingOn() {
        val selection = DatabaseRowSelection()
        selection.selectAll(listOf("row-1", "row-2"))

        selection.unselectAll()

        assertTrue(selection.isSelecting)
        assertEquals(emptySet(), selection.selectedRowIds)
    }

    @Test
    fun rowsThatAreNoLongerShownAreUnselectedButSelectingStaysOn() {
        val selection = DatabaseRowSelection()
        selection.select("row-1")

        selection.keepOnly(listOf("row-2"))

        assertTrue(selection.isSelecting)
        assertEquals(emptySet(), selection.selectedRowIds)
    }

    @Test
    fun stoppingSelectingUnselectsEverythingAndTurnsSelectingOff() {
        val selection = DatabaseRowSelection()
        selection.select("row-1")

        selection.stopSelecting()

        assertFalse(selection.isSelecting)
        assertEquals(emptySet(), selection.selectedRowIds)
    }
}
