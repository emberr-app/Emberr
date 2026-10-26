package com.emberr.domain.movie

import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieEpisode
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MoviePerson
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.MovieTitleExtras

interface MovieTitleSource {
    suspend fun search(query: String): List<MovieSearchResult>

    suspend fun loadDetails(tmdbId: Long, kind: MovieKind, mayReuseRecentCopy: Boolean = false): MovieDetails?

    suspend fun loadPerson(personId: Long): MoviePerson?

    suspend fun loadSeasonEpisodes(showId: Long, seasonNumber: Int): List<MovieEpisode>?

    suspend fun loadTitleExtras(tmdbId: Long, kind: MovieKind): MovieTitleExtras?
}
