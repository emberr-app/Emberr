package com.emberr.presentation.shared.editor.blockViews.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.WatchState
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.titleKey
import com.emberr.domain.model.withDeleted
import com.emberr.domain.model.withWatchState
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.sync.SyncCoordinator
import com.emberr.domain.util.system.showNativeToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class WatchlistBlockEditor(
    private val repository: NoteRepository,
    private val titleSource: MovieTitleSource,
    private val appScope: CoroutineScope,
    private val changeWatchlistBlock: (blockId: String, change: (WatchlistBlock) -> WatchlistBlock) -> Unit
) {
    private val movieEditorsByWatchlistId = mutableMapOf<String, MovieBlockEditor>()

    private val _addingToWatchlistIds = MutableStateFlow<Set<String>>(emptySet())
    val addingToWatchlistIds: StateFlow<Set<String>> = _addingToWatchlistIds.asStateFlow()

    private val _lastAddedBlockId = MutableStateFlow<String?>(null)
    val lastAddedBlockId: StateFlow<String?> = _lastAddedBlockId.asStateFlow()

    val canSearch: Boolean get() = titleSource.canSearch

    suspend fun search(query: String): List<MovieSearchResult> = titleSource.search(query)

    fun titlesOf(watchlistNoteId: String): Flow<List<MovieBlock>> =
        repository.observeNoteContent(watchlistNoteId)
            .onStart { repository.getNoteContent(watchlistNoteId) }
            .map { content ->
                content?.blocks.orEmpty()
                    .filterIsInstance<MovieBlock>()
                    .filter { !it.isDeleted && it.details != null }
            }

    fun movieEditorFor(watchlistNoteId: String): MovieBlockEditor =
        movieEditorsByWatchlistId.getOrPut(watchlistNoteId) {
            MovieBlockEditor(titleSource, appScope, hostFor(watchlistNoteId))
        }

    fun rename(blockId: String, title: String) {
        changeWatchlistBlock(blockId) { block -> block.copy(title = title) }
    }

    fun changeSettings(blockId: String, change: (WatchlistBlock) -> WatchlistBlock) {
        changeWatchlistBlock(blockId, change)
    }

    fun forgetLastAddedTitle() {
        _lastAddedBlockId.value = null
    }

    fun addTitle(watchlistNoteId: String, result: MovieSearchResult) {
        if (watchlistNoteId in _addingToWatchlistIds.value) return
        _addingToWatchlistIds.update { it + watchlistNoteId }
        appScope.launch {
            try {
                val details = titleSource.loadDetails(result.tmdbId, result.kind)
                if (details == null) {
                    showNativeToast("couldn't find this title")
                    return@launch
                }
                val linkedState = newestWatchState(watchlistNoteId, details)
                val now = System.currentTimeMillis()
                val newTitle = MovieBlock(
                    id = UUID.randomUUID().toString(),
                    details = details,
                    watchStatus = linkedState?.status ?: WatchStatus.NOT_WATCHED,
                    ratingHalfStars = linkedState?.ratingHalfStars,
                    watchStateChangedAt = linkedState?.changedAt ?: 0L,
                    addedAt = now,
                    updatedAt = now
                )
                _lastAddedBlockId.value = newTitle.id
                changeTitles(watchlistNoteId) { blocks, _ -> blocks + newTitle }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                showNativeToast("couldn't load this title, check your internet")
            } finally {
                _addingToWatchlistIds.update { it - watchlistNoteId }
            }
        }
    }

    fun removeTitle(watchlistNoteId: String, blockId: String) {
        removeTitles(watchlistNoteId, setOf(blockId))
    }

    fun removeTitles(watchlistNoteId: String, blockIds: Set<String>) {
        appScope.launch {
            changeTitles(watchlistNoteId) { blocks, now ->
                blocks.map { block -> if (block.id in blockIds) block.withDeleted(true, now) else block }
            }
        }
    }

    private fun hostFor(watchlistNoteId: String): MovieBlockHost = object : MovieBlockHost {
        override fun changeMovieBlock(blockId: String, change: (MovieBlock) -> MovieBlock) {
            appScope.launch {
                changeTitles(watchlistNoteId) { blocks, now ->
                    blocks.map { block -> if (block.id == blockId && block is MovieBlock) change(block).copy(updatedAt = now) else block }
                }
            }
        }

        override fun applyWatchStateToTitle(blockId: String, state: WatchState) {
            appScope.launch {
                changeTitles(watchlistNoteId) { blocks, now ->
                    val titleKey = (blocks.firstOrNull { it.id == blockId } as? MovieBlock)?.titleKey()
                    blocks.map { block ->
                        val isSameTitle = block is MovieBlock && !block.isDeleted && titleKey != null && block.titleKey() == titleKey
                        if (block is MovieBlock && (block.id == blockId || isSameTitle)) block.withWatchState(state, now) else block
                    }
                }
            }
        }

        override suspend fun newestWatchStateInThisSpace(details: MovieDetails): WatchState? =
            newestWatchState(watchlistNoteId, details)
    }

    private suspend fun newestWatchState(watchlistNoteId: String, details: MovieDetails): WatchState? {
        val spaceId = repository.getNoteById(watchlistNoteId)?.spaceId ?: return null
        return repository.findNewestWatchState(spaceId, details.titleKey())
    }

    private suspend fun changeTitles(watchlistNoteId: String, change: (blocks: List<NoteBlock>, now: Long) -> List<NoteBlock>) {
        try {
            SyncCoordinator.mutex.withLock {
                val metadata = repository.getNoteById(watchlistNoteId) ?: return@withLock
                val blocks = repository.getNoteContent(watchlistNoteId)?.blocks.orEmpty()
                val updatedBlocks = change(blocks, System.currentTimeMillis())
                if (updatedBlocks != blocks) repository.saveNote(metadata, NoteContent(blocks = updatedBlocks))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cause: Exception) {
            cause.printStackTrace()
        }
    }
}
