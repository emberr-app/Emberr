package com.emberr.presentation.shared

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

private const val LONGEST_WAIT_FOR_NEW_LANGUAGE_MILLIS = 1500L
private const val BLUR_IN_MILLIS = 400
private const val NAMES_APPEAR_MILLIS = 120
private const val NAME_ROLL_MILLIS = 1000
private const val SHARPEN_MILLIS = 380
private const val NEIGHBOUR_NAME_FADE = 0.4f
private const val VISIBLE_NAME_LINES = 5
private const val ROLL_LINE_HEIGHT_TIMES_FONT_SIZE = 2.6f
private const val CENTER_NAME_EXTRA_SCALE = 0.4f
private val StrongestBlur = 8.dp
private val NameRollEasing = CubicBezierEasing(0.45f, 0f, 0.15f, 1f)

private val LocalAppLanguageTag = staticCompositionLocalOf { "" }

internal class LanguageChangeRequest(
    val chosenLanguageName: String,
    val namesRolledPast: List<String>,
    val namesBelow: List<String>,
    val applyLanguage: () -> Unit
)

object LanguageChangeTransition {
    internal val languageChangeRequests = MutableSharedFlow<LanguageChangeRequest>(extraBufferCapacity = 1)

    fun changeLanguage(
        chosenLanguageName: String,
        namesRolledPast: List<String>,
        namesBelow: List<String>,
        applyLanguage: () -> Unit
    ) {
        languageChangeRequests.tryEmit(LanguageChangeRequest(chosenLanguageName, namesRolledPast, namesBelow, applyLanguage))
    }
}

internal class LanguageNameRollState {
    val blurAmount = Animatable(0f)
    val namesAlpha = Animatable(0f)
    val rollPosition = Animatable(0f)
    var rollingNames by mutableStateOf(emptyList<String>())
        private set

    suspend fun blurAndRollTo(chosenLanguageName: String, namesRolledPast: List<String>, namesBelow: List<String>) {
        rollingNames = namesRolledPast + chosenLanguageName + namesBelow
        rollPosition.snapTo(0f)
        coroutineScope {
            launch { blurAmount.animateTo(1f, tween(BLUR_IN_MILLIS)) }
            launch { namesAlpha.animateTo(1f, tween(NAMES_APPEAR_MILLIS)) }
            rollPosition.animateTo(namesRolledPast.size.toFloat(), tween(NAME_ROLL_MILLIS, easing = NameRollEasing))
        }
    }

    suspend fun sharpen() {
        coroutineScope {
            launch { namesAlpha.animateTo(0f, tween(SHARPEN_MILLIS)) }
            blurAmount.animateTo(0f, tween(SHARPEN_MILLIS))
        }
    }
}

@Composable
fun LanguageChangeTransitionHost(languageTag: String, content: @Composable () -> Unit) {
    val rollState = remember { LanguageNameRollState() }
    val currentLanguageTag by rememberUpdatedState(languageTag)

    LaunchedEffect(Unit) {
        LanguageChangeTransition.languageChangeRequests.collect { request ->
            val languageTagBeforeChange = currentLanguageTag
            rollState.blurAndRollTo(request.chosenLanguageName, request.namesRolledPast, request.namesBelow)
            request.applyLanguage()
            withTimeoutOrNull(LONGEST_WAIT_FOR_NEW_LANGUAGE_MILLIS) {
                snapshotFlow { currentLanguageTag }.first { tag -> tag != languageTagBeforeChange }
            }
            withFrameNanos { }
            rollState.sharpen()
        }
    }

    LanguageNameRollLayer(rollState = rollState) {
        CompositionLocalProvider(LocalAppLanguageTag provides languageTag, content = content)
    }
}

@Composable
internal fun LanguageNameRollLayer(rollState: LanguageNameRollState, content: @Composable () -> Unit) {
    val nameStyle = MaterialTheme.typography.titleLarge
    val nameColor = MaterialTheme.colorScheme.onBackground
    val rollLineHeight = with(LocalDensity.current) { (nameStyle.fontSize * ROLL_LINE_HEIGHT_TIMES_FONT_SIZE).toDp() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val blurPixels = StrongestBlur.toPx() * rollState.blurAmount.value
                    renderEffect = if (blurPixels > 0f) BlurEffect(blurPixels, blurPixels, TileMode.Decal) else null
                }
        ) {
            content()
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(rollLineHeight * VISIBLE_NAME_LINES)
                .graphicsLayer { alpha = rollState.namesAlpha.value }
        ) {
            rollState.rollingNames.forEachIndexed { index, name ->
                Text(
                    text = name,
                    style = nameStyle,
                    color = nameColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            val distanceFromCenter = index - rollState.rollPosition.value
                            translationY = distanceFromCenter * rollLineHeight.toPx()
                            alpha = (1f - abs(distanceFromCenter) * NEIGHBOUR_NAME_FADE).coerceIn(0f, 1f)
                            val nameScale = 1f + CENTER_NAME_EXTRA_SCALE * (1f - abs(distanceFromCenter)).coerceIn(0f, 1f)
                            scaleX = nameScale
                            scaleY = nameScale
                        }
                )
            }
        }
    }
}
