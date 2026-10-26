package com.emberr.domain.movie

import androidx.compose.ui.text.intl.Locale
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TMDB_PROXY_ADDRESS = "https://emberr-tmdb.emberr.workers.dev"
private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
private const val MOST_SEARCH_RESULTS = 10
private const val ENGLISH = "en"
private const val NO_LANGUAGE = "null"
private val savedInfoParts = listOf("videos", "external_ids", "keywords", "watch/providers", "images")
private const val TRENDING_KEEP_MILLIS = 60 * 60 * 1000L

private class SavedTrending(val languageTag: String, val results: List<MovieSearchResult>, val savedAtMillis: Long)

class TmdbRequestFailedException(status: HttpStatusCode) : Exception("TMDB answered ${status.value}")

class TmdbTooManyRequestsException : Exception("Too many TMDB requests")

class TmdbMovieTitleSource : MovieTitleSource {

    private val httpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            }
        }
    }

    private val trendingMutex = Mutex()
    private var savedTrending: SavedTrending? = null

    override suspend fun search(query: String): List<MovieSearchResult> {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return trending()
        val response = requestTmdb("search/multi") {
            parameter("query", trimmedQuery)
            parameter("include_adult", false)
        }
        return searchResultsFrom(response)
    }

    private suspend fun trending(): List<MovieSearchResult> = trendingMutex.withLock {
        val languageTag = deviceLanguageTag()
        val now = System.currentTimeMillis()
        val saved = savedTrending
        if (saved != null && saved.languageTag == languageTag && now - saved.savedAtMillis < TRENDING_KEEP_MILLIS) {
            return@withLock saved.results
        }
        val results = searchResultsFrom(requestTmdb("trending/all/week"))
        savedTrending = SavedTrending(languageTag, results, now)
        results
    }

    private suspend fun searchResultsFrom(response: HttpResponse): List<MovieSearchResult> {
        val replyText = successfulReplyText(response)
        return withContext(Dispatchers.Default) { TmdbReplyReader.searchResultsFrom(replyText, MOST_SEARCH_RESULTS) }
    }

    override suspend fun loadDetails(tmdbId: Long, kind: MovieKind): MovieDetails? {
        val language = Locale.current.language.lowercase()
        val isEnglish = language == ENGLISH
        val ratingsPart = if (kind == MovieKind.MOVIE) "release_dates" else "content_ratings"
        val extraParts = listOfNotNull("credits", ratingsPart, "translations".takeUnless { isEnglish }) + savedInfoParts
        val extraLanguages = listOf(language, ENGLISH, NO_LANGUAGE).distinct().joinToString(",")
        val path = if (kind == MovieKind.MOVIE) "movie/$tmdbId" else "tv/$tmdbId"
        val response = requestTmdb(path) {
            parameter("append_to_response", extraParts.joinToString(","))
            parameter("include_video_language", extraLanguages)
            parameter("include_image_language", extraLanguages)
        }
        if (response.status == HttpStatusCode.NotFound) return null

        val replyText = successfulReplyText(response)
        val region = Locale.current.region
        val now = System.currentTimeMillis()
        return withContext(Dispatchers.Default) {
            when (kind) {
                MovieKind.MOVIE -> TmdbReplyReader.movieDetailsFrom(replyText, language, region, now)
                MovieKind.SHOW -> TmdbReplyReader.showDetailsFrom(replyText, language, region, now)
            }
        }
    }

    private suspend fun requestTmdb(
        path: String,
        addParameters: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse =
        httpClient.get("$TMDB_PROXY_ADDRESS/$path") {
            parameter("language", deviceLanguageTag())
            addParameters()
        }

    private suspend fun successfulReplyText(response: HttpResponse): String {
        if (response.status == HttpStatusCode.TooManyRequests) throw TmdbTooManyRequestsException()
        if (!response.status.isSuccess()) throw TmdbRequestFailedException(response.status)
        return response.bodyAsText()
    }

    private fun deviceLanguageTag(): String {
        val locale = Locale.current
        return if (locale.region.isBlank()) locale.language else "${locale.language}-${locale.region}"
    }
}
