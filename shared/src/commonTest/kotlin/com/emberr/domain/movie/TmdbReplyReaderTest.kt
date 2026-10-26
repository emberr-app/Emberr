package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieEpisodeSummary
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MoviePersonRole
import com.emberr.domain.model.MovieSeason
import com.emberr.domain.model.MovieTrailer
import com.emberr.domain.model.MovieWatchProviders
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbReplyReaderTest {

    @Test
    fun aMovieReplyBecomesMovieDetails() {
        val details = TmdbReplyReader.movieDetailsFrom(SAVED_MOVIE_REPLY, language = "en", region = "GB", refreshedAt = 42L)

        assertEquals(155L, details.tmdbId)
        assertEquals(MovieKind.MOVIE, details.kind)
        assertEquals("The Dark Knight", details.title)
        assertEquals("2008", details.yearText)
        assertEquals("Welcome to a world without rules.", details.tagline)
        assertEquals(152, details.runtimeMinutes)
        assertEquals("PG-13", details.ageRating)
        assertEquals(8.5, details.tmdbRating)
        assertEquals(listOf("Drama", "Action", "Crime"), details.genres)
        assertEquals("Director", details.creditLabel)
        assertEquals(listOf("Christopher Nolan"), details.creditNames)
        assertEquals("English", details.language)
        assertEquals(listOf("United Kingdom", "United States of America"), details.countries)
        assertEquals(LocalDate(2008, 7, 16), details.releaseDate)
        assertEquals("https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg", details.posterUrl)
        assertEquals("https://image.tmdb.org/t/p/w1280/dqK9Hag1054tghRQSqLSaJ6PcAD.jpg", details.backdropUrl)
        assertEquals(
            listOf(
                MovieCastMember("Christian Bale", "Bruce Wayne", "https://image.tmdb.org/t/p/w185/bale.jpg", personId = 1),
                MovieCastMember("Heath Ledger", "Joker", "https://image.tmdb.org/t/p/w185/ledger.jpg", personId = 2),
                MovieCastMember("Aaron Eckhart", "Harvey Dent", null, personId = 3)
            ),
            details.cast
        )
        assertEquals(42L, details.refreshedAt)
        assertEquals(MovieTrailer(youtubeKey = "EXeTwQWrcwY", name = "Official Trailer"), details.trailer)
        assertEquals(
            listOf("tt0468569", "https://www.warnerbros.com/movies/dark-knight", "thedarkknight", "thedarkknight", "thedarkknight", "Q163872"),
            listOf(details.imdbId, details.homepage, details.instagramHandle, details.facebookHandle, details.xHandle, details.wikidataId)
        )
        assertEquals(
            listOf("The Dark Knight Collection", 185_000_000L, 1_004_558_444L, "Released"),
            listOf(details.collectionName, details.budgetDollars, details.revenueDollars, details.productionStatus)
        )
        assertEquals(
            listOf(
                MovieNamedLogo("Legendary Pictures", "https://image.tmdb.org/t/p/w185/legendary.png"),
                MovieNamedLogo("DC Comics", "https://image.tmdb.org/t/p/w185/dc.png"),
                MovieNamedLogo("Syncopy", null)
            ),
            details.productionCompanies
        )
        assertEquals(listOf("joker", "vigilante"), details.keywords)
        val appleTv = MovieNamedLogo("Apple TV", "https://image.tmdb.org/t/p/w92/apple.jpg")
        assertEquals(
            MovieWatchProviders(
                regionCode = "GB",
                link = "https://www.themoviedb.org/movie/155-the-dark-knight/watch?locale=GB",
                stream = listOf(
                    MovieNamedLogo("Max", "https://image.tmdb.org/t/p/w92/max.jpg"),
                    MovieNamedLogo("Netflix", "https://image.tmdb.org/t/p/w92/netflix.jpg")
                ),
                rent = listOf(appleTv),
                buy = listOf(appleTv)
            ),
            details.watchProviders
        )
        assertEquals("https://image.tmdb.org/t/p/w500/logo-en.png", details.titleLogoUrl)
    }

    @Test
    fun aShowReplyInAnotherLanguageBecomesShowDetailsWithEnglishText() {
        val details = TmdbReplyReader.showDetailsFrom(SAVED_SHOW_REPLY, language = "de", region = "DE", refreshedAt = 42L)

        assertEquals(1396L, details.tmdbId)
        assertEquals(MovieKind.SHOW, details.kind)
        assertEquals("Breaking Bad", details.title)
        assertEquals("2008 - 2013", details.yearText)
        assertEquals("Change the equation.", details.tagline)
        assertEquals("A chemistry teacher diagnosed with cancer turns to making meth.", details.overview)
        assertEquals(45, details.runtimeMinutes)
        assertEquals(5, details.seasonCount)
        assertEquals(62, details.episodeCount)
        assertEquals("16", details.ageRating)
        assertEquals(null, details.tmdbRating)
        assertEquals(listOf("Krimi"), details.genres)
        assertEquals("Created by", details.creditLabel)
        assertEquals(listOf("Vince Gilligan"), details.creditNames)
        assertEquals(LocalDate(2008, 1, 20), details.releaseDate)
        assertEquals(null, details.posterUrl)
        assertEquals(listOf("Bryan Cranston"), details.cast.map { it.name })
        assertEquals(MovieTrailer(youtubeKey = "deTrailer", name = "Offizieller Trailer"), details.trailer)
        assertEquals(listOf("tt0903747", "breakingbad"), listOf(details.imdbId, details.instagramHandle))
        assertEquals(listOf(MovieNamedLogo("AMC", "https://image.tmdb.org/t/p/w185/amc.png")), details.networks)
        assertEquals(
            listOf(
                MovieSeason(0, "Specials", 9, LocalDate(2009, 2, 17), null),
                MovieSeason(1, "Staffel 1", 7, LocalDate(2008, 1, 20), "https://image.tmdb.org/t/p/w342/season1.jpg")
            ),
            details.seasons
        )
        assertEquals(
            listOf(null, MovieEpisodeSummary(5, 16, "Felina", LocalDate(2013, 9, 29))),
            listOf(details.nextEpisode, details.lastEpisode)
        )
        assertEquals(listOf("drug dealer"), details.keywords)
    }

    @Test
    fun aPersonReplyBecomesAnActorPageWithTheBestKnownRolesFirst() {
        val person = TmdbReplyReader.personFrom(SAVED_PERSON_REPLY, mostRoles = 3)

        assertEquals(3894L, person.tmdbId)
        assertEquals("Christian Bale", person.name)
        assertEquals(
            listOf("Acting", "Haverfordwest, Pembrokeshire, Wales, UK", "https://image.tmdb.org/t/p/h632/bale.jpg"),
            listOf(person.knownFor, person.placeOfBirth, person.photoUrl)
        )
        assertEquals(listOf(LocalDate(1974, 1, 30), null), listOf(person.birthday, person.deathday))
        assertEquals("English biography.", person.biography)
        assertEquals(listOf("nm0000288", "christianbale"), listOf(person.imdbId, person.instagramHandle))
        assertEquals(
            listOf(
                MoviePersonRole(155, MovieKind.MOVIE, "The Dark Knight", "2008", "Bruce Wayne", "https://image.tmdb.org/t/p/w342/tdk.jpg"),
                MoviePersonRole(272, MovieKind.MOVIE, "Batman Begins", "2005", "Bruce Wayne", null),
                MoviePersonRole(2316, MovieKind.SHOW, "The Office", "2005", "Executive Producer", "https://image.tmdb.org/t/p/w342/office.jpg")
            ),
            person.roles
        )
    }

    private companion object {
        const val SAVED_MOVIE_REPLY = """
        {
          "adult": false,
          "id": 155,
          "imdb_id": "tt0468569",
          "title": "The Dark Knight",
          "original_title": "The Dark Knight",
          "tagline": "Welcome to a world without rules.",
          "overview": "Batman raises the stakes in his war on crime.",
          "release_date": "2008-07-16",
          "runtime": 152,
          "vote_average": 8.5,
          "vote_count": 33000,
          "genres": [{"id": 18, "name": "Drama"}, {"id": 28, "name": "Action"}, {"id": 80, "name": "Crime"}],
          "original_language": "en",
          "spoken_languages": [
            {"english_name": "Mandarin", "iso_639_1": "zh", "name": "普通话"},
            {"english_name": "English", "iso_639_1": "en", "name": "English"}
          ],
          "production_countries": [
            {"iso_3166_1": "GB", "name": "United Kingdom"},
            {"iso_3166_1": "US", "name": "United States of America"}
          ],
          "poster_path": "/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
          "backdrop_path": "/dqK9Hag1054tghRQSqLSaJ6PcAD.jpg",
          "homepage": "https://www.warnerbros.com/movies/dark-knight",
          "status": "Released",
          "budget": 185000000,
          "revenue": 1004558444,
          "belongs_to_collection": {"id": 263, "name": "The Dark Knight Collection", "poster_path": "/collection.jpg"},
          "production_companies": [
            {"id": 923, "name": "Legendary Pictures", "logo_path": "/legendary.png", "origin_country": "US"},
            {"id": 429, "name": "DC Comics", "logo_path": "/dc.svg", "origin_country": "US"},
            {"id": 9996, "name": "Syncopy", "logo_path": null, "origin_country": "GB"}
          ],
          "credits": {
            "cast": [
              {"id": 3, "name": "Aaron Eckhart", "character": "Harvey Dent", "order": 2, "profile_path": null},
              {"id": 1, "name": "Christian Bale", "character": "Bruce Wayne", "order": 0, "profile_path": "/bale.jpg"},
              {"id": 2, "name": "Heath Ledger", "character": "Joker", "order": 1, "profile_path": "/ledger.jpg"}
            ],
            "crew": [
              {"id": 525, "name": "Christopher Nolan", "job": "Director", "department": "Directing"},
              {"id": 525, "name": "Christopher Nolan", "job": "Screenplay", "department": "Writing"},
              {"id": 947, "name": "Hans Zimmer", "job": "Original Music Composer", "department": "Sound"}
            ]
          },
          "release_dates": {
            "results": [
              {"iso_3166_1": "DE", "release_dates": [{"certification": "16", "type": 3}]},
              {"iso_3166_1": "US", "release_dates": [{"certification": "", "type": 1}, {"certification": "PG-13", "type": 3}]}
            ]
          },
          "videos": {
            "results": [
              {"iso_639_1": "en", "name": "Official Teaser", "key": "teaserKey", "site": "YouTube", "type": "Teaser", "official": true},
              {"iso_639_1": "en", "name": "Vimeo Trailer", "key": "vimeoKey", "site": "Vimeo", "type": "Trailer", "official": true},
              {"iso_639_1": "de", "name": "Offizieller Trailer", "key": "deTrailer", "site": "YouTube", "type": "Trailer", "official": true},
              {"iso_639_1": "en", "name": "Fan Trailer", "key": "fanKey", "site": "YouTube", "type": "Trailer", "official": false},
              {"iso_639_1": "en", "name": "Official Trailer", "key": "EXeTwQWrcwY", "site": "YouTube", "type": "Trailer", "official": true},
              {"iso_639_1": "en", "name": "Behind the Scenes", "key": "clipKey", "site": "YouTube", "type": "Featurette", "official": true}
            ]
          },
          "external_ids": {
            "imdb_id": "tt0468569",
            "wikidata_id": "Q163872",
            "facebook_id": "thedarkknight",
            "instagram_id": "thedarkknight",
            "twitter_id": "thedarkknight"
          },
          "keywords": {"keywords": [{"id": 1, "name": "joker"}, {"id": 2, "name": "vigilante"}]},
          "watch/providers": {
            "results": {
              "US": {"link": "https://www.themoviedb.org/movie/155-the-dark-knight/watch?locale=US", "flatrate": [{"provider_name": "Hulu", "logo_path": "/hulu.jpg", "display_priority": 1}]},
              "GB": {
                "link": "https://www.themoviedb.org/movie/155-the-dark-knight/watch?locale=GB",
                "flatrate": [
                  {"provider_id": 8, "provider_name": "Netflix", "logo_path": "/netflix.jpg", "display_priority": 5},
                  {"provider_id": 1899, "provider_name": "Max", "logo_path": "/max.jpg", "display_priority": 2}
                ],
                "rent": [{"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/apple.jpg", "display_priority": 4}],
                "buy": [{"provider_id": 2, "provider_name": "Apple TV", "logo_path": "/apple.jpg", "display_priority": 4}]
              }
            }
          },
          "images": {
            "backdrops": [],
            "posters": [],
            "logos": [
              {"iso_639_1": "de", "file_path": "/logo-de.png", "vote_average": 9.0},
              {"iso_639_1": null, "file_path": "/logo-none.png", "vote_average": 8.0},
              {"iso_639_1": "en", "file_path": "/logo-en-lower-vote.png", "vote_average": 3.0},
              {"iso_639_1": "en", "file_path": "/logo-en.png", "vote_average": 5.5}
            ]
          }
        }
        """

        const val SAVED_PERSON_REPLY = """
        {
          "id": 3894,
          "name": "Christian Bale",
          "biography": "",
          "birthday": "1974-01-30",
          "deathday": null,
          "place_of_birth": "Haverfordwest, Pembrokeshire, Wales, UK",
          "known_for_department": "Acting",
          "profile_path": "/bale.jpg",
          "imdb_id": "nm0000288",
          "combined_credits": {
            "cast": [
              {"id": 272, "media_type": "movie", "title": "Batman Begins", "character": "Bruce Wayne", "release_date": "2005-06-10", "poster_path": null, "vote_count": 21000, "genre_ids": [28]},
              {"id": 155, "media_type": "movie", "title": "The Dark Knight", "character": "Bruce Wayne", "release_date": "2008-07-16", "poster_path": "/tdk.jpg", "vote_count": 33000, "genre_ids": [18, 28]},
              {"id": 22980, "media_type": "tv", "name": "The Tonight Show", "character": "Self", "first_air_date": "2009-06-01", "vote_count": 90000, "genre_ids": [10767]},
              {"id": 999, "media_type": "movie", "title": "", "character": "Cameo", "release_date": "2010-01-01", "vote_count": 25000, "genre_ids": []},
              {"id": 140, "media_type": "movie", "title": "Little Women", "character": "Laurie", "release_date": "1994-12-21", "vote_count": 3000, "genre_ids": [18]}
            ],
            "crew": [
              {"id": 155, "media_type": "movie", "title": "The Dark Knight", "job": "Thanks", "release_date": "2008-07-16", "vote_count": 33000, "genre_ids": []},
              {"id": 2316, "media_type": "tv", "name": "The Office", "job": "Executive Producer", "first_air_date": "2005-03-24", "poster_path": "/office.jpg", "vote_count": 4000, "genre_ids": [35]}
            ]
          },
          "external_ids": {"imdb_id": "nm0000288", "instagram_id": "christianbale", "facebook_id": null, "twitter_id": null, "wikidata_id": "Q45772"},
          "translations": {
            "translations": [
              {"iso_3166_1": "US", "iso_639_1": "en", "data": {"biography": "English biography."}},
              {"iso_3166_1": "DE", "iso_639_1": "de", "data": {"biography": ""}}
            ]
          }
        }
        """

        const val SAVED_SHOW_REPLY = """
        {
          "id": 1396,
          "name": "Breaking Bad",
          "original_name": "Breaking Bad",
          "tagline": "",
          "overview": "",
          "first_air_date": "2008-01-20",
          "last_air_date": "2013-09-29",
          "status": "Ended",
          "in_production": false,
          "episode_run_time": [45, 47],
          "last_episode_to_air": {"id": 62161, "runtime": 55, "name": "Felina", "season_number": 5, "episode_number": 16, "air_date": "2013-09-29"},
          "next_episode_to_air": null,
          "number_of_seasons": 5,
          "number_of_episodes": 62,
          "vote_average": 0.0,
          "vote_count": 0,
          "genres": [{"id": 80, "name": "Krimi"}],
          "created_by": [{"id": 66633, "name": "Vince Gilligan"}],
          "original_language": "en",
          "spoken_languages": [{"english_name": "English", "iso_639_1": "en", "name": "English"}],
          "production_countries": [{"iso_3166_1": "US", "name": "United States of America"}],
          "poster_path": null,
          "homepage": "https://www.sonypictures.com/tv/breakingbad",
          "networks": [{"id": 174, "name": "AMC", "logo_path": "/amc.png", "origin_country": "US"}],
          "seasons": [
            {"air_date": "2009-02-17", "episode_count": 9, "id": 3577, "name": "Specials", "poster_path": null, "season_number": 0},
            {"air_date": "2008-01-20", "episode_count": 7, "id": 3572, "name": "Staffel 1", "poster_path": "/season1.jpg", "season_number": 1}
          ],
          "videos": {
            "results": [
              {"iso_639_1": "en", "name": "Official Trailer", "key": "enTrailer", "site": "YouTube", "type": "Trailer", "official": true},
              {"iso_639_1": "de", "name": "Teaser", "key": "deTeaser", "site": "YouTube", "type": "Teaser", "official": true},
              {"iso_639_1": "de", "name": "Offizieller Trailer", "key": "deTrailer", "site": "YouTube", "type": "Trailer", "official": true}
            ]
          },
          "external_ids": {"imdb_id": "tt0903747", "tvdb_id": 81189, "wikidata_id": "Q1079", "facebook_id": null, "instagram_id": "breakingbad", "twitter_id": "BreakingBad"},
          "keywords": {"results": [{"id": 1, "name": "drug dealer"}]},
          "credits": {
            "cast": [{"id": 17419, "name": "Bryan Cranston", "character": "Walter White", "order": 0, "profile_path": "/cranston.jpg"}],
            "crew": []
          },
          "content_ratings": {
            "results": [
              {"iso_3166_1": "US", "rating": "TV-MA"},
              {"iso_3166_1": "DE", "rating": "16"}
            ]
          },
          "translations": {
            "translations": [
              {"iso_3166_1": "GB", "iso_639_1": "en", "data": {"name": "", "overview": "British overview.", "tagline": ""}},
              {"iso_3166_1": "US", "iso_639_1": "en", "data": {"name": "", "overview": "A chemistry teacher diagnosed with cancer turns to making meth.", "tagline": "Change the equation."}}
            ]
          }
        }
        """
    }
}
