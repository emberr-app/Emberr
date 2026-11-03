package com.emberr.presentation.calendar

import com.emberr.data.local.room.entity.TaskSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

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

    @Test
    fun movingAnEventKeepsItsDurationAndStopsAtTheEdgesOfTheDay() {
        val tenToEleven = EventTimeRange(startMinuteOfDay = 10 * 60, durationMinutes = 60)

        assertEquals(
            EventTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 60),
            moveEventTimeRange(tenToEleven, draggedMinutes = 2000f)
        )
        assertEquals(
            EventTimeRange(startMinuteOfDay = 0, durationMinutes = 60),
            moveEventTimeRange(tenToEleven, draggedMinutes = -2000f)
        )
    }

    @Test
    fun anOvernightEventIsSplitIntoTheEveningOfItsFirstDayAndTheMorningOfTheNextDay() {
        val elevenPmToSixAm = CalendarEvent(
            blockId = "sleep",
            noteId = "2026-11-02",
            text = "Night shift",
            isChecked = false,
            dateString = "2026-11-02",
            occurrenceDateString = "2026-11-02",
            reminderTimestamp = timestampOnDate("2026-11-02", 23 * 60),
            categoryId = null,
            durationMinutes = 7 * 60,
            sourceType = TaskSource.DAILY,
            url = null,
            description = null,
            recurrenceRule = null,
            allDay = null
        )

        val firstDay = LocalDate.parse("2026-11-02")
        val nextDay = LocalDate.parse("2026-11-03")

        assertTrue(elevenPmToSixAm.crossesMidnight())
        assertEquals(
            EventTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 60),
            elevenPmToSixAm.timeRange().partOnDay(elevenPmToSixAm.daysAfterStart(firstDay))
        )
        assertTrue(elevenPmToSixAm.continuesInto(nextDay))
        assertEquals(
            EventTimeRange(startMinuteOfDay = 0, durationMinutes = 6 * 60),
            elevenPmToSixAm.timeRange().partOnDay(elevenPmToSixAm.daysAfterStart(nextDay))
        )
        assertFalse(elevenPmToSixAm.continuesInto(LocalDate.parse("2026-11-04")))
    }

    @Test
    fun draggingAnOvernightEventKeepsItsStartWhenResizingTheEndAndCarriesItIntoTheNextDayWhenMoving() {
        val elevenPmToSixAm = EventTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 7 * 60)

        assertEquals(
            EventTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 8 * 60),
            resizeOvernightEventEnd(elevenPmToSixAm, draggedMinutes = 61f)
        )
        assertEquals(
            EventTimeRange(startMinuteOfDay = 23 * 60, durationMinutes = 60 + MinimumEventDurationMinutes),
            resizeOvernightEventEnd(elevenPmToSixAm, draggedMinutes = -2000f)
        )
        assertEquals(
            MovedOvernightEvent(daysLater = 1, timeRange = EventTimeRange(startMinuteOfDay = 60, durationMinutes = 7 * 60)),
            moveOvernightEvent(elevenPmToSixAm, draggedMinutes = 120f)
        )
    }
}
