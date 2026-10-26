package com.emberr.domain.movie

import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.table.DeviceTableLayout
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchlistViewsTest {

    @Test
    fun deviceOrderKeepsSavedViewsFirstAddsNewOnesAtTheEndAndDropsDeletedOnes() {
        val watchlist = WatchlistBlock(
            id = "watchlist",
            watchlistNoteId = "watchlist-note",
            views = listOf("a", "b", "c", "d").map { id -> WatchlistViewSettings(id = id, name = id, layout = WatchlistView.LIST) }
        )
        val deviceLayout = DeviceTableLayout(activeViewId = "deleted", viewOrderIds = listOf("c", "deleted", "a"))

        assertEquals(listOf("c", "a", "b", "d"), watchlist.viewsInOrderOf(deviceLayout).map { it.id })
        assertEquals("c", watchlist.openViewOn(deviceLayout).id)
    }
}
