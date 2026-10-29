package com.emberr.domain.movie

import com.emberr.domain.database.todayInThisTimeZone
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.WatchlistSortKey
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.model.starsTextOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.math.roundToInt

fun List<MovieBlock>.shownIn(
    view: WatchlistViewSettings,
    searchText: String = "",
    today: LocalDate = todayInThisTimeZone(),
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): List<MovieBlock> {
    val searchWords = searchText.lowercase().split(' ').filter { it.isNotBlank() }
    val usableFilters = view.filters.filter { it.isUsable() }
    val matchingTitles = filter { title ->
        title.details != null &&
            usableFilters.all { filter -> title.passes(filter, today, timeZone) } &&
            (searchWords.isEmpty() || title.searchableText().let { text -> searchWords.all { word -> word in text } })
    }
    val byChosenKey = compareByChosenKey(view.sortKey, view.sortsDescending)
    return matchingTitles.sortedWith(byChosenKey.thenBy { it.details?.title?.lowercase().orEmpty() })
}

private fun MovieBlock.searchableText(): String {
    val details = details ?: return ""
    val ratingText = details.tmdbRating?.let { ((it * 10).roundToInt() / 10.0).toString() }
    return listOfNotNull(
        details.title,
        details.yearText,
        details.tagline,
        details.overview,
        details.kind.label,
        details.ageRating,
        details.language,
        ratingText,
        ratingHalfStars?.let(::starsTextOf),
        watchStatus.label
    ).plus(details.genres)
        .plus(details.creditNames)
        .plus(details.countries)
        .plus(details.cast.flatMap { member -> listOf(member.name, member.character) })
        .joinToString(" ")
        .lowercase()
}

private fun compareByChosenKey(sortKey: WatchlistSortKey, sortsDescending: Boolean): Comparator<MovieBlock> {
    val valueOf: (MovieBlock) -> Comparable<*>? = when (sortKey) {
        WatchlistSortKey.STATUS -> { title -> title.watchStatus.ordinal }
        WatchlistSortKey.YOUR_RATING -> { title -> title.ratingHalfStars }
        WatchlistSortKey.TMDB_RATING -> { title -> title.details?.tmdbRating }
        WatchlistSortKey.YEAR -> { title -> title.details?.let { it.releaseDate?.year ?: it.yearText.take(4).toIntOrNull() } }
        WatchlistSortKey.DATE_ADDED -> { title -> title.addedAt }
        WatchlistSortKey.TITLE -> { title -> title.details?.title?.lowercase() }
    }
    val withValuesFirst = compareBy<MovieBlock> { valueOf(it) == null }
    val byValue = Comparator<MovieBlock> { first, second -> compareValues(valueOf(first), valueOf(second)) }
    return withValuesFirst.then(if (sortsDescending) byValue.reversed() else byValue)
}
