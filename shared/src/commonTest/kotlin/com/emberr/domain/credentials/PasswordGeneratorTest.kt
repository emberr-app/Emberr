package com.emberr.domain.credentials

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PasswordGeneratorTest {

    @Test
    fun passwordKeepsEveryMinimumAndGrowsWhenMinimumsNeedMoreRoomThanTheLength() {
        val options = PasswordGeneratorOptions(
            length = SHORTEST_GENERATED_PASSWORD,
            minimumNumbers = 4,
            minimumSymbols = 3,
            avoidsAmbiguousCharacters = true
        )

        repeat(50) { seed ->
            val random = Random(seed)
            val password = generatePassword(options) { bound -> random.nextInt(bound) }

            assertEquals(9, password.length)
            assertTrue(password.count { it.isDigit() } >= 4, password)
            assertTrue(password.count { !it.isLetterOrDigit() } >= 3, password)
            assertTrue(password.any { it.isUpperCase() } && password.any { it.isLowerCase() }, password)
            assertTrue(password.none { it in "IOl01" }, password)
        }
    }
}
