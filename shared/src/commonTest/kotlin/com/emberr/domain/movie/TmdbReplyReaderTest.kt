package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieKind
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbReplyReaderTest {

    @Test
    fun aMovieReplyBecomesMovieDetails() {
        val details = TmdbReplyReader.movieDetailsFrom(SAVED_MOVIE_REPLY, region = "GB", refreshedAt = 42L)

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
                MovieCastMember("Christian Bale", "Bruce Wayne", "https://image.tmdb.org/t/p/w185/bale.jpg"),
                MovieCastMember("Heath Ledger", "Joker", "https://image.tmdb.org/t/p/w185/ledger.jpg"),
                MovieCastMember("Aaron Eckhart", "Harvey Dent", null)
            ),
            details.cast
        )
        assertEquals(42L, details.refreshedAt)
    }

    @Test
    fun aShowReplyInAnotherLanguageBecomesShowDetailsWithEnglishText() {
        val details = TmdbReplyReader.showDetailsFrom(SAVED_SHOW_REPLY, region = "DE", refreshedAt = 42L)

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
          "belongs_to_collection": null,
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
          "last_episode_to_air": {"id": 62161, "runtime": 55},
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
