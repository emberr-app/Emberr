package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.movie.TmdbTooManyRequestsException
import com.emberr.presentation.shared.components.WavingDots
import kotlinx.coroutines.CancellationException

private const val SHIMMER_WAVE_MILLIS = 1300
private const val SHIMMER_DELAY_PER_ITEM = 0.08f
private val SkeletonLineShape = RoundedCornerShape(4.dp)

internal class ShimmerColors(val base: Color, val highlight: Color)

internal fun shimmerColorsOf(background: Color, textColor: Color): ShimmerColors =
    ShimmerColors(base = background, highlight = lerp(background, textColor, 0.14f))

@Composable
internal fun rememberShimmerProgress(): State<Float> =
    rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = SHIMMER_WAVE_MILLIS, easing = LinearEasing))
    )

internal fun Modifier.shimmer(progress: State<Float>, colors: ShimmerColors, itemIndex: Int = 0): Modifier =
    drawBehind {
        val bandWidth = size.width.coerceAtLeast(size.height)
        val itemProgress = (progress.value - itemIndex * SHIMMER_DELAY_PER_ITEM).mod(1f)
        val bandCenter = -bandWidth + itemProgress * (size.width + bandWidth * 2)
        drawRect(
            Brush.linearGradient(
                colors = listOf(colors.base, colors.highlight, colors.base),
                start = Offset(bandCenter - bandWidth / 2, 0f),
                end = Offset(bandCenter + bandWidth / 2, size.height / 3)
            )
        )
    }

@Composable
internal fun MovieSkeletonLine(
    widthFraction: Float,
    height: Dp,
    progress: State<Float>,
    colors: ShimmerColors,
    itemIndex: Int = 0
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(SkeletonLineShape)
            .shimmer(progress, colors, itemIndex)
    )
}

internal sealed interface LiveTmdbLoad<out T> {
    data object Loading : LiveTmdbLoad<Nothing>
    data class Loaded<T>(val value: T) : LiveTmdbLoad<T>
    data class Failed(val message: String) : LiveTmdbLoad<Nothing>
}

internal class LiveTmdbLoadState<T>(val load: LiveTmdbLoad<T>, val retry: () -> Unit)

internal val <T> LiveTmdbLoadState<T>.loadedValue: T? get() = (load as? LiveTmdbLoad.Loaded)?.value

@Composable
internal fun <T> rememberLiveTmdbLoad(key: Any, load: suspend () -> T?): LiveTmdbLoadState<T> {
    var attempt by remember(key) { mutableIntStateOf(0) }
    val latestLoad by rememberUpdatedState(load)
    val result by produceState<LiveTmdbLoad<T>>(LiveTmdbLoad.Loading, key, attempt) {
        value = LiveTmdbLoad.Loading
        value = try {
            latestLoad()?.let { LiveTmdbLoad.Loaded(it) } ?: LiveTmdbLoad.Failed("Couldn't find this on TMDB")
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: TmdbTooManyRequestsException) {
            LiveTmdbLoad.Failed("Too many requests right now. Try again in a minute.")
        } catch (_: Exception) {
            LiveTmdbLoad.Failed("Needs internet to load this")
        }
    }
    return LiveTmdbLoadState(result, retry = { attempt++ })
}

@Composable
internal fun LiveTmdbLoadMessage(
    state: LiveTmdbLoadState<*>,
    textColor: Color,
    chipBackground: Color,
    modifier: Modifier = Modifier
) {
    when (val load = state.load) {
        LiveTmdbLoad.Loading -> Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Loading", style = MaterialTheme.typography.bodyLarge, color = textColor)
            WavingDots(style = MaterialTheme.typography.bodyLarge.copy(color = textColor))
        }
        is LiveTmdbLoad.Failed -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = load.message, style = MaterialTheme.typography.bodyLarge, color = textColor)
            MovieChip(
                label = "Try again",
                onClick = state.retry,
                modifier = Modifier.height(movieChipHeight()),
                background = chipBackground,
                contentColor = textColor
            )
        }
        is LiveTmdbLoad.Loaded -> Unit
    }
}
