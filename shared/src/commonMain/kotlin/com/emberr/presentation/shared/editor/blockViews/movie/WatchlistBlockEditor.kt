package com.emberr.presentation.shared.editor.blockViews.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieSearchResult
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.WatchState
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.model.titleKey
import com.emberr.domain.model.withDeleted
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.movie.newViewWithLayout
import com.emberr.domain.movie.viewsInOrderOf
import com.emberr.domain.movie.withViewAdded
import com.emberr.domain.movie.withViewAddedAfter
import com.emberr.domain.movie.withViewChanged
import com.emberr.domain.movie.withViewRemoved
import com.emberr.domain.movie.withWatchStateOnAllCopiesOf
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.table.DeviceTableLayoutStore
import com.emberr.domain.util.media.WebImageFetcher
import com.emberr.domain.util.sync.SyncCoordinator
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WatchlistBlockEditor(
    private val repository: NoteRepository,
    private val titleSource: MovieTitleSource,
    private val webImageFetcher: WebImageFetcher,
    private val appScope: CoroutineScope,
    private val deviceTableLayoutStore: DeviceTableLayoutStore,
    private val changeWatchlistBlock: (blockId: String, change: (WatchlistBlock) -> WatchlistBlock) -> Unit
) {
    private val movieEditorsByWatchlistId = mutableMapOf<String, MovieBlockEditor>()

    private val _addingToWatchlistIds = MutableStateFlow<Set<String>>(emptySet())
    val addingToWatchlistIds: StateFlow<Set<String>> = _addingToWatchlistIds.asStateFlow()

    private val _lastAddedBlockId = MutableStateFlow<String?>(null)
    val lastAddedBlockId: StateFlow<String?> = _lastAddedBlockId.asStateFlow()

    suspend fun search(query: String): List<MovieSearchResult> = titleSource.search(query)

    fun loadSuggestionsEarly() {
        appScope.launch { runCatching { titleSource.search("") } }
    }

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
            MovieBlockEditor(titleSource, webImageFetcher, appScope, hostFor(watchlistNoteId))
        }

    fun rename(blockId: String, title: String) {
        changeWatchlistBlock(blockId) { block -> block.copy(title = title) }
    }

    fun showView(blockId: String, viewId: String) {
        deviceTableLayoutStore.setActiveViewId(blockId, viewId)
    }

    fun reorderViews(blockId: String, viewIds: List<String>) {
        deviceTableLayoutStore.setViewOrder(blockId, viewIds)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addView(block: WatchlistBlock, layout: WatchlistView) {
        val newView = block.newViewWithLayout(layout, Uuid.random().toString())
        val viewIdsHere = block.viewsInOrderOf(deviceTableLayoutStore.currentLayoutOf(block.id)).map { it.id }
        changeWatchlistBlock(block.id) { it.withViewAdded(newView) }
        deviceTableLayoutStore.setViewOrder(block.id, viewIdsHere + newView.id)
        deviceTableLayoutStore.setActiveViewId(block.id, newView.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun duplicateView(block: WatchlistBlock, view: WatchlistViewSettings) {
        val copy = view.copy(id = Uuid.random().toString(), name = "${view.name} copy")
        val viewIdsHere = block.viewsInOrderOf(deviceTableLayoutStore.currentLayoutOf(block.id)).map { it.id }
        val originalIndex = viewIdsHere.indexOf(view.id)
        val viewIdsWithCopy = viewIdsHere.toMutableList().apply { add(originalIndex + 1, copy.id) }
        changeWatchlistBlock(block.id) { it.withViewAddedAfter(copy, view.id) }
        deviceTableLayoutStore.setViewOrder(block.id, viewIdsWithCopy)
        deviceTableLayoutStore.setActiveViewId(block.id, copy.id)
    }

    fun removeView(blockId: String, viewId: String) {
        changeWatchlistBlock(blockId) { it.withViewRemoved(viewId) }
    }

    fun changeView(blockId: String, viewId: String, change: (WatchlistViewSettings) -> WatchlistViewSettings) {
        changeWatchlistBlock(blockId) { it.withViewChanged(viewId, change) }
    }

    fun forgetLastAddedTitle() {
        _lastAddedBlockId.value = null
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addTitle(watchlistNoteId: String, result: MovieSearchResult) {
        if (watchlistNoteId in _addingToWatchlistIds.value) return
        _addingToWatchlistIds.update { it + watchlistNoteId }
        appScope.launch {
            try {
                titleSource.loadDetailsWithImages(result.tmdbId, result.kind, webImageFetcher) { details ->
                    val linkedState = newestWatchState(watchlistNoteId, details)
                    val now = System.currentTimeMillis()
                    val newTitle = MovieBlock(id = Uuid.random().toString(), updatedAt = now)
                        .withChosenTitle(details, linkedState, now)
                    _lastAddedBlockId.value = newTitle.id
                    changeTitles(watchlistNoteId) { blocks, _ -> blocks + newTitle }
                }
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
                changeTitles(watchlistNoteId) { blocks, now -> blocks.withWatchStateOnAllCopiesOf(blockId, state, now) }
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
