package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.emberr.presentation.shared.components.isBlurFallbackForced
import dev.chrisbanes.haze.HazeDefaults

internal val PosterBackdropBaseColor = Color(0xFF1C1C1C)
private const val BACKDROP_IMAGE_SCALE = 1.4f
private const val EXTRA_DARKNESS_WITHOUT_BLUR = 0.4f

internal data class MovieCardColors(
    val title: Color,
    val secondary: Color,
    val chipBackground: Color,
    val chipContent: Color,
    val icon: Color
)

@Composable
internal fun movieCardColors(isOnPoster: Boolean): MovieCardColors =
    if (isOnPoster) {
        MovieCardColors(
            title = Color.White,
            secondary = Color.White.copy(alpha = 0.75f),
            chipBackground = Color.White.copy(alpha = 0.16f),
            chipContent = Color.White,
            icon = Color.White.copy(alpha = 0.85f)
        )
    } else {
        MovieCardColors(
            title = MaterialTheme.colorScheme.onSurface,
            secondary = MaterialTheme.colorScheme.outline,
            chipBackground = chipContainerColor(),
            chipContent = MaterialTheme.colorScheme.onSurface,
            icon = MaterialTheme.colorScheme.outline
        )
    }

@Composable
internal fun BoxScope.BlurredPosterBackdrop(imageUrl: String, blurRadius: Dp, scrim: Brush) {
    val canBlur = HazeDefaults.blurEnabled() && !isBlurFallbackForced.value
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                scaleX = BACKDROP_IMAGE_SCALE
                scaleY = BACKDROP_IMAGE_SCALE
            }
            .then(if (canBlur) Modifier.blur(blurRadius, BlurredEdgeTreatment.Rectangle) else Modifier)
    )
    Box(modifier = Modifier.matchParentSize().background(scrim))
    if (!canBlur) {
        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = EXTRA_DARKNESS_WITHOUT_BLUR)))
    }
}
