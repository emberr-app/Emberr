package com.emberr.presentation.shared.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

private const val DOT_COUNT = 3
private const val WAVE_DURATION_MS = 1200
private const val DELAY_BETWEEN_DOTS = 0.15f
private const val SHARE_OF_WAVE_SPENT_LIFTED = 0.4f
private val DOT_LIFT_HEIGHT = 3.dp

@Composable
fun WavingDots(modifier: Modifier = Modifier, style: TextStyle = LocalTextStyle.current) {
    val transition = rememberInfiniteTransition(label = "wavingDots")
    val wavePosition by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(WAVE_DURATION_MS, easing = LinearEasing)),
        label = "wavePosition"
    )

    Row(modifier = modifier) {
        repeat(DOT_COUNT) { dotIndex ->
            Text(
                text = ".",
                style = style,
                modifier = Modifier.offset {
                    val dotPosition = (wavePosition - dotIndex * DELAY_BETWEEN_DOTS).mod(1f)
                    val liftFraction = if (dotPosition < SHARE_OF_WAVE_SPENT_LIFTED) {
                        sin(dotPosition / SHARE_OF_WAVE_SPENT_LIFTED * PI).toFloat()
                    } else {
                        0f
                    }
                    IntOffset(x = 0, y = -(DOT_LIFT_HEIGHT.toPx() * liftFraction).toInt())
                }
            )
        }
    }
}
