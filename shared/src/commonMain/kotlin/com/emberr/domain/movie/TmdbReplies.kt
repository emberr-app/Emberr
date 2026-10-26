package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val POSTER_ADDRESS = "https://image.tmdb.org/t/p/w500"
private const val SEARCH_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w154"
private const val CAST_PHOTO_ADDRESS = "https://image.tmdb.org/t/p/w185"
private const val MOST_CAST_MEMBERS = 10
private const val FALLBACK_REGION = "US"
private const val ENGLISH = "en"
private val runningShowStatuses = setOf("Returning Series", "In Production", "Planned")

internal object TmdbReplyReader {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun searchResultsFrom(replyText: String, mostResults: Int): List<MovieSearchResult> =
        json.decodeFromString<TmdbSearchReply>(replyText).results
            .mapNotNull { item -> item.toSearchResult() }
            .take(mostResults)

    fun movieDetailsFrom(replyText: String, region: String, refreshedAt: Long): MovieDetails {
        val reply = json.decodeFromString<TmdbMovieReply>(replyText)
        val english = reply.translations.englishData()
        return MovieDetails(
            tmdbId = reply.id,
            kind = MovieKind.MOVIE,
            title = reply.title.ifBlank { english?.title.orEmpty() }.ifBlank { reply.originalTitle },
            yearText = reply.releaseDate.take(4),
            tagline = reply.tagline.ifBlank { english?.tagline.orEmpty() },
            overview = reply.overview.ifBlank { english?.overview.orEmpty() },
            runtimeMinutes = reply.runtime?.takeIf { it > 0 },
            ageRating = ratingForRegion(region) { code ->
                reply.releaseDates.results
                    .firstOrNull { it.countryCode == code }
                    ?.releaseDates
                    ?.map { it.certification.trim() }
                    ?.firstOrNull { it.isNotEmpty() }
            },
            tmdbRating = reply.voteAverage.takeIf { reply.voteCount > 0 },
            genres = reply.genres.map { it.name }.filter { it.isNotBlank() },
            creditLabel = "Director",
            creditNames = reply.credits.crew.filter { it.job == "Director" }.map { it.name }.distinct(),
            language = languageNameOf(reply.originalLanguage, reply.spokenLanguages),
            countries = reply.productionCountries.map { it.name }.filter { it.isNotBlank() },
            releaseDate = dateOrNull(reply.releaseDate),
            posterUrl = reply.posterPath?.let { POSTER_ADDRESS + it },
            cast = castFrom(reply.credits),
            refreshedAt = refreshedAt
        )
    }

    fun showDetailsFrom(replyText: String, region: String, refreshedAt: Long): MovieDetails {
        val reply = json.decodeFromString<TmdbShowReply>(replyText)
        val english = reply.translations.englishData()
        return MovieDetails(
            tmdbId = reply.id,
            kind = MovieKind.SHOW,
            title = reply.name.ifBlank { english?.name.orEmpty() }.ifBlank { reply.originalName },
            yearText = showYearText(reply),
            tagline = reply.tagline.ifBlank { english?.tagline.orEmpty() },
            overview = reply.overview.ifBlank { english?.overview.orEmpty() },
            runtimeMinutes = (reply.episodeRunTime.firstOrNull() ?: reply.lastEpisodeToAir?.runtime)?.takeIf { it > 0 },
            seasonCount = reply.numberOfSeasons?.takeIf { it > 0 },
            episodeCount = reply.numberOfEpisodes?.takeIf { it > 0 },
            ageRating = ratingForRegion(region) { code ->
                reply.contentRatings.results.firstOrNull { it.countryCode == code }?.rating?.trim()?.takeIf { it.isNotEmpty() }
            },
            tmdbRating = reply.voteAverage.takeIf { reply.voteCount > 0 },
            genres = reply.genres.map { it.name }.filter { it.isNotBlank() },
            creditLabel = "Created by",
            creditNames = reply.createdBy.map { it.name }.filter { it.isNotBlank() },
            language = languageNameOf(reply.originalLanguage, reply.spokenLanguages),
            countries = reply.productionCountries.map { it.name }.filter { it.isNotBlank() },
            releaseDate = dateOrNull(reply.firstAirDate),
            posterUrl = reply.posterPath?.let { POSTER_ADDRESS + it },
            cast = castFrom(reply.credits),
            refreshedAt = refreshedAt
        )
    }

    private fun TmdbSearchItem.toSearchResult(): MovieSearchResult? {
        val kind = when (mediaType) {
            "movie" -> MovieKind.MOVIE
            "tv" -> MovieKind.SHOW
            else -> return null
        }
        val shownTitle = if (kind == MovieKind.MOVIE) title else name
        if (shownTitle.isBlank()) return null
        val date = if (kind == MovieKind.MOVIE) releaseDate else firstAirDate
        return MovieSearchResult(
            tmdbId = id,
            kind = kind,
            title = shownTitle,
            yearText = date.take(4),
            tmdbRating = voteAverage.takeIf { voteCount > 0 },
            posterUrl = posterPath?.let { SEARCH_POSTER_ADDRESS + it }
        )
    }

