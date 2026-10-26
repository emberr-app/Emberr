package com.emberr.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MovieKind(val label: String) {
    MOVIE("Movie"),
    SHOW("Show")
}

@Serializable
enum class WatchStatus(val label: String) {
    NOT_WATCHED("Not watched"),
    WATCHING("Watching"),
    WATCHED("Watched")
}

@Serializable
enum class WatchlistView(val label: String) {
    LIST("List"),
    BOARD("Board"),
    GALLERY("Gallery")
}

@Serializable
enum class WatchlistSortKey(val label: String, val sortsDescendingFirst: Boolean) {
    STATUS("Status", false),
    YOUR_RATING("Your rating", true),
    TMDB_RATING("TMDB rating", true),
    YEAR("Year", true),
    DATE_ADDED("Date added", true),
    TITLE("Title", false)
}

@Serializable
enum class WatchlistFilterField(val label: String) {
    TYPE("Type"),
    STATUS("Status"),
    GENRE("Genre"),
    RELEASE_DATE("Release date"),
    DATE_ADDED("Date added"),
    YOUR_RATING("Your rating"),
    TMDB_RATING("TMDB rating"),
    RUNTIME("Runtime"),
    LANGUAGE("Language"),
    WHERE_TO_WATCH("Where to watch"),
    SHOW_STATUS("Show status")
}

@Serializable
enum class WatchlistFilterCondition(val label: String) {
    IS("is"),
    IS_ANY_OF("is any of"),
    HAS_ANY_OF("has any of"),
    HAS_NONE_OF("has none of"),
    STREAMS_ON_ANY_OF("streams on any of"),
    BEFORE("before"),
    AFTER("after"),
    ON_OR_AFTER("on or after"),
    BETWEEN("between"),
    WITHIN_PAST_DAYS("within the past days"),
    WITHIN_PAST_YEARS("within the past years"),
    AT_LEAST("at least"),
    AT_MOST("at most"),
    NOT_RATED_YET("not rated yet"),
    SHORTER_THAN("shorter than"),
    LONGER_THAN("longer than"),
    STILL_AIRING("still airing"),
    ENDED("ended")
}

@Immutable
@Serializable
data class WatchlistFilter(
    val id: String,
    val field: WatchlistFilterField,
    val condition: WatchlistFilterCondition,
    val picks: List<String> = emptyList(),
    val date: LocalDate? = null,
    val endDate: LocalDate? = null,
    val number: Double? = null
)

@Immutable
@Serializable
data class WatchlistViewSettings(
    val id: String,
    val name: String,
    val layout: WatchlistView,
    val sortKey: WatchlistSortKey = WatchlistSortKey.STATUS,
    val sortsDescending: Boolean = false,
    val filters: List<WatchlistFilter> = emptyList()
)

@Immutable
@Serializable
data class MovieCastMember(
    val name: String,
    val character: String = "",
    val photoUrl: String? = null,
    val photoFilePath: String? = null,
    val personId: Long? = null
)

@Immutable
@Serializable
data class MovieTrailer(
    val youtubeKey: String,
    val name: String = ""
)

@Immutable
@Serializable
data class MovieNamedLogo(
    val name: String,
    val logoUrl: String? = null,
    val logoFilePath: String? = null
)

@Immutable
@Serializable
data class MovieSeason(
    val number: Int,
    val name: String = "",
    val episodeCount: Int? = null,
    val airDate: LocalDate? = null,
    val posterUrl: String? = null,
    val posterFilePath: String? = null
)

@Immutable
@Serializable
data class MovieEpisodeSummary(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val name: String = "",
    val airDate: LocalDate? = null
)

@Immutable
@Serializable
data class MovieWatchProviders(
    val regionCode: String,
    val link: String = "",
    val stream: List<MovieNamedLogo> = emptyList(),
    val rent: List<MovieNamedLogo> = emptyList(),
    val buy: List<MovieNamedLogo> = emptyList()
)

@Immutable
@Serializable
data class MovieDetails(
    val tmdbId: Long,
    val kind: MovieKind,
    val title: String,
    val yearText: String = "",
    val tagline: String = "",
    val overview: String = "",
    val runtimeMinutes: Int? = null,
    val seasonCount: Int? = null,
    val episodeCount: Int? = null,
    val ageRating: String = "",
    val tmdbRating: Double? = null,
    val genres: List<String> = emptyList(),
    val creditLabel: String = "",
    val creditNames: List<String> = emptyList(),
    val language: String = "",
    val countries: List<String> = emptyList(),
    val releaseDate: LocalDate? = null,
    val posterUrl: String? = null,
    val posterFilePath: String? = null,
    val backdropUrl: String? = null,
    val backdropFilePath: String? = null,
    val cast: List<MovieCastMember> = emptyList(),
    val trailer: MovieTrailer? = null,
    val imdbId: String = "",
    val homepage: String = "",
    val instagramHandle: String = "",
    val facebookHandle: String = "",
    val xHandle: String = "",
    val wikidataId: String = "",
    val collectionName: String = "",
    val budgetDollars: Long? = null,
    val revenueDollars: Long? = null,
    val productionStatus: String = "",
    val productionCompanies: List<MovieNamedLogo> = emptyList(),
    val networks: List<MovieNamedLogo> = emptyList(),
    val seasons: List<MovieSeason> = emptyList(),
    val nextEpisode: MovieEpisodeSummary? = null,
    val lastEpisode: MovieEpisodeSummary? = null,
    val keywords: List<String> = emptyList(),
    val watchProviders: MovieWatchProviders? = null,
    val titleLogoUrl: String? = null,
    val titleLogoFilePath: String? = null,
    val refreshedAt: Long = 0L
)

