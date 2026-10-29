package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.film
import emberr.shared.generated.resources.star
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

internal const val PosterAspectRatio = 2f / 3f
internal val PosterCornerRadius = 12.dp
internal val RatingStarColor = Color(0xFFE0A82E)
internal val MovieBlock.isWatched: Boolean get() = watchStatus == WatchStatus.WATCHED
private val ChipShape = RoundedCornerShape(8.dp)

@Composable
internal fun TmdbRating(rating: Double, numberColor: Color = MaterialTheme.colorScheme.outline) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(Res.drawable.star),
            contentDescription = null,
            tint = RatingStarColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = tmdbRatingText(rating),
            style = MaterialTheme.typography.labelSmall,
            color = numberColor,
            maxLines = 1
        )
    }
}

@Composable
internal fun MovieChip(
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    background: Color = chipContainerColor(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(ChipShape)
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
internal fun movieChipHeight(): Dp =
    with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } + 12.dp

@Composable
internal fun MoviePoster(
    filePath: String?,
    url: String?,
    width: Dp,
    cornerRadius: Dp,
    savesPosterColor: Boolean = false
) {
    var isImageShown by remember(filePath, url) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(width)
            .aspectRatio(PosterAspectRatio)
            .clip(RoundedCornerShape(cornerRadius))
            .then(if (isImageShown) Modifier else Modifier.background(chipContainerColor())),
        contentAlignment = Alignment.Center
    ) {
        if (!isImageShown) {
            Icon(
                painter = painterResource(Res.drawable.film),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(width * 0.4f)
            )
        }
        if (filePath != null || url != null) {
            MovieImage(
                filePath = filePath,
                url = url,
                modifier = Modifier.fillMaxSize(),
                onImageLoaded = { loadedImage ->
                    isImageShown = true
                    if (savesPosterColor) savePosterColor(posterColorKeyOf(filePath, url), loadedImage)
                }
            )
        }
    }
}

@Composable
internal fun chipContainerColor(): Color =
    if (isDesktopPlatform) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant

@Composable
internal fun WatchStatus.dotColor(): Color {
    val isDark = LocalAppIsDark.current
    return when (this) {
        WatchStatus.NOT_WATCHED -> if (isDark) Color(0xFF529CCA) else Color(0xFF2383E2)
        WatchStatus.WATCHING -> if (isDark) Color(0xFFD9A347) else Color(0xFFCB912F)
        WatchStatus.WATCHED -> if (isDark) Color(0xFF5FA37E) else Color(0xFF448361)
    }
}

internal fun MovieDetails.lengthText(): String? = when (kind) {
    MovieKind.MOVIE -> runtimeMinutes?.let(::minutesText)
    MovieKind.SHOW -> seasonCount?.let { seasons -> if (seasons == 1) "1 season" else "$seasons seasons" }
}

internal fun minutesText(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

internal fun tmdbRatingText(rating: Double): String = ((rating * 10).roundToInt() / 10.0).toString()
