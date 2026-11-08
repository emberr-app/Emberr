package com.emberr.presentation.shared.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SliderHeight = 44.dp
private val RailHeight = 4.dp
private val ThumbHeight = 20.dp
private val ThumbWidth = 44.dp
private val RailShape = RoundedCornerShape(RailHeight / 2)
private val ThumbShape = RoundedCornerShape(ThumbHeight / 2)
private val ThumbGlowElevation = 6.dp
private const val THUMB_GLOW_ALPHA = 0.35f
private const val TRACK_ALPHA = 0.12f
private const val KEYBOARD_PAGE_STEPS = 10

@Composable
fun EmberrSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    valueLabel: ((Float) -> String)? = null,
    accentColor: Color = MaterialTheme.colorScheme.secondary,
    labelColor: Color = MaterialTheme.colorScheme.onSecondary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = TRACK_ALPHA)
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val density = LocalDensity.current
    val thumbWidthPx = with(density) { ThumbWidth.toPx() }
    val rangeSpan = valueRange.endInclusive - valueRange.start
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val currentValue by rememberUpdatedState(clampedValue)
    val shownValue by animateFloatAsState(
        targetValue = clampedValue,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "slider_value"
    )

    fun snapped(rawValue: Float): Float {
        val clamped = rawValue.coerceIn(valueRange.start, valueRange.endInclusive)
        if (steps <= 0 || rangeSpan == 0f) return clamped
        val stepSize = rangeSpan / (steps + 1)
        return valueRange.start + ((clamped - valueRange.start) / stepSize).roundToInt() * stepSize
    }

    fun changeTo(rawValue: Float) {
        val newValue = snapped(rawValue)
        if (newValue != currentValue) currentOnValueChange(newValue)
    }

    val keyboardStep = if (steps > 0) rangeSpan / (steps + 1) else rangeSpan / 100f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(SliderHeight)
            .progressSemantics(clampedValue, valueRange, steps)
            .semantics {
                setProgress { targetValue ->
                    changeTo(targetValue)
                    true
                }
            }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val target = when (event.key) {
                    Key.DirectionRight, Key.DirectionUp -> clampedValue + keyboardStep
                    Key.DirectionLeft, Key.DirectionDown -> clampedValue - keyboardStep
                    Key.PageUp -> clampedValue + keyboardStep * KEYBOARD_PAGE_STEPS
                    Key.PageDown -> clampedValue - keyboardStep * KEYBOARD_PAGE_STEPS
                    Key.MoveHome -> valueRange.start
                    Key.MoveEnd -> valueRange.endInclusive
                    else -> return@onKeyEvent false
                }
                changeTo(target)
                currentOnValueChangeFinished?.invoke()
                true
            }
    ) {
        val sliderWidthPx = constraints.maxWidth.toFloat()
        val travelPx = (sliderWidthPx - thumbWidthPx).coerceAtLeast(1f)

        fun valueAt(pointerX: Float): Float =
            valueRange.start + (pointerX - thumbWidthPx / 2) / travelPx * rangeSpan

        val pointerModifier = Modifier
            .pointerInput(valueRange, steps, travelPx) {
                detectTapGestures { position ->
                    changeTo(valueAt(position.x))
                    currentOnValueChangeFinished?.invoke()
                }
            }
            .pointerInput(valueRange, steps, travelPx) {
                detectHorizontalDragGestures(
                    onDragStart = { position -> changeTo(valueAt(position.x)) },
                    onDragEnd = { currentOnValueChangeFinished?.invoke() },
                    onDragCancel = { currentOnValueChangeFinished?.invoke() }
                ) { change, _ ->
                    change.consume()
                    changeTo(valueAt(change.position.x))
                }
            }

        val shownFraction = if (rangeSpan == 0f) 0f else (shownValue - valueRange.start) / rangeSpan
        val thumbCenterPx = shownFraction * travelPx + thumbWidthPx / 2

        Box(modifier = Modifier.fillMaxWidth().height(SliderHeight).then(pointerModifier)) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxWidth()
                    .height(RailHeight)
                    .background(trackColor, RailShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(with(density) { thumbCenterPx.toDp() })
                    .height(RailHeight)
                    .background(accentColor, RailShape)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset((thumbCenterPx - thumbWidthPx / 2).roundToInt(), 0) }
                    .width(ThumbWidth)
                    .height(ThumbHeight)
                    .shadow(
                        elevation = ThumbGlowElevation,
                        shape = ThumbShape,
                        ambientColor = accentColor.copy(alpha = THUMB_GLOW_ALPHA),
                        spotColor = accentColor.copy(alpha = THUMB_GLOW_ALPHA)
                    )
                    .background(accentColor, ThumbShape)
            ) {
                if (valueLabel != null) {
                    Text(
                        text = valueLabel(shownValue),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
