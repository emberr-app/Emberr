package com.emberr.domain.util.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebImageAddressTest {

    @Test
    fun aWebAddressIsAcceptedWithoutSurroundingSpaces() {
        assertEquals("https://example.com/logo.png", webImageAddressOrNull("  https://example.com/logo.png \n"))
        assertEquals("HTTP://example.com/a.jpg", webImageAddressOrNull("HTTP://example.com/a.jpg"))
    }

    @Test
    fun textThatIsNotASingleWebAddressIsRefused() {
        assertNull(webImageAddressOrNull("example.com/logo.png"))
        assertNull(webImageAddressOrNull("https://example.com/a b.png"))
        assertNull(webImageAddressOrNull("file:///home/me/logo.png"))
        assertNull(webImageAddressOrNull(""))
    }
}