@Immutable
@Serializable
@SerialName("movie")
data class MovieBlock(
    override val id: String,
    val details: MovieDetails? = null,
    val watchStatus: WatchStatus = WatchStatus.NOT_WATCHED,
    val ratingHalfStars: Int? = null,
    val watchStateChangedAt: Long = 0L,
    val addedAt: Long = 0L,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isHighlighted: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("watchlist")
data class WatchlistBlock(
    override val id: String,
    val watchlistNoteId: String,
    val title: String = "Watchlist",
    val views: List<WatchlistViewSettings> = emptyList(),
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isHighlighted: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
data class MovieEpisode(
    val number: Int,
    val name: String = "",
    val airDate: LocalDate? = null,
    val runtimeMinutes: Int? = null,
    val overview: String = "",
    val stillUrl: String? = null
)

@Immutable
data class MoviePersonRole(
    val tmdbId: Long,
    val kind: MovieKind,
    val title: String,
    val yearText: String = "",
    val roleName: String = "",
    val posterUrl: String? = null
)

@Immutable
data class MoviePerson(
    val tmdbId: Long,
    val name: String,
    val knownFor: String = "",
    val birthday: LocalDate? = null,
    val deathday: LocalDate? = null,
    val placeOfBirth: String = "",
    val biography: String = "",
    val photoUrl: String? = null,
    val imdbId: String = "",
    val instagramHandle: String = "",
    val facebookHandle: String = "",
    val xHandle: String = "",
    val roles: List<MoviePersonRole> = emptyList()
)

@Immutable
data class MovieReview(
    val author: String,
    val text: String,
    val link: String,
    val ratingOutOfTen: Double? = null,
    val writtenOn: LocalDate? = null
)

@Immutable
data class MovieTitleExtras(
    val similarTitles: List<MovieSearchResult> = emptyList(),
    val reviews: List<MovieReview> = emptyList()
)

@Immutable
data class MovieSearchResult(
    val tmdbId: Long,
    val kind: MovieKind,
    val title: String,
    val yearText: String = "",
    val tmdbRating: Double? = null,
    val posterUrl: String? = null
)

fun starsTextOf(halfStars: Int): String =
    if (halfStars % 2 == 0) "${halfStars / 2}" else "${halfStars / 2}.5"

fun MovieBlock.summaryLine(): String? {
    val details = details ?: return null
    val titleWithYear = if (details.yearText.isBlank()) details.title else "${details.title} (${details.yearText})"
    val rating = ratingHalfStars?.let { "★ ${starsTextOf(it)}" }
    return listOfNotNull(titleWithYear, watchStatus.label, rating).joinToString(" · ")
}

data class WatchState(
    val status: WatchStatus,
    val ratingHalfStars: Int?,
    val changedAt: Long
)

fun MovieDetails.titleKey(): String = "${kind.name.lowercase()}:$tmdbId"

fun MovieBlock.titleKey(): String? = details?.titleKey()

fun MovieSearchResult.titleKey(): String = "${kind.name.lowercase()}:$tmdbId"

fun MovieBlock.watchState(): WatchState = WatchState(watchStatus, ratingHalfStars, watchStateChangedAt)

fun MovieBlock.withWatchState(state: WatchState, now: Long): MovieBlock =
    copy(
        watchStatus = state.status,
        ratingHalfStars = state.ratingHalfStars,
        watchStateChangedAt = state.changedAt,
        updatedAt = now
    )

val MovieDetails.hasPoster: Boolean get() = posterFilePath != null || posterUrl != null

val MovieDetails.hasBackdrop: Boolean get() = backdropFilePath != null || backdropUrl != null

val MovieTrailer.youtubeLink: String get() = "https://www.youtube.com/watch?v=$youtubeKey"

val MovieWatchProviders.allProviders: List<MovieNamedLogo> get() = stream + rent + buy

fun MovieBlock.mediaFilePaths(): List<String> {
    val details = details ?: return emptyList()
    val logos = details.productionCompanies + details.networks + details.watchProviders?.allProviders.orEmpty()
    return listOfNotNull(details.posterFilePath, details.backdropFilePath, details.titleLogoFilePath) +
        details.cast.mapNotNull { it.photoFilePath } +
        details.seasons.mapNotNull { it.posterFilePath } +
        logos.mapNotNull { it.logoFilePath }
}
