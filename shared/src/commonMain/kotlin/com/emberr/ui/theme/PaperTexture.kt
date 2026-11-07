package com.emberr.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import kotlin.random.Random

const val FULL_PAPER_TEXTURE_STRENGTH = 100

private const val NOISE_TILE_SIZE = 256
private const val NOISE_SEED = 7
private const val GRAIN_SIZE_DP = 0.75f
private const val STRONGEST_TEXTURE_ALPHA = 0.144f
private val NoiseSpeckAlphas = listOf(0.25f, 0.5f, 0.75f, 1f)
private val NoiseSpeckColors = listOf(Color.Black, Color.White)

val LocalPaperTextureStrength = staticCompositionLocalOf { 0 }

fun interface PaperGrainMover {
    fun moveGrainBy(delta: Offset)
}

val LocalPaperGrainMover = staticCompositionLocalOf { PaperGrainMover { } }

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

private fun grainScaleFor(density: Float): Float = (density * GRAIN_SIZE_DP).coerceAtLeast(1f)

private class GrainScrollFollower(private val tileSpan: Float) : NestedScrollConnection, PaperGrainMover {
    var offsetX by mutableFloatStateOf(0f)
        private set
    var offsetY by mutableFloatStateOf(0f)
        private set

    override fun moveGrainBy(delta: Offset) {
        offsetX = (offsetX + delta.x).mod(tileSpan)
        offsetY = (offsetY + delta.y).mod(tileSpan)
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        moveGrainBy(consumed)
        return Offset.Zero
    }
}

@Composable
fun PaperTextureBox(
    modifier: Modifier = Modifier,
    propagateMinConstraints: Boolean = false,
    content: @Composable () -> Unit
) {
    val textureStrength = LocalPaperTextureStrength.current.coerceAtMost(FULL_PAPER_TEXTURE_STRENGTH)
    val density = LocalDensity.current.density
    val grainScale = grainScaleFor(density)
    val tileSpan = NOISE_TILE_SIZE * grainScale
    val grainScrollFollower = remember(tileSpan) { GrainScrollFollower(tileSpan) }

    Box(modifier = modifier.nestedScroll(grainScrollFollower), propagateMinConstraints = propagateMinConstraints) {
        CompositionLocalProvider(LocalPaperGrainMover provides grainScrollFollower, content = content)
        if (textureStrength > 0) {
            val textureAlpha = textureStrength / FULL_PAPER_TEXTURE_STRENGTH.toFloat() * STRONGEST_TEXTURE_ALPHA
            Box(modifier = Modifier.matchParentSize().clipToBounds()) {
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = grainScrollFollower.offsetX
                            translationY = grainScrollFollower.offsetY
                        }
                        .drawBehind {
                            translate(left = -tileSpan, top = -tileSpan) {
                                scale(grainScale, pivot = Offset.Zero) {
                                    drawRect(
                                        brush = paperNoiseBrush,
                                        size = Size(
                                            (size.width + tileSpan * 2) / grainScale,
                                            (size.height + tileSpan * 2) / grainScale
                                        ),
                                        alpha = textureAlpha
                                    )
                                }
                            }
                        }
                )
            }
        }
    }
}
