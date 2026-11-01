package com.emberr.domain.people

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PersonBirthdayTest {

    @Test
    fun aBirthdayWithAYearGivesMonthDayAndYear() {
        assertEquals(PersonBirthday(month = 3, day = 14, year = 1996), readPersonBirthday("1996-03-14"))
    }

    @Test
    fun aBirthdayWithoutAYearGivesMonthAndDayOnly() {
        assertEquals(PersonBirthday(month = 2, day = 29, year = null), readPersonBirthday("--02-29"))
        assertNull(readPersonBirthday("--02-30"))
    }
}
