package com.emberr.domain.importer.canvas

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val GREY_COLOR_NAME = "grey"
private const val LOWEST_COLORFUL_SATURATION = 0.15f
private const val FULL_CIRCLE_DEGREES = 360f
private val hexColorPattern = Regex("^#?([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$")

private val colorNamesByObsidianPreset = mapOf(
    "1" to "red",
    "2" to "orange",
    "3" to "yellow",
    "4" to "green",
    "5" to "blue",
    "6" to "purple"
)

private val colorNamesByHue = listOf(
    0f to "red",
    33f to "orange",
    54f to "yellow",
    70f to "lime",
    122f to "green",
    207f to "blue",
    280f to "purple",
    335f to "pink"
)

internal object CanvasColorMatcher {

    fun colorNameFor(obsidianColor: String?): String? {
        val color = obsidianColor?.trim() ?: return null
        colorNamesByObsidianPreset[color]?.let { return it }
        val hexDigits = hexColorPattern.matchEntire(color)?.groupValues?.get(1) ?: return null
        val fullHexDigits = if (hexDigits.length == 3) hexDigits.map { "$it$it" }.joinToString("") else hexDigits
        val red = fullHexDigits.substring(0, 2).toInt(16) / 255f
        val green = fullHexDigits.substring(2, 4).toInt(16) / 255f
        val blue = fullHexDigits.substring(4, 6).toInt(16) / 255f
        return nearestColorName(red, green, blue)
    }

    private fun nearestColorName(red: Float, green: Float, blue: Float): String {
        val brightest = max(red, max(green, blue))
        val darkest = min(red, min(green, blue))
        val spread = brightest - darkest
        val lightness = (brightest + darkest) / 2f
        val saturation = if (spread == 0f) 0f else spread / (1f - abs(2f * lightness - 1f))
        if (saturation < LOWEST_COLORFUL_SATURATION) return GREY_COLOR_NAME

        val hue = when (brightest) {
            red -> 60f * (((green - blue) / spread).mod(6f))
            green -> 60f * ((blue - red) / spread + 2f)
            else -> 60f * ((red - green) / spread + 4f)
        }
        return colorNamesByHue.minBy { (namedHue, _) -> hueDistance(hue, namedHue) }.second
    }

    private fun hueDistance(firstHue: Float, secondHue: Float): Float {
        val distance = abs(firstHue - secondHue).mod(FULL_CIRCLE_DEGREES)
        return min(distance, FULL_CIRCLE_DEGREES - distance)
    }
}
