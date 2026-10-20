package com.emberr.presentation.calendar

import kotlin.test.Test
import kotlin.test.assertEquals

class EventResizeTest {

    private val tenToTenThirty = EventTimeRange(startMinuteOfDay = 10 * 60, durationMinutes = 30)

    @Test
    fun draggingTheBottomEdgeSnapsToFiveMinutesAndNeverGoesBelowFifteenMinutes() {
        assertEquals(
            EventTimeRange(startMinuteOfDay = 10 * 60, durationMinutes = 40),
            resizeEventTimeRange(tenToTenThirty, EventResizeEdge.BOTTOM, draggedMinutes = 8.4f)
        )
        assertEquals(
            EventTimeRange(startMinuteOfDay = 10 * 60, durationMinutes = 15),
            resizeEventTimeRange(tenToTenThirty, EventResizeEdge.BOTTOM, draggedMinutes = -120f)
        )
    }

    @Test
    fun draggingTheTopEdgeKeepsTheEndTimeAndStopsAtMidnight() {
        assertEquals(
            EventTimeRange(startMinuteOfDay = 9 * 60 + 45, durationMinutes = 45),
            resizeEventTimeRange(tenToTenThirty, EventResizeEdge.TOP, draggedMinutes = -14f)
        )
        assertEquals(
            EventTimeRange(startMinuteOfDay = 0, durationMinutes = 10 * 60 + 30),
            resizeEventTimeRange(tenToTenThirty, EventResizeEdge.TOP, draggedMinutes = -2000f)
        )
    }

    @Test
    fun resizingAnEventThatRunsPastMidnightKeepsTheStartAValidTimeOfDay() {
        val elevenFiftyToTwelveTwenty = EventTimeRange(startMinuteOfDay = 23 * 60 + 50, durationMinutes = 30)

        val topDraggedDown = resizeEventTimeRange(elevenFiftyToTwelveTwenty, EventResizeEdge.TOP, draggedMinutes = 120f)
        val bottomDraggedUp = resizeEventTimeRange(elevenFiftyToTwelveTwenty, EventResizeEdge.BOTTOM, draggedMinutes = -120f)

        assertEquals(EventTimeRange(startMinuteOfDay = 23 * 60 + 45, durationMinutes = 35), topDraggedDown)
        assertEquals(EventTimeRange(startMinuteOfDay = 23 * 60 + 50, durationMinutes = 15), bottomDraggedUp)
        timestampOnDate("2026-10-20", topDraggedDown.startMinuteOfDay)
        timestampOnDate("2026-10-20", bottomDraggedUp.startMinuteOfDay)
    }
}