    private fun showYearText(reply: TmdbShowReply): String {
        val firstYear = reply.firstAirDate.take(4)
        val lastYear = reply.lastAirDate.take(4)
        val isStillRunning = reply.inProduction || reply.status in runningShowStatuses
        return when {
            firstYear.isBlank() -> ""
            isStillRunning -> "$firstYear - Present"
            lastYear.isBlank() || lastYear == firstYear -> firstYear
            else -> "$firstYear - $lastYear"
        }
    }

    private fun ratingForRegion(region: String, ratingIn: (countryCode: String) -> String?): String =
        listOf(region.uppercase(), FALLBACK_REGION).distinct().firstNotNullOfOrNull(ratingIn).orEmpty()

    private fun languageNameOf(languageCode: String, spokenLanguages: List<TmdbSpokenLanguage>): String =
        spokenLanguages.firstOrNull { it.code == languageCode }?.englishName?.takeIf { it.isNotBlank() }
            ?: languageCode.uppercase()

    private fun castFrom(credits: TmdbCredits): List<MovieCastMember> =
        credits.cast
            .sortedBy { it.order }
            .take(MOST_CAST_MEMBERS)
            .map { member ->
                MovieCastMember(
                    name = member.name,
                    character = member.character,
                    photoUrl = member.profilePath?.let { CAST_PHOTO_ADDRESS + it }
                )
            }

    private fun dateOrNull(text: String): LocalDate? =
        try {
            if (text.isBlank()) null else LocalDate.parse(text)
        } catch (_: IllegalArgumentException) {
            null
        }

    private fun TmdbTranslations.englishData(): TmdbTranslationData? =
        (translations.firstOrNull { it.languageCode == ENGLISH && it.countryCode == FALLBACK_REGION }
            ?: translations.firstOrNull { it.languageCode == ENGLISH })
            ?.data
}

@Serializable
internal data class TmdbSearchReply(val results: List<TmdbSearchItem> = emptyList())

@Serializable
internal data class TmdbSearchItem(
    val id: Long,
    @SerialName("media_type") val mediaType: String = "",
    val title: String = "",
    val name: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null
)

@Serializable
internal data class TmdbMovieReply(
    val id: Long,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String = "",
    val tagline: String = "",
    val overview: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    val runtime: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val genres: List<TmdbNamed> = emptyList(),
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("spoken_languages") val spokenLanguages: List<TmdbSpokenLanguage> = emptyList(),
    @SerialName("production_countries") val productionCountries: List<TmdbNamed> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    val credits: TmdbCredits = TmdbCredits(),
    @SerialName("release_dates") val releaseDates: TmdbReleaseDates = TmdbReleaseDates(),
    val translations: TmdbTranslations = TmdbTranslations()
)

@Serializable
internal data class TmdbShowReply(
    val id: Long,
    val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    val tagline: String = "",
    val overview: String = "",
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("last_air_date") val lastAirDate: String = "",
    val status: String = "",
    @SerialName("in_production") val inProduction: Boolean = false,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisode? = null,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val genres: List<TmdbNamed> = emptyList(),
    @SerialName("created_by") val createdBy: List<TmdbNamed> = emptyList(),
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("spoken_languages") val spokenLanguages: List<TmdbSpokenLanguage> = emptyList(),
    @SerialName("production_countries") val productionCountries: List<TmdbNamed> = emptyList(),
    @SerialName("poster_path") val posterPath: String? = null,
    val credits: TmdbCredits = TmdbCredits(),
    @SerialName("content_ratings") val contentRatings: TmdbContentRatings = TmdbContentRatings(),
    val translations: TmdbTranslations = TmdbTranslations()
)

@Serializable
internal data class TmdbNamed(val name: String = "")

@Serializable
internal data class TmdbSpokenLanguage(
    @SerialName("iso_639_1") val code: String = "",
    @SerialName("english_name") val englishName: String = ""
)

@Serializable
internal data class TmdbEpisode(val runtime: Int? = null)

@Serializable
internal data class TmdbCredits(
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

@Serializable
internal data class TmdbCastMember(
    val name: String = "",
    val character: String = "",
    val order: Int = Int.MAX_VALUE,
    @SerialName("profile_path") val profilePath: String? = null
)

@Serializable
internal data class TmdbCrewMember(
    val name: String = "",
    val job: String = ""
)

@Serializable
internal data class TmdbReleaseDates(val results: List<TmdbCountryReleases> = emptyList())

@Serializable
internal data class TmdbCountryReleases(
    @SerialName("iso_3166_1") val countryCode: String = "",
    @SerialName("release_dates") val releaseDates: List<TmdbRelease> = emptyList()
)

@Serializable
internal data class TmdbRelease(val certification: String = "")

@Serializable
internal data class TmdbContentRatings(val results: List<TmdbContentRating> = emptyList())

@Serializable
internal data class TmdbContentRating(
    @SerialName("iso_3166_1") val countryCode: String = "",
    val rating: String = ""
)

@Serializable
internal data class TmdbTranslations(val translations: List<TmdbTranslation> = emptyList())

@Serializable
internal data class TmdbTranslation(
    @SerialName("iso_639_1") val languageCode: String = "",
    @SerialName("iso_3166_1") val countryCode: String = "",
    val data: TmdbTranslationData = TmdbTranslationData()
)

@Serializable
internal data class TmdbTranslationData(
    val title: String = "",
    val name: String = "",
    val overview: String = "",
    val tagline: String = ""
)
