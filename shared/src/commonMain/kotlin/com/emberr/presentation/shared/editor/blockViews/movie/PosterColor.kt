package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImagePainter
import com.emberr.domain.model.MovieDetails
import com.emberr.ui.theme.LocalAppIsDark
import kotlin.math.abs

private const val SAMPLE_WIDTH = 20
private const val SAMPLE_HEIGHT = 30
private const val HUE_BUCKET_COUNT = 12
private const val HUE_BUCKET_DEGREES = 360f / HUE_BUCKET_COUNT
private const val MIN_AVERAGE_CHROMA = 0.04f
private const val MAX_STUB_SATURATION = 0.55f
private const val DARK_STUB_LIGHTNESS = 0.3f
private const val LIGHT_STUB_LIGHTNESS = 0.86f

internal data class PosterColor(val hue: Float, val saturation: Float)

private val foundPosterColors = mutableStateMapOf<String, PosterColor>()

internal fun posterColorKeyOf(filePath: String?, url: String?): String? = filePath ?: url

internal val MovieDetails.posterColorKey: String? get() = posterColorKeyOf(posterFilePath, posterUrl)

internal fun savedPosterColor(key: String?): PosterColor? = key?.let { foundPosterColors[it] }

internal expect fun samplePosterPixels(loadedImage: AsyncImagePainter.State.Success, width: Int, height: Int): IntArray?

internal fun savePosterColor(key: String?, loadedImage: AsyncImagePainter.State.Success) {
    if (key == null || key in foundPosterColors) return
    val pixels = samplePosterPixels(loadedImage, SAMPLE_WIDTH, SAMPLE_HEIGHT) ?: return
    foundPosterColors[key] = findPosterColor(pixels)
}

@Composable
internal fun PosterColor.stubColor(): Color = Color.hsl(
    hue = hue,
    saturation = saturation.coerceAtMost(MAX_STUB_SATURATION),
    lightness = if (LocalAppIsDark.current) DARK_STUB_LIGHTNESS else LIGHT_STUB_LIGHTNESS
)

private fun findPosterColor(pixels: IntArray): PosterColor {
    val bucketChroma = FloatArray(HUE_BUCKET_COUNT)
    val bucketHueSum = FloatArray(HUE_BUCKET_COUNT)
    val bucketSaturationSum = FloatArray(HUE_BUCKET_COUNT)
    var totalChroma = 0f
    for (pixel in pixels) {
        val color = Color(pixel)
        val brightest = maxOf(color.red, color.green, color.blue)
        val darkest = minOf(color.red, color.green, color.blue)
        val chroma = brightest - darkest
        if (chroma <= 0f) continue
        val lightness = (brightest + darkest) / 2f
        val saturation = chroma / (1f - abs(2f * lightness - 1f))
        val hue = hueOf(color, brightest, chroma)
        val bucket = (hue / HUE_BUCKET_DEGREES).toInt().coerceIn(0, HUE_BUCKET_COUNT - 1)
        bucketChroma[bucket] += chroma
        bucketHueSum[bucket] += hue * chroma
        bucketSaturationSum[bucket] += saturation * chroma
        totalChroma += chroma
    }

    if (totalChroma / pixels.size < MIN_AVERAGE_CHROMA) return PosterColor(hue = 0f, saturation = 0f)
    val strongestBucket = bucketChroma.indices.maxBy { bucketChroma[it] }
    val weight = bucketChroma[strongestBucket]
    return PosterColor(
        hue = bucketHueSum[strongestBucket] / weight,
        saturation = bucketSaturationSum[strongestBucket] / weight
    )
}

private fun hueOf(color: Color, brightest: Float, chroma: Float): Float {
    val hue = when (brightest) {
        color.red -> 60f * (((color.green - color.blue) / chroma).mod(6f))
        color.green -> 60f * ((color.blue - color.red) / chroma + 2f)
        else -> 60f * ((color.red - color.green) / chroma + 4f)
    }
    return hue.mod(360f)
}
