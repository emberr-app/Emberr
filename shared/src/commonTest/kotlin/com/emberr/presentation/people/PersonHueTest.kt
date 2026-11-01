package com.emberr.presentation.people

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonHueTest {

    @Test
    fun theSamePersonAlwaysGetsTheSameHueAndEveryHueIsOnTheColorWheel() {
        val personNoteIds = listOf(
            "",
            "7f3c2a10-9b1e-4c55-8d2f-1a2b3c4d5e6f",
            "e2b7c9d4-0000-4fff-bfff-ffffffffffff",
            "a".repeat(500)
        )

        personNoteIds.forEach { personNoteId ->
            assertEquals(personHue(personNoteId), personHue(personNoteId))
            assertTrue(personHue(personNoteId) in 0 until 360, "hue ${personHue(personNoteId)} for \"$personNoteId\"")
        }
        assertEquals(111, personHue("7f3c2a10-9b1e-4c55-8d2f-1a2b3c4d5e6f"))
    }
}
