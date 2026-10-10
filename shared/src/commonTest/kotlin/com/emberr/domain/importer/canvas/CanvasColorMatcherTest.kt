package com.emberr.domain.importer.canvas

import com.emberr.ui.theme.HighlightColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CanvasColorMatcherTest {

    @Test
    fun obsidianPresetColorsBecomeTheirEmberrColor() {
        assertEquals(
            listOf("red", "orange", "yellow", "green", "blue", "purple"),
            listOf("1", "2", "3", "4", "5", "6").map { CanvasColorMatcher.colorNameFor(it) }
        )
    }

    @Test
    fun hexColorsBecomeTheEmberrColorWithTheNearestHue() {
        assertEquals("red", CanvasColorMatcher.colorNameFor("#FF0000"))
        assertEquals("orange", CanvasColorMatcher.colorNameFor("#FFA500"))
        assertEquals("yellow", CanvasColorMatcher.colorNameFor("#ffff00"))
        assertEquals("green", CanvasColorMatcher.colorNameFor("#00C000"))
        assertEquals("blue", CanvasColorMatcher.colorNameFor("#00FFFF"))
        assertEquals("purple", CanvasColorMatcher.colorNameFor("#800080"))
        assertEquals("pink", CanvasColorMatcher.colorNameFor("#FF69B4"))
        assertEquals("red", CanvasColorMatcher.colorNameFor("#F00"))
    }

    @Test
    fun colorsWithLittleSaturationBecomeGrey() {
        assertEquals("grey", CanvasColorMatcher.colorNameFor("#808080"))
        assertEquals("grey", CanvasColorMatcher.colorNameFor("#FFFFFF"))
        assertEquals("grey", CanvasColorMatcher.colorNameFor("#000000"))
    }

    @Test
    fun missingOrUnreadableColorsStayDefault() {
        assertNull(CanvasColorMatcher.colorNameFor(null))
        assertNull(CanvasColorMatcher.colorNameFor("7"))
        assertNull(CanvasColorMatcher.colorNameFor("#GGGGGG"))
        assertNull(CanvasColorMatcher.colorNameFor("red"))
    }

    @Test
    fun everyMatchedColorIsOneTheCanvasCanShow() {
        val emberrColorNames = HighlightColor.entries.map { it.storageName }.toSet()
        val matchedColorNames = listOf("1", "2", "3", "4", "5", "6", "#808080") +
            (0 until 360 step 5).map { hue -> hexForHue(hue) }
        matchedColorNames.forEach { color ->
            assertTrue(CanvasColorMatcher.colorNameFor(color) in emberrColorNames, "$color matched an unknown color")
        }
    }

    private fun hexForHue(hue: Int): String {
        val section = hue / 60
        val rising = (255 * (hue % 60) / 60)
        val falling = 255 - rising
        val (red, green, blue) = when (section) {
            0 -> Triple(255, rising, 0)
            1 -> Triple(falling, 255, 0)
            2 -> Triple(0, 255, rising)
            3 -> Triple(0, falling, 255)
            4 -> Triple(rising, 0, 255)
            else -> Triple(255, 0, falling)
        }
        return "#" + listOf(red, green, blue).joinToString("") { it.toString(16).padStart(2, '0') }
    }
}
