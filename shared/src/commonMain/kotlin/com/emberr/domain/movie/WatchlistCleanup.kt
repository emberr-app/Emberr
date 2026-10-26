package com.emberr.domain.movie

import com.emberr.data.local.room.dao.BlockDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.domain.media.LocalMediaGcLog
import com.emberr.domain.model.NoteBlockSerializer
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

object WatchlistCleanup {
    const val WATCHLIST_NOTE_ID_FIELD = "watchlistNoteId"
    const val DELETED_BLOCK_GRACE_PERIOD_MILLIS = 30L * 24 * 60 * 60 * 1000

    fun watchlistsToDelete(watchlistBlocks: List<WatchlistBlock>, now: Long): Set<String> =
        watchlistBlocks
            .groupBy { it.watchlistNoteId }
            .filterValues { blocksShowingWatchlist ->
                blocksShowingWatchlist.all { block -> block.isDeleted && now - block.updatedAt >= DELETED_BLOCK_GRACE_PERIOD_MILLIS }
            }
            .keys
}

class WatchlistCleaner(
    private val noteDao: NoteDao,
    private val blockDao: BlockDao,
    private val noteRepository: NoteRepository
) {
    private val blockJson = Json { ignoreUnknownKeys = true }

    suspend fun deleteWatchlistsOfRemovedBlocks() = withContext(Dispatchers.IO) {
        try {
            val watchlistBlocks = blockDao.findBlocksContainingIncludingDeleted(WatchlistCleanup.WATCHLIST_NOTE_ID_FIELD)
                .mapNotNull { entity ->
                    runCatching { blockJson.decodeFromString(NoteBlockSerializer, entity.blockDataJson) }.getOrNull() as? WatchlistBlock
                }
            if (watchlistBlocks.isEmpty()) return@withContext
            val watchlistIdsToDelete = WatchlistCleanup.watchlistsToDelete(watchlistBlocks, now = System.currentTimeMillis())
                .filter { watchlistNoteId -> noteDao.getNoteById(watchlistNoteId)?.isSubNote == true }
            watchlistIdsToDelete.forEach { watchlistNoteId -> noteRepository.deleteNote(watchlistNoteId, "") }
            LocalMediaGcLog.d("deleteWatchlistsOfRemovedBlocks: deleted ${watchlistIdsToDelete.size} watchlist(s)")
        } catch (e: Exception) {
            LocalMediaGcLog.e("deleteWatchlistsOfRemovedBlocks: failed with ${e::class.simpleName}: ${e.message}", e)
        }
    }
}
