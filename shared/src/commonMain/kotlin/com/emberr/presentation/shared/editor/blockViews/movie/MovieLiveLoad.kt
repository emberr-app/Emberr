package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.domain.movie.TmdbTooManyRequestsException
import com.emberr.presentation.shared.components.WavingDots
import kotlinx.coroutines.CancellationException

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
            ) {}
        }
        is LiveTmdbLoad.Loaded -> Unit
    }
}
