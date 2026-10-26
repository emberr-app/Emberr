package com.emberr.domain.movie

import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult

interface MovieTitleSource {
    suspend fun search(query: String): List<MovieSearchResult>

    suspend fun loadDetails(tmdbId: Long, kind: MovieKind): MovieDetails?
}
