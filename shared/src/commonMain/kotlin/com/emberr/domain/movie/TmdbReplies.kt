package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieEpisode
import com.emberr.domain.model.MovieEpisodeSummary
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MoviePerson
import com.emberr.domain.model.MoviePersonRole
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.MovieSeason
import com.emberr.domain.model.MovieTrailer
import com.emberr.domain.model.MovieWatchProviders
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val POSTER_ADDRESS = "https://image.tmdb.org/t/p/w500"
private const val BACKDROP_ADDRESS = "https://image.tmdb.org/t/p/w1280"
private const val SEARCH_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w154"
private const val CAST_PHOTO_ADDRESS = "https://image.tmdb.org/t/p/w185"
private const val PERSON_PHOTO_ADDRESS = "https://image.tmdb.org/t/p/h632"
private const val ROLE_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w342"
private const val EPISODE_STILL_ADDRESS = "https://image.tmdb.org/t/p/w300"
private const val SEASON_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w342"
private const val COMPANY_LOGO_ADDRESS = "https://image.tmdb.org/t/p/w185"
private const val PROVIDER_LOGO_ADDRESS = "https://image.tmdb.org/t/p/w92"
private const val TITLE_LOGO_ADDRESS = "https://image.tmdb.org/t/p/w500"
private const val YOUTUBE = "YouTube"
private val trailerTypesInOrder = listOf("Trailer", "Teaser")
private val talkAndNewsGenreIds = setOf(10763, 10767)
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

    fun movieDetailsFrom(replyText: String, language: String, region: String, refreshedAt: Long): MovieDetails {
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
            backdropUrl = reply.backdropPath?.let { BACKDROP_ADDRESS + it },
            cast = castFrom(reply.credits),
            trailer = trailerFrom(reply.videos, language),
            imdbId = reply.imdbId.ifBlank { reply.externalIds.imdbId },
            homepage = reply.homepage,
            instagramHandle = reply.externalIds.instagramId,
            facebookHandle = reply.externalIds.facebookId,
            xHandle = reply.externalIds.twitterId,
            wikidataId = reply.externalIds.wikidataId,
            collectionName = reply.collection?.name.orEmpty(),
            budgetDollars = reply.budget.takeIf { it > 0 },
            revenueDollars = reply.revenue.takeIf { it > 0 },
            productionStatus = reply.status,
            productionCompanies = namedLogosFrom(reply.productionCompanies),
            keywords = reply.keywords.keywords.map { it.name }.filter { it.isNotBlank() },
            watchProviders = watchProvidersFrom(reply.watchProviders, region),
            titleLogoUrl = titleLogoUrlFrom(reply.images, language),
            refreshedAt = refreshedAt
        )
    }

    fun showDetailsFrom(replyText: String, language: String, region: String, refreshedAt: Long): MovieDetails {
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
            backdropUrl = reply.backdropPath?.let { BACKDROP_ADDRESS + it },
            cast = castFrom(reply.credits),
            trailer = trailerFrom(reply.videos, language),
            imdbId = reply.externalIds.imdbId,
            homepage = reply.homepage,
            instagramHandle = reply.externalIds.instagramId,
            facebookHandle = reply.externalIds.facebookId,
            xHandle = reply.externalIds.twitterId,
            wikidataId = reply.externalIds.wikidataId,
            productionStatus = reply.status,
            productionCompanies = namedLogosFrom(reply.productionCompanies),
            networks = namedLogosFrom(reply.networks),
            seasons = reply.seasons.map { season ->
                MovieSeason(
                    number = season.seasonNumber,
                    name = season.name,
                    episodeCount = season.episodeCount?.takeIf { it > 0 },
                    airDate = dateOrNull(season.airDate),
                    posterUrl = season.posterPath?.let { SEASON_POSTER_ADDRESS + it }
                )
            },
            nextEpisode = reply.nextEpisodeToAir?.toEpisodeSummary(),
            lastEpisode = reply.lastEpisodeToAir?.toEpisodeSummary(),
            keywords = reply.keywords.results.map { it.name }.filter { it.isNotBlank() },
            watchProviders = watchProvidersFrom(reply.watchProviders, region),
            titleLogoUrl = titleLogoUrlFrom(reply.images, language),
            refreshedAt = refreshedAt
        )
    }

    fun personFrom(replyText: String, mostRoles: Int): MoviePerson {
        val reply = json.decodeFromString<TmdbPersonReply>(replyText)
        return MoviePerson(
            tmdbId = reply.id,
            name = reply.name,
            knownFor = reply.knownForDepartment,
            birthday = dateOrNull(reply.birthday),
            deathday = dateOrNull(reply.deathday),
            placeOfBirth = reply.placeOfBirth,
            biography = reply.biography.ifBlank { reply.translations.englishData()?.biography.orEmpty() },
            photoUrl = reply.profilePath?.let { PERSON_PHOTO_ADDRESS + it },
            imdbId = reply.imdbId.ifBlank { reply.externalIds.imdbId },
            instagramHandle = reply.externalIds.instagramId,
            facebookHandle = reply.externalIds.facebookId,
            xHandle = reply.externalIds.twitterId,
            roles = rolesFrom(reply.combinedCredits, mostRoles)
        )
    }

    fun seasonEpisodesFrom(replyText: String): List<MovieEpisode> =
        json.decodeFromString<TmdbSeasonReply>(replyText).episodes
            .sortedBy { it.episodeNumber }
            .map { episode ->
                MovieEpisode(
                    number = episode.episodeNumber,
                    name = episode.name,
                    airDate = dateOrNull(episode.airDate),
                    runtimeMinutes = episode.runtime?.takeIf { it > 0 },
                    overview = episode.overview,
                    stillUrl = episode.stillPath?.let { EPISODE_STILL_ADDRESS + it }
                )
            }

    private fun rolesFrom(credits: TmdbCombinedCredits, mostRoles: Int): List<MoviePersonRole> {
        val castThenCrew = credits.cast.map { it to it.character } + credits.crew.map { it to it.job }
        return castThenCrew
            .filter { (credit, _) -> credit.genreIds.none { it in talkAndNewsGenreIds } }
            .distinctBy { (credit, _) -> credit.mediaType to credit.id }
            .sortedByDescending { (credit, _) -> credit.voteCount }
            .mapNotNull { (credit, roleName) -> credit.toRole(roleName) }
            .take(mostRoles)
    }

    private fun TmdbPersonCredit.toRole(roleName: String): MoviePersonRole? {
        val kind = when (mediaType) {
            "movie" -> MovieKind.MOVIE
            "tv" -> MovieKind.SHOW
            else -> return null
        }
        val shownTitle = if (kind == MovieKind.MOVIE) title else name
        if (shownTitle.isBlank()) return null
        return MoviePersonRole(
            tmdbId = id,
            kind = kind,
            title = shownTitle,
            yearText = (if (kind == MovieKind.MOVIE) releaseDate else firstAirDate).take(4),
            roleName = roleName,
            posterUrl = posterPath?.let { ROLE_POSTER_ADDRESS + it }
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
                    photoUrl = member.profilePath?.let { CAST_PHOTO_ADDRESS + it },
                    personId = member.id
                )
            }

    private fun trailerFrom(videos: TmdbVideos, language: String): MovieTrailer? =
        videos.results
            .filter { it.site == YOUTUBE && it.key.isNotBlank() && it.type in trailerTypesInOrder }
            .sortedWith(
                compareBy<TmdbVideo>(
                    { trailerTypesInOrder.indexOf(it.type) },
                    { languageRankOf(it.languageCode, language) },
                    { !it.official }
                )
            )
            .firstOrNull()
            ?.let { MovieTrailer(youtubeKey = it.key, name = it.name) }

    private fun titleLogoUrlFrom(images: TmdbImages, language: String): String? =
        images.logos
            .filter { it.filePath.isNotBlank() }
            .sortedWith(compareBy<TmdbImage>({ languageRankOf(it.languageCode, language) }, { -it.voteAverage }))
            .firstOrNull()
            ?.let { TITLE_LOGO_ADDRESS + pngPathOf(it.filePath) }

    private fun languageRankOf(languageCode: String?, language: String): Int =
        when {
            languageCode == language -> 0
            languageCode == ENGLISH -> 1
            languageCode.isNullOrBlank() -> 2
            else -> 3
        }

    private fun namedLogosFrom(companies: List<TmdbCompany>): List<MovieNamedLogo> =
        companies
            .filter { it.name.isNotBlank() }
            .map { company ->
                MovieNamedLogo(name = company.name, logoUrl = company.logoPath?.let { COMPANY_LOGO_ADDRESS + pngPathOf(it) })
            }

    private fun watchProvidersFrom(watchProviders: TmdbWatchProviders, region: String): MovieWatchProviders? {
        val regionCode = region.uppercase().ifBlank { FALLBACK_REGION }
        val regionProviders = watchProviders.results[regionCode] ?: return null
        return MovieWatchProviders(
            regionCode = regionCode,
            link = regionProviders.link,
            stream = providerLogosFrom(regionProviders.flatrate),
            rent = providerLogosFrom(regionProviders.rent),
            buy = providerLogosFrom(regionProviders.buy)
        )
    }

    private fun providerLogosFrom(providers: List<TmdbProvider>): List<MovieNamedLogo> =
        providers
            .filter { it.name.isNotBlank() }
            .sortedBy { it.displayPriority }
            .map { provider ->
                MovieNamedLogo(name = provider.name, logoUrl = provider.logoPath?.let { PROVIDER_LOGO_ADDRESS + it })
            }

    private fun pngPathOf(path: String): String =
        if (path.endsWith(".svg", ignoreCase = true)) path.dropLast(".svg".length) + ".png" else path

    private fun TmdbEpisode.toEpisodeSummary(): MovieEpisodeSummary? {
        val season = seasonNumber ?: return null
        val episode = episodeNumber ?: return null
        return MovieEpisodeSummary(seasonNumber = season, episodeNumber = episode, name = name, airDate = dateOrNull(airDate))
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
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("imdb_id") val imdbId: String = "",
    val homepage: String = "",
    val status: String = "",
    val budget: Long = 0L,
    val revenue: Long = 0L,
    @SerialName("belongs_to_collection") val collection: TmdbNamed? = null,
    @SerialName("production_companies") val productionCompanies: List<TmdbCompany> = emptyList(),
    val credits: TmdbCredits = TmdbCredits(),
    @SerialName("release_dates") val releaseDates: TmdbReleaseDates = TmdbReleaseDates(),
    val translations: TmdbTranslations = TmdbTranslations(),
    val videos: TmdbVideos = TmdbVideos(),
    @SerialName("external_ids") val externalIds: TmdbExternalIds = TmdbExternalIds(),
    val keywords: TmdbKeywords = TmdbKeywords(),
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders = TmdbWatchProviders(),
    val images: TmdbImages = TmdbImages()
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
    @SerialName("next_episode_to_air") val nextEpisodeToAir: TmdbEpisode? = null,
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
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val homepage: String = "",
    @SerialName("production_companies") val productionCompanies: List<TmdbCompany> = emptyList(),
    val networks: List<TmdbCompany> = emptyList(),
    val seasons: List<TmdbSeason> = emptyList(),
    val credits: TmdbCredits = TmdbCredits(),
    @SerialName("content_ratings") val contentRatings: TmdbContentRatings = TmdbContentRatings(),
    val translations: TmdbTranslations = TmdbTranslations(),
    val videos: TmdbVideos = TmdbVideos(),
    @SerialName("external_ids") val externalIds: TmdbExternalIds = TmdbExternalIds(),
    val keywords: TmdbKeywords = TmdbKeywords(),
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders = TmdbWatchProviders(),
    val images: TmdbImages = TmdbImages()
)

