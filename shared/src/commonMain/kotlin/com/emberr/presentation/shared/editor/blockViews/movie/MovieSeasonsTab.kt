package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieEpisode
import com.emberr.domain.model.MovieEpisodeSummary
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSeason
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.DefaultBlockShape
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_down
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private val SeasonPosterWidth = 56.dp
private val SeasonPosterCornerRadius = 8.dp
private val EpisodeStillWidthDesktop = 160.dp
private val EpisodeStillWidthPhone = 120.dp
private val EpisodeStillShape = RoundedCornerShape(8.dp)
private const val SHORT_OVERVIEW_LINES = 3

internal fun hasSeasonsToShow(details: MovieDetails): Boolean =
    details.kind == MovieKind.SHOW &&
        (details.seasons.isNotEmpty() || details.nextEpisode != null || details.lastEpisode != null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MovieSeasonsTab(details: MovieDetails, colors: DetailsColors) {
    var openSeasonNumber by remember(details.tmdbId) { mutableStateOf<Int?>(null) }
    val airingEpisodes = listOfNotNull(
        details.nextEpisode?.let { "Next episode" to it },
        details.lastEpisode?.let { "Latest episode" to it }
    )
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        if (airingEpisodes.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DefaultBlockShape)
                    .background(colors.panel)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                airingEpisodes.forEach { (label, episode) -> MovieAiringEpisode(label = label, episode = episode, colors = colors) }
            }
        }
        if (details.seasons.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                details.seasons.forEach { season ->
                    val isOpen = openSeasonNumber == season.number
                    MovieSeasonCard(
                        showId = details.tmdbId,
                        season = season,
                        isOpen = isOpen,
                        colors = colors,
                        onToggle = { openSeasonNumber = if (isOpen) null else season.number }
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieAiringEpisode(label: String, episode: MovieEpisodeSummary, colors: DetailsColors) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
        val episodeLine = listOf("S${episode.seasonNumber}", "E${episode.episodeNumber}", episode.name)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
        Text(text = episodeLine, style = MaterialTheme.typography.bodyLarge, color = colors.text)
        episode.airDate?.let { date ->
            Text(text = releaseDateText(date), style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
        }
    }
}

@Composable
private fun MovieSeasonCard(
    showId: Long,
    season: MovieSeason,
    isOpen: Boolean,
    colors: DetailsColors,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .background(colors.panel)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MoviePoster(
                filePath = season.posterFilePath,
                url = season.posterUrl,
                width = SeasonPosterWidth,
                cornerRadius = SeasonPosterCornerRadius
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = season.name.ifBlank { "Season ${season.number}" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.text
                )
                val seasonFacts = listOfNotNull(
                    season.episodeCount?.let(::episodeCountText),
                    season.airDate?.year?.toString()
                ).joinToString(" · ")
                if (seasonFacts.isNotEmpty()) {
                    Text(text = seasonFacts, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                }
            }
            Icon(
                painter = painterResource(Res.drawable.chevron_down),
                contentDescription = if (isOpen) "Hide episodes" else "Show episodes",
                tint = colors.secondaryText,
                modifier = Modifier.size(18.dp).rotate(if (isOpen) 180f else 0f)
            )
        }
        if (isOpen) {
            MovieSeasonEpisodes(showId = showId, seasonNumber = season.number, colors = colors)
        }
    }
}

@Composable
private fun MovieSeasonEpisodes(showId: Long, seasonNumber: Int, colors: DetailsColors) {
    val titleSource = koinInject<MovieTitleSource>()
    val liveEpisodes = rememberLiveTmdbLoad("$showId:$seasonNumber") { titleSource.loadSeasonEpisodes(showId, seasonNumber) }
    val episodes = liveEpisodes.loadedValue
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        when {
            episodes == null -> LiveTmdbLoadMessage(
                state = liveEpisodes,
                textColor = colors.text,
                chipBackground = colors.chipBackground
            )
            episodes.isEmpty() -> Text(
                text = "No episodes listed yet",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.secondaryText
            )
            else -> episodes.forEach { episode -> MovieEpisodeRow(episode = episode, colors = colors) }
        }
    }
}

@Composable
private fun MovieEpisodeRow(episode: MovieEpisode, colors: DetailsColors) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(if (isDesktopPlatform) EpisodeStillWidthDesktop else EpisodeStillWidthPhone)
                    .aspectRatio(16f / 9f)
                    .clip(EpisodeStillShape)
                    .background(colors.chipBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "E${episode.number}", style = MaterialTheme.typography.labelSmall, color = colors.text)
                if (episode.stillUrl != null) {
                    MovieImage(filePath = null, url = episode.stillUrl, modifier = Modifier.fillMaxSize())
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = listOf("E${episode.number}", episode.name).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val episodeFacts = listOfNotNull(
                    episode.airDate?.let(::releaseDateText),
                    episode.runtimeMinutes?.let(::minutesText)
                ).joinToString(" · ")
                if (episodeFacts.isNotEmpty()) {
                    Text(text = episodeFacts, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
                }
            }
        }
        if (episode.overview.isNotBlank()) {
            MovieReadMoreText(text = episode.overview, color = colors.secondaryText, shortLines = SHORT_OVERVIEW_LINES)
        }
    }
}
