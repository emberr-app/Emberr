package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
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

private val SimilarPosterMinWidthDesktop = 140.dp
private val SimilarPosterMinWidthPhone = 88.dp
private val SimilarColumnGap = 16.dp
private val SimilarRowGap = 24.dp
private const val SHORT_REVIEW_LINES = 6
private const val SKELETON_SIMILAR_TITLES = 8
private const val SKELETON_REVIEWS = 2

@Composable
internal fun MovieMoreLikeThisTab(
    liveExtras: LiveTmdbLoadState<MovieTitleExtras>,
    colors: DetailsColors,
    onOpenTitle: (MovieSearchResult) -> Unit
) {
    val extras = liveExtras.loadedValue
    when {
        liveExtras.load == LiveTmdbLoad.Loading -> MovieSimilarTitlesSkeleton(colors = colors)
        extras == null -> LiveTmdbLoadMessage(state = liveExtras, textColor = colors.text, chipBackground = colors.chipBackground)
        extras.similarTitles.isEmpty() -> MovieTabEmptyMessage(text = "No similar titles yet", colors = colors)
        else -> MovieSimilarGrid(items = extras.similarTitles) { title, posterWidth ->
            MovieSimilarTitle(title = title, width = posterWidth, colors = colors, onClick = { onOpenTitle(title) })
        }
    }
}

@Composable
private fun <T : Any> MovieSimilarGrid(items: List<T>, cell: @Composable (item: T, posterWidth: Dp) -> Unit) {
    MovieAdaptiveGrid(
        items = items,
        minimumCellWidth = if (isDesktopPlatform) SimilarPosterMinWidthDesktop else SimilarPosterMinWidthPhone,
        columnGap = SimilarColumnGap,
        rowGap = SimilarRowGap,
        cell = cell
    )
}

@Composable
private fun MovieSimilarTitlesSkeleton(colors: DetailsColors) {
    val progress = rememberShimmerProgress()
    val shimmerColors = shimmerColorsOf(colors.chipBackground, colors.text)
    MovieSimilarGrid(items = (0 until SKELETON_SIMILAR_TITLES).toList()) { index, posterWidth ->
        MoviePosterSkeleton(width = posterWidth, progress = progress, shimmerColors = shimmerColors, itemIndex = index)
    }
}

@Composable
internal fun MoviePosterSkeleton(width: Dp, progress: State<Float>, shimmerColors: ShimmerColors, itemIndex: Int) {
    Column(modifier = Modifier.width(width), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(PosterAspectRatio)
                .clip(RoundedCornerShape(PosterCornerRadius))
                .shimmer(progress, shimmerColors, itemIndex = itemIndex)
        )
        MovieSkeletonLine(widthFraction = 0.85f, height = 12.dp, progress = progress, colors = shimmerColors, itemIndex = itemIndex)
        MovieSkeletonLine(widthFraction = 0.5f, height = 10.dp, progress = progress, colors = shimmerColors, itemIndex = itemIndex)
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
        liveExtras.load == LiveTmdbLoad.Loading -> MovieReviewsSkeleton(colors = colors)
        extras == null -> LiveTmdbLoadMessage(state = liveExtras, textColor = colors.text, chipBackground = colors.chipBackground)
        extras.reviews.isEmpty() -> MovieTabEmptyMessage(text = "No reviews yet", colors = colors)
        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            extras.reviews.forEach { review -> MovieReviewCard(review = review, colors = colors) }
        }
    }
}

@Composable
private fun MovieReviewsSkeleton(colors: DetailsColors) {
    val progress = rememberShimmerProgress()
    val shimmerColors = shimmerColorsOf(colors.chipBackground, colors.text)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(SKELETON_REVIEWS) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DefaultBlockShape)
                    .background(colors.panel)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MovieSkeletonLine(widthFraction = 0.35f, height = 14.dp, progress = progress, colors = shimmerColors, itemIndex = index)
                MovieSkeletonLine(widthFraction = 0.2f, height = 10.dp, progress = progress, colors = shimmerColors, itemIndex = index)
                listOf(1f, 1f, 0.9f, 0.6f).forEach { widthFraction ->
                    MovieSkeletonLine(widthFraction = widthFraction, height = 12.dp, progress = progress, colors = shimmerColors, itemIndex = index)
                }
            }
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
                contentColor = colors.text
            ) {
                Icon(
                    painter = painterResource(Res.drawable.link),
                    contentDescription = null,
                    tint = colors.text,
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