@Serializable
internal data class TmdbNamed(val name: String = "")

@Serializable
internal data class TmdbSpokenLanguage(
    @SerialName("iso_639_1") val code: String = "",
    @SerialName("english_name") val englishName: String = ""
)

@Serializable
internal data class TmdbEpisode(
    val runtime: Int? = null,
    val name: String = "",
    @SerialName("season_number") val seasonNumber: Int? = null,
    @SerialName("episode_number") val episodeNumber: Int? = null,
    @SerialName("air_date") val airDate: String = ""
)

@Serializable
internal data class TmdbCompany(
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null
)

@Serializable
internal data class TmdbSeason(
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    @SerialName("episode_count") val episodeCount: Int? = null,
    @SerialName("air_date") val airDate: String = "",
    @SerialName("poster_path") val posterPath: String? = null
)

@Serializable
internal data class TmdbVideos(val results: List<TmdbVideo> = emptyList())

@Serializable
internal data class TmdbVideo(
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false,
    @SerialName("iso_639_1") val languageCode: String? = null
)

@Serializable
internal data class TmdbExternalIds(
    @SerialName("imdb_id") val imdbId: String = "",
    @SerialName("wikidata_id") val wikidataId: String = "",
    @SerialName("facebook_id") val facebookId: String = "",
    @SerialName("instagram_id") val instagramId: String = "",
    @SerialName("twitter_id") val twitterId: String = ""
)

