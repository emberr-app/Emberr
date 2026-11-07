package com.emberr.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.random.Random

const val FULL_PAPER_TEXTURE_STRENGTH = 100

private const val NOISE_TILE_SIZE = 256
private const val NOISE_SEED = 7
private const val GRAIN_SIZE_DP = 0.75f
private const val STRONGEST_TEXTURE_ALPHA = 0.144f
private val NoiseSpeckAlphas = listOf(0.25f, 0.5f, 0.75f, 1f)
private val NoiseSpeckColors = listOf(Color.Black, Color.White)

val LocalPaperTextureStrength = staticCompositionLocalOf { 0 }

private val paperNoiseBrush: ShaderBrush by lazy {
    ShaderBrush(ImageShader(createPaperNoiseTile(), TileMode.Repeated, TileMode.Repeated))
}

private fun createPaperNoiseTile(): ImageBitmap {
    val tile = ImageBitmap(NOISE_TILE_SIZE, NOISE_TILE_SIZE)
    val random = Random(NOISE_SEED)
    val speckLooks = NoiseSpeckColors.flatMap { speckColor ->
        NoiseSpeckAlphas.map { speckAlpha -> speckColor.copy(alpha = speckAlpha) }
    }
    val specksByLook = speckLooks.associateWith { mutableListOf<Offset>() }

    for (x in 0 until NOISE_TILE_SIZE) {
        for (y in 0 until NOISE_TILE_SIZE) {
            val speckLook = speckLooks[random.nextInt(speckLooks.size)]
            specksByLook.getValue(speckLook).add(Offset(x + 0.5f, y + 0.5f))
        }
    }

    val tileCanvas = Canvas(tile)
    specksByLook.forEach { (speckLook, speckPositions) ->
        val speckPaint = Paint().apply {
            color = speckLook
            strokeWidth = 1f
            strokeCap = StrokeCap.Square
            isAntiAlias = false
        }
        tileCanvas.drawPoints(PointMode.Points, speckPositions, speckPaint)
    }
    return tile
}

@Composable
fun Modifier.paperTexture(): Modifier {
    val textureStrength = LocalPaperTextureStrength.current.coerceAtMost(FULL_PAPER_TEXTURE_STRENGTH)
    if (textureStrength <= 0) return this
    val textureAlpha = textureStrength / FULL_PAPER_TEXTURE_STRENGTH.toFloat() * STRONGEST_TEXTURE_ALPHA

    return drawWithContent {
        drawContent()
        val grainScale = (density * GRAIN_SIZE_DP).coerceAtLeast(1f)
        scale(grainScale, pivot = Offset.Zero) {
            drawRect(
                brush = paperNoiseBrush,
                size = Size(size.width / grainScale, size.height / grainScale),
                alpha = textureAlpha
            )
        }
    }
}
