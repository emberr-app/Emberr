package com.emberr.domain.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

class OneTimeCodeTest {

    @Test
    fun plainKeyGivesTheRfcCodesWithSixDigits() {
        assertEquals(OneTimeCode(digits = "287082", secondsLeft = 1), oneTimeCodeFor(RFC_SECRET, 59_000L))
        assertEquals("081804", oneTimeCodeFor(RFC_SECRET.lowercase().chunked(4).joinToString(" "), 1_111_111_109_000L)?.digits)
    }

    @Test
    fun otpauthLinkWithEightDigitsGivesTheRfcCodes() {
        val link = "otpauth://totp/Example:alice@example.com?secret=$RFC_SECRET&issuer=Example&digits=8"

        assertEquals("94287082", oneTimeCodeFor(link, 59_000L)?.digits)
        assertEquals("07081804", oneTimeCodeFor(link, 1_111_111_109_000L)?.digits)
    }

    private companion object {
        const val RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
    }
}
