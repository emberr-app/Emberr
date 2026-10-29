package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieEpisode
import com.emberr.domain.model.MovieEpisodeSummary
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MoviePerson
import com.emberr.domain.model.MoviePersonRole
import com.emberr.domain.model.MovieReview
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.MovieTitleExtras
import com.emberr.domain.model.MovieSeason
import com.emberr.domain.model.MovieTrailer
import com.emberr.domain.model.MovieWatchProviders
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json

private const val POSTER_ADDRESS = "https://image.tmdb.org/t/p/w500"
private const val SEARCH_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w154"
private const val CAST_PHOTO_ADDRESS = "https://image.tmdb.org/t/p/w185"
private const val PERSON_PHOTO_ADDRESS = "https://image.tmdb.org/t/p/h632"
private const val EPISODE_STILL_ADDRESS = "https://image.tmdb.org/t/p/w300"
private const val MEDIUM_POSTER_ADDRESS = "https://image.tmdb.org/t/p/w342"
private const val COMPANY_LOGO_ADDRESS = "https://image.tmdb.org/t/p/w185"
private const val PROVIDER_LOGO_ADDRESS = "https://image.tmdb.org/t/p/w92"
private const val YOUTUBE = "YouTube"
private val trailerTypesInOrder = listOf("Trailer", "Teaser")
private val talkAndNewsGenreIds = setOf(10763, 10767)
private const val MOST_CAST_MEMBERS = 10
private const val FALLBACK_REGION = "US"
private const val ENGLISH = "en"

internal object TmdbReplyReader {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun searchResultsFrom(replyText: String, mostResults: Int): List<MovieSearchResult> =
        json.decodeFromString<TmdbSearchReply>(replyText).results
            .mapNotNull { item -> movieKindOf(item.mediaType)?.let { kind -> item.toSearchResult(kind, SEARCH_POSTER_ADDRESS) } }
            .take(mostResults)

    fun movieDetailsFrom(replyText: String, language: String, region: String): MovieDetails {
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
            trailer = trailerFrom(reply.videos, language),
            imdbId = reply.imdbId.ifBlank { reply.externalIds.imdbId },
            homepage = reply.homepage,
            instagramHandle = reply.externalIds.instagramId,
            facebookHandle = reply.externalIds.facebookId,
            xHandle = reply.externalIds.twitterId,
            collectionName = reply.collection?.name.orEmpty(),
            budgetDollars = reply.budget.takeIf { it > 0 },
            revenueDollars = reply.revenue.takeIf { it > 0 },
            productionStatus = reply.status,
            productionCompanies = namedLogosFrom(reply.productionCompanies),
            keywords = reply.keywords.keywords.map { it.name }.filter { it.isNotBlank() },
            watchProviders = watchProvidersFrom(reply.watchProviders, region)
        )
    }

    fun showDetailsFrom(replyText: String, language: String, region: String): MovieDetails {
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
            trailer = trailerFrom(reply.videos, language),
            imdbId = reply.externalIds.imdbId,
            homepage = reply.homepage,
            instagramHandle = reply.externalIds.instagramId,
            facebookHandle = reply.externalIds.facebookId,
            xHandle = reply.externalIds.twitterId,
            productionStatus = reply.status,
            productionCompanies = namedLogosFrom(reply.productionCompanies),
            networks = namedLogosFrom(reply.networks),
            seasons = reply.seasons.map { season ->
                MovieSeason(
                    number = season.seasonNumber,
                    name = season.name,
                    episodeCount = season.episodeCount?.takeIf { it > 0 },
                    airDate = dateOrNull(season.airDate),
                    posterUrl = season.posterPath?.let { MEDIUM_POSTER_ADDRESS + it }
                )
            },
            nextEpisode = reply.nextEpisodeToAir?.toEpisodeSummary(),
            lastEpisode = reply.lastEpisodeToAir?.toEpisodeSummary(),
            keywords = reply.keywords.results.map { it.name }.filter { it.isNotBlank() },
            watchProviders = watchProvidersFrom(reply.watchProviders, region)
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

    fun titleExtrasFrom(replyText: String, kind: MovieKind, mostSimilarTitles: Int): MovieTitleExtras {
        val reply = json.decodeFromString<TmdbTitleExtrasReply>(replyText)
        val similarTitles = (reply.recommendations.results + reply.similar.results)
            .distinctBy { it.id }
            .mapNotNull { item -> item.toSearchResult(kind, MEDIUM_POSTER_ADDRESS) }
            .take(mostSimilarTitles)
        return MovieTitleExtras(similarTitles = similarTitles, reviews = reviewsOf(reply.reviews))
    }

    fun reviewsFrom(replyText: String): List<MovieReview> =
        reviewsOf(json.decodeFromString<TmdbReviewList>(replyText))

    private fun reviewsOf(reviews: TmdbReviewList): List<MovieReview> =
        reviews.results.mapNotNull { review ->
            val text = review.content.replace("\r\n", "\n").trim()
            if (text.isEmpty()) return@mapNotNull null
            MovieReview(
                author = review.authorDetails.name.ifBlank { review.author }.ifBlank { review.authorDetails.username },
                text = text,
                link = review.url,
                ratingOutOfTen = review.authorDetails.rating,
                writtenOn = dateOrNull(review.createdAt.take(10))
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
        val kind = movieKindOf(mediaType) ?: return null
        val shownTitle = if (kind == MovieKind.MOVIE) title else name
        if (shownTitle.isBlank()) return null
        return MoviePersonRole(
            tmdbId = id,
            kind = kind,
            title = shownTitle,
            yearText = (if (kind == MovieKind.MOVIE) releaseDate else firstAirDate).take(4),
            roleName = roleName,
            posterUrl = posterPath?.let { MEDIUM_POSTER_ADDRESS + it }
        )
    }

    private fun movieKindOf(mediaType: String): MovieKind? =
        when (mediaType) {
            "movie" -> MovieKind.MOVIE
            "tv" -> MovieKind.SHOW
            else -> null
        }

    private fun TmdbSearchItem.toSearchResult(kind: MovieKind, posterAddress: String): MovieSearchResult? {
        val shownTitle = if (kind == MovieKind.MOVIE) title else name
        if (shownTitle.isBlank()) return null
        val date = if (kind == MovieKind.MOVIE) releaseDate else firstAirDate
        return MovieSearchResult(
            tmdbId = id,
            kind = kind,
            title = shownTitle,
            yearText = date.take(4),
            tmdbRating = voteAverage.takeIf { voteCount > 0 },
            posterUrl = posterPath?.let { posterAddress + it }
        )
    }

    private fun showYearText(reply: TmdbShowReply): String {
        val firstYear = reply.firstAirDate.take(4)
        val lastYear = reply.lastAirDate.take(4)
        val isStillRunning = reply.inProduction || reply.status in StillAiringShowStatuses
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
