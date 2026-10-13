package com.emberr.presentation.shared.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.emberr.ui.theme.LocalAppIsDark

private const val FRONT_GHOST_SIDE_INSET = 0.1f
private const val FRONT_GHOST_HEAD_TOP = 0.05f
private const val FRONT_GHOST_HEM_TOP = 0.86f
private const val FRONT_GHOST_HEM_BOTTOM = 0.98f
private const val FRONT_GHOST_HEM_BUMP_COUNT = 3
private const val FRONT_GHOST_EYE_HEIGHT = 0.42f
private const val FRONT_GHOST_EYE_SPACING = 0.13f
private const val FRONT_GHOST_EYE_RADIUS_X = 0.055f
private const val FRONT_GHOST_EYE_RADIUS_Y = 0.075f

@Composable
fun EmberrFrontGhost(modifier: Modifier = Modifier) {
    val bodyColor = if (LocalAppIsDark.current) Color.White else Color.Black
    val eyeColor = MaterialTheme.colorScheme.background
    val bodyPath = remember { Path() }
    var phase by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var previousFrameNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameNanos ->
                phase += (frameNanos - previousFrameNanos) / 1_000_000_000f
                previousFrameNanos = frameNanos
            }
        }
    }

    Canvas(modifier = modifier) {
        val ghostSize = size.minDimension
        val ghostTopLeft = Offset(x = (size.width - ghostSize) / 2f, y = (size.height - ghostSize) / 2f)

        fun pointAt(x: Float, y: Float) = ghostTopLeft + Offset(x, y) * ghostSize

        bodyPath.reset()
        val headLeft = pointAt(FRONT_GHOST_SIDE_INSET, 0f)
        val headRight = pointAt(1f - FRONT_GHOST_SIDE_INSET, 0f)
        val headDiameter = headRight.x - headLeft.x
        val headTop = pointAt(0f, FRONT_GHOST_HEAD_TOP).y
        bodyPath.arcTo(
            rect = Rect(offset = Offset(headLeft.x, headTop), size = Size(headDiameter, headDiameter)),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = true
        )

        val hemTop = pointAt(0f, FRONT_GHOST_HEM_TOP).y
        val hemBottom = pointAt(0f, FRONT_GHOST_HEM_BOTTOM).y
        bodyPath.lineTo(headRight.x, hemTop)
        val bumpWidth = headDiameter / FRONT_GHOST_HEM_BUMP_COUNT
        for (bumpIndex in 0 until FRONT_GHOST_HEM_BUMP_COUNT) {
            val bumpStartX = headRight.x - bumpWidth * bumpIndex
            val bumpEndX = bumpStartX - bumpWidth
            bodyPath.cubicTo(bumpStartX, hemBottom, bumpEndX, hemBottom, bumpEndX, hemTop)
        }
        bodyPath.close()
        drawPath(path = bodyPath, color = bodyColor)

        val openness = blinkOpenness(phase)
        val eyeSize = Size(
            width = FRONT_GHOST_EYE_RADIUS_X * 2f * ghostSize,
            height = FRONT_GHOST_EYE_RADIUS_Y * 2f * ghostSize * openness
        )
        for (eyeCenterX in listOf(0.5f - FRONT_GHOST_EYE_SPACING, 0.5f + FRONT_GHOST_EYE_SPACING)) {
            val eyeCenter = pointAt(eyeCenterX, FRONT_GHOST_EYE_HEIGHT)
            drawOval(
                color = eyeColor,
                topLeft = Offset(eyeCenter.x - eyeSize.width / 2f, eyeCenter.y - eyeSize.height / 2f),
                size = eyeSize
            )
        }
    }
}
