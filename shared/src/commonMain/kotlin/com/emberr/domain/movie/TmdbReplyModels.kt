package com.emberr.domain.movie

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders = TmdbWatchProviders()
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
    @SerialName("watch/providers") val watchProviders: TmdbWatchProviders = TmdbWatchProviders()
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
internal data class TmdbTitleExtrasReply(
    val recommendations: TmdbSearchReply = TmdbSearchReply(),
    val similar: TmdbSearchReply = TmdbSearchReply(),
    val reviews: TmdbReviewList = TmdbReviewList()
)

@Serializable
internal data class TmdbReviewList(val results: List<TmdbReview> = emptyList())

@Serializable
internal data class TmdbReview(
    val author: String = "",
    @SerialName("author_details") val authorDetails: TmdbReviewAuthor = TmdbReviewAuthor(),
    val content: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val url: String = ""
)

@Serializable
internal data class TmdbReviewAuthor(
    val name: String = "",
    val username: String = "",
    val rating: Double? = null
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