@Serializable
internal data class TmdbKeywords(
    val keywords: List<TmdbNamed> = emptyList(),
    val results: List<TmdbNamed> = emptyList()
)

@Serializable
internal data class TmdbWatchProviders(val results: Map<String, TmdbRegionProviders> = emptyMap())

@Serializable
internal data class TmdbRegionProviders(
    val link: String = "",
    val flatrate: List<TmdbProvider> = emptyList(),
    val rent: List<TmdbProvider> = emptyList(),
    val buy: List<TmdbProvider> = emptyList()
)

@Serializable
internal data class TmdbProvider(
    @SerialName("provider_name") val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = Int.MAX_VALUE
)

@Serializable
internal data class TmdbImages(val logos: List<TmdbImage> = emptyList())

@Serializable
internal data class TmdbImage(
    @SerialName("file_path") val filePath: String = "",
    @SerialName("iso_639_1") val languageCode: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0
)

@Serializable
internal data class TmdbCredits(
    val cast: List<TmdbCastMember> = emptyList(),
    val crew: List<TmdbCrewMember> = emptyList()
)

@Serializable
internal data class TmdbCastMember(
    val id: Long? = null,
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
    val tagline: String = "",
    val biography: String = ""
)

@Serializable
internal data class TmdbSeasonReply(val episodes: List<TmdbSeasonEpisode> = emptyList())

