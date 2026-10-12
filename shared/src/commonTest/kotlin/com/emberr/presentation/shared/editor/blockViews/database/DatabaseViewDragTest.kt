package com.emberr.presentation.shared.editor.blockViews.database

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseViewDragTest {

    private val viewIds = listOf("table", "board", "gallery")
    private val pillSpans = mapOf(
        "table" to HeaderSpan(left = 0f, right = 80f),
        "board" to HeaderSpan(left = 82f, right = 162f),
        "gallery" to HeaderSpan(left = 164f, right = 264f)
    )

    @Test
    fun theDropGoesBeforeTheFirstPillWhoseMiddleIsRightOfThePointer() {
        assertEquals("table", viewToDropBefore(viewIds, pillSpans, pointerX = 10f))
        assertEquals("board", viewToDropBefore(viewIds, pillSpans, pointerX = 60f))
        assertEquals("gallery", viewToDropBefore(viewIds, pillSpans, pointerX = 200f))
        assertNull(viewToDropBefore(viewIds, pillSpans, pointerX = 230f))
    }

    @Test
    fun droppingAPillNextToItselfKeepsItInPlace() {
        assertTrue(dropKeepsViewInPlace(viewIds, draggedViewId = "board", beforeViewId = "board"))
        assertTrue(dropKeepsViewInPlace(viewIds, draggedViewId = "board", beforeViewId = "gallery"))
        assertTrue(dropKeepsViewInPlace(viewIds, draggedViewId = "gallery", beforeViewId = null))
    }

    @Test
    fun droppingAPillSomewhereElseMovesIt() {
        assertFalse(dropKeepsViewInPlace(viewIds, draggedViewId = "board", beforeViewId = "table"))
        assertFalse(dropKeepsViewInPlace(viewIds, draggedViewId = "board", beforeViewId = null))
        assertFalse(dropKeepsViewInPlace(viewIds, draggedViewId = "table", beforeViewId = "gallery"))
    }

    @Test
    fun theDraggedPillFollowsThePointer() {
        val dragState = DatabaseViewDragState()
        dragState.pillSpans.putAll(pillSpans)

        dragState.start("board", grabXInPill = 20f)
        dragState.moveTo("board", pointerXInPill = 120f)

        assertEquals(100f, dragState.offsetOf("board"))
        assertEquals(202f, dragState.pointerX)
        assertEquals(0f, dragState.offsetOf("table"))
    }

    @Test
    fun scrollingTheRowWhileDraggingMovesThePointerAndThePillWithIt() {
        val dragState = DatabaseViewDragState()
        dragState.pillSpans.putAll(pillSpans)
        dragState.start("board", grabXInPill = 20f)

        dragState.shiftPointerBy(30f)

        assertTrue(dragState.isDragging)
        assertEquals(132f, dragState.pointerX)
        assertEquals(30f, dragState.offsetOf("board"))
    }

    @Test
    fun afterResetNoPillIsDragged() {
        val dragState = DatabaseViewDragState()
        dragState.pillSpans.putAll(pillSpans)
        dragState.start("board", grabXInPill = 20f)

        dragState.reset()

        assertNull(dragState.draggedViewId)
        assertEquals(0f, dragState.offsetOf("board"))
    }
}
