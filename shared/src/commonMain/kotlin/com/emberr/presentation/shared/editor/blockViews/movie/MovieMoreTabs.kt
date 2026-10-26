package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieReview
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.MovieTitleExtras
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.DefaultBlockShape
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.star
import org.jetbrains.compose.resources.painterResource

private val SimilarPosterMinWidthDesktop = 100.dp
private val SimilarPosterMinWidthPhone = 88.dp
private val SimilarColumnGap = 16.dp
private val SimilarRowGap = 24.dp
private const val SHORT_REVIEW_LINES = 6

@Composable
internal fun MovieMoreLikeThisTab(
    liveExtras: LiveTmdbLoadState<MovieTitleExtras>,
    colors: DetailsColors,
    onOpenTitle: (MovieSearchResult) -> Unit
) {
    val extras = liveExtras.loadedValue
    when {
        extras == null -> LiveTmdbLoadMessage(state = liveExtras, textColor = colors.text, chipBackground = colors.chipBackground)
        extras.similarTitles.isEmpty() -> MovieTabEmptyMessage(text = "No similar titles yet", colors = colors)
        else -> BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val posterMinWidth = if (isDesktopPlatform) SimilarPosterMinWidthDesktop else SimilarPosterMinWidthPhone
            val columnCount = ((maxWidth + SimilarColumnGap) / (posterMinWidth + SimilarColumnGap)).toInt().coerceAtLeast(1)
            val posterWidth = (maxWidth - SimilarColumnGap * (columnCount - 1)) / columnCount
            Column(verticalArrangement = Arrangement.spacedBy(SimilarRowGap)) {
                extras.similarTitles.chunked(columnCount).forEach { rowTitles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(SimilarColumnGap)) {
                        rowTitles.forEach { title ->
                            MovieSimilarTitle(title = title, width = posterWidth, colors = colors, onClick = { onOpenTitle(title) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieSimilarTitle(title: MovieSearchResult, width: Dp, colors: DetailsColors, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(DefaultBlockShape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MoviePoster(filePath = null, url = title.posterUrl, width = width, cornerRadius = PosterCornerRadius)
        Text(
            text = title.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val facts = listOfNotNull(title.yearText.ifBlank { null }, title.tmdbRating?.let { "★ ${tmdbRatingText(it)}" })
            .joinToString(" · ")
        if (facts.isNotEmpty()) {
            Text(text = facts, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText, maxLines = 1)
        }
    }
}

@Composable
internal fun MovieReviewsTab(liveExtras: LiveTmdbLoadState<MovieTitleExtras>, colors: DetailsColors) {
    val extras = liveExtras.loadedValue
    when {
        extras == null -> LiveTmdbLoadMessage(state = liveExtras, textColor = colors.text, chipBackground = colors.chipBackground)
        extras.reviews.isEmpty() -> MovieTabEmptyMessage(text = "No reviews yet", colors = colors)
        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            extras.reviews.forEach { review -> MovieReviewCard(review = review, colors = colors) }
        }
    }
}

@Composable
private fun MovieReviewCard(review: MovieReview, colors: DetailsColors) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .background(colors.panel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = review.author,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                review.writtenOn?.let { date ->
                    Text(text = releaseDateText(date), style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                }
            }
            review.ratingOutOfTen?.let { rating ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        painter = painterResource(Res.drawable.star),
                        contentDescription = null,
                        tint = RatingStarColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(text = tmdbRatingText(rating), style = MaterialTheme.typography.labelSmall, color = colors.text)
                    Text(text = "/ 10", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                }
            }
        }
        MovieReadMoreText(text = review.text, color = colors.text, shortLines = SHORT_REVIEW_LINES)
        if (review.link.isNotBlank()) {
            MovieChip(
                label = "Read on TMDB",
                onClick = { uriHandler.openUri(review.link) },
                modifier = Modifier.height(movieChipHeight()),
                background = colors.chipBackground,
                contentColor = colors.chipText
            ) {
                Icon(
                    painter = painterResource(Res.drawable.link),
                    contentDescription = null,
                    tint = colors.chipText,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MovieTabEmptyMessage(text: String, colors: DetailsColors) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = colors.secondaryText)
}
