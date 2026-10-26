package com.emberr.domain.movie

import com.emberr.domain.model.WatchlistBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchlistCleanupTest {

    private val now = 100L * 24 * 60 * 60 * 1000
    private val longAgo = now - WatchlistCleanup.DELETED_BLOCK_GRACE_PERIOD_MILLIS
    private val yesterday = now - 24L * 60 * 60 * 1000

    @Test
    fun aWatchlistIsDeletedOnlyWhenEveryBlockShowingItWasRemovedLongEnoughAgo() {
        val blocks = listOf(
            WatchlistBlock(id = "kept-copy", watchlistNoteId = "still-shown", isDeleted = true, updatedAt = longAgo),
            WatchlistBlock(id = "live", watchlistNoteId = "still-shown", isDeleted = false, updatedAt = longAgo),
            WatchlistBlock(id = "removed-yesterday", watchlistNoteId = "recently-removed", isDeleted = true, updatedAt = yesterday),
            WatchlistBlock(id = "removed-long-ago", watchlistNoteId = "removed-long-ago", isDeleted = true, updatedAt = longAgo)
        )

        assertEquals(setOf("removed-long-ago"), WatchlistCleanup.watchlistsToDelete(blocks, now))
    }
}
