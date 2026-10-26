package com.emberr.domain.movie

import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.repository.NoteRepository

private const val WATCHLIST_HEADING_LEVEL = 3

suspend fun List<NoteBlock>.withWatchlistsExpanded(repository: NoteRepository): List<NoteBlock> =
    flatMap { block ->
        if (block is WatchlistBlock && !block.isDeleted) {
            val titles = repository.getNoteContent(block.watchlistNoteId)?.blocks.orEmpty()
                .filterIsInstance<MovieBlock>()
                .filter { !it.isDeleted && it.details != null }
            val heading = HeadingBlock(
                id = block.id,
                text = block.title.ifBlank { "Watchlist" },
                level = WATCHLIST_HEADING_LEVEL,
                indentationLevel = block.indentationLevel
            )
            listOf(heading) + titles.map { it.copy(indentationLevel = block.indentationLevel) }
        } else {
            listOf(block)
        }
    }
