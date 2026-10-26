package com.emberr.presentation.shared.editor.blockViews.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.WatchState
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.movie.withImagesSavedBy
import com.emberr.domain.util.media.WebImageFetcher
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface MovieBlockHost {
    fun changeMovieBlock(blockId: String, change: (MovieBlock) -> MovieBlock)
    fun applyWatchStateToTitle(blockId: String, state: WatchState)
    suspend fun newestWatchStateInThisSpace(details: MovieDetails): WatchState?
}

class MovieBlockEditor(
    private val titleSource: MovieTitleSource,
    private val webImageFetcher: WebImageFetcher,
    private val scope: CoroutineScope,
    private val host: MovieBlockHost
) {
    private val _loadingBlockIds = MutableStateFlow<Set<String>>(emptySet())
    val loadingBlockIds: StateFlow<Set<String>> = _loadingBlockIds.asStateFlow()

    suspend fun search(query: String): List<MovieSearchResult> = titleSource.search(query)

    fun chooseTitle(blockId: String, result: MovieSearchResult) {
        loadDetails(blockId, result.tmdbId, result.kind) { details ->
            val linkedState = host.newestWatchStateInThisSpace(details)
            val now = System.currentTimeMillis()
            host.changeMovieBlock(blockId) { block ->
                block.copy(
                    details = details,
                    addedAt = now,
                    watchStatus = linkedState?.status ?: WatchStatus.NOT_WATCHED,
                    ratingHalfStars = linkedState?.ratingHalfStars,
                    watchStateChangedAt = linkedState?.changedAt ?: 0L
                )
            }
        }
    }

    fun refreshDetails(blockId: String, currentDetails: MovieDetails) {
        loadDetails(blockId, currentDetails.tmdbId, currentDetails.kind) { details ->
            host.changeMovieBlock(blockId) { block -> block.copy(details = details) }
        }
    }

    fun changeTitle(blockId: String) {
        host.changeMovieBlock(blockId) { block ->
            block.copy(
                details = null,
                watchStatus = WatchStatus.NOT_WATCHED,
                ratingHalfStars = null,
                watchStateChangedAt = 0L,
                addedAt = 0L
            )
        }
    }

    fun setWatchStatus(block: MovieBlock, status: WatchStatus) {
        host.applyWatchStateToTitle(block.id, WatchState(status, block.ratingHalfStars, System.currentTimeMillis()))
    }

    fun setRating(block: MovieBlock, ratingHalfStars: Int?) {
        host.applyWatchStateToTitle(block.id, WatchState(block.watchStatus, ratingHalfStars, System.currentTimeMillis()))
    }

    fun showNextWatchStatus(block: MovieBlock) {
        val nextStatus = WatchStatus.entries[(block.watchStatus.ordinal + 1) % WatchStatus.entries.size]
        setWatchStatus(block, nextStatus)
    }

    private fun loadDetails(
        blockId: String,
        tmdbId: Long,
        kind: MovieKind,
        useDetails: suspend (MovieDetails) -> Unit
    ) {
        if (blockId in _loadingBlockIds.value) return
        _loadingBlockIds.update { it + blockId }
        scope.launch {
            try {
                val details = titleSource.loadDetails(tmdbId, kind)?.withImagesSavedBy(webImageFetcher)
                if (details == null) {
                    showNativeToast("couldn't find this title")
                } else {
                    useDetails(details)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                showNativeToast("couldn't load this title, check your internet")
            } finally {
                _loadingBlockIds.update { it - blockId }
            }
        }
    }
}
