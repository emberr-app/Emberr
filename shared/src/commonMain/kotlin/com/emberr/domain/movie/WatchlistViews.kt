package com.emberr.domain.movie

import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistFilterField
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.table.DeviceTableLayout

const val DEFAULT_WATCHLIST_VIEW_ID = "default-list"

fun WatchlistBlock.allViews(): List<WatchlistViewSettings> =
    views.ifEmpty {
        listOf(WatchlistViewSettings(id = DEFAULT_WATCHLIST_VIEW_ID, name = WatchlistView.LIST.label, layout = WatchlistView.LIST))
    }

fun WatchlistBlock.viewsInOrderOf(deviceLayout: DeviceTableLayout): List<WatchlistViewSettings> {
    val viewsInBlockOrder = allViews()
    val viewsById = viewsInBlockOrder.associateBy { it.id }
    val viewsInSavedOrder = deviceLayout.viewOrderIds.distinct().mapNotNull(viewsById::get)
    val savedViewIds = viewsInSavedOrder.map { it.id }.toSet()
    return viewsInSavedOrder + viewsInBlockOrder.filter { it.id !in savedViewIds }
}

fun WatchlistBlock.openViewOn(deviceLayout: DeviceTableLayout): WatchlistViewSettings {
    val orderedViews = viewsInOrderOf(deviceLayout)
    return orderedViews.firstOrNull { it.id == deviceLayout.activeViewId } ?: orderedViews.first()
}

fun WatchlistBlock.newViewWithLayout(layout: WatchlistView, id: String): WatchlistViewSettings {
    val takenNames = allViews().map { it.name }.toSet()
    val name = generateSequence(1) { it + 1 }
        .map { number -> if (number == 1) layout.label else "${layout.label} $number" }
        .first { it !in takenNames }
    return WatchlistViewSettings(id = id, name = name, layout = layout)
}

fun WatchlistBlock.withViewAdded(view: WatchlistViewSettings): WatchlistBlock = copy(views = allViews() + view)

fun WatchlistBlock.withViewAddedAfter(view: WatchlistViewSettings, afterViewId: String): WatchlistBlock {
    val viewsNow = allViews()
    val insertAt = viewsNow.indexOfFirst { it.id == afterViewId }.takeIf { it >= 0 }?.plus(1) ?: viewsNow.size
    return copy(views = viewsNow.toMutableList().apply { add(insertAt, view) })
}

fun WatchlistBlock.withViewRemoved(viewId: String): WatchlistBlock {
    val remainingViews = allViews().filter { it.id != viewId }
    return if (remainingViews.isEmpty()) this else copy(views = remainingViews)
}

fun WatchlistBlock.withViewChanged(viewId: String, change: (WatchlistViewSettings) -> WatchlistViewSettings): WatchlistBlock =
    copy(views = allViews().map { view -> if (view.id == viewId) change(view) else view })

fun WatchlistViewSettings.boardStatuses(): List<WatchStatus> {
    val statusFilters = filters.filter { it.field == WatchlistFilterField.STATUS && it.picks.isNotEmpty() }
    return WatchStatus.entries.filter { status -> statusFilters.all { status.name in it.picks } }
}