@Serializable
internal data class TmdbSeasonEpisode(
    @SerialName("episode_number") val episodeNumber: Int = 0,
    val name: String = "",
    @SerialName("air_date") val airDate: String = "",
    val runtime: Int? = null,
    val overview: String = "",
    @SerialName("still_path") val stillPath: String? = null
)

@Serializable
internal data class TmdbPersonReply(
    val id: Long,
    val name: String = "",
    val biography: String = "",
    val birthday: String = "",
    val deathday: String = "",
    @SerialName("place_of_birth") val placeOfBirth: String = "",
    @SerialName("known_for_department") val knownForDepartment: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    @SerialName("imdb_id") val imdbId: String = "",
    @SerialName("combined_credits") val combinedCredits: TmdbCombinedCredits = TmdbCombinedCredits(),
    @SerialName("external_ids") val externalIds: TmdbExternalIds = TmdbExternalIds(),
    val translations: TmdbTranslations = TmdbTranslations()
)

@Serializable
internal data class TmdbCombinedCredits(
    val cast: List<TmdbPersonCredit> = emptyList(),
    val crew: List<TmdbPersonCredit> = emptyList()
)

@Serializable
internal data class TmdbPersonCredit(
    val id: Long,
    @SerialName("media_type") val mediaType: String = "",
    val title: String = "",
    val name: String = "",
    val character: String = "",
    val job: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList()
)
