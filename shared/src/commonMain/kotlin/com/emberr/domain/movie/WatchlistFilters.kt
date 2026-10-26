package com.emberr.domain.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.WatchlistFilter
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField

val WatchlistFilterField.conditions: List<WatchlistFilterCondition>
    get() = when (this) {
        WatchlistFilterField.TYPE -> listOf(WatchlistFilterCondition.IS)
        WatchlistFilterField.STATUS -> listOf(WatchlistFilterCondition.IS_ANY_OF)
        WatchlistFilterField.GENRE -> listOf(WatchlistFilterCondition.HAS_ANY_OF, WatchlistFilterCondition.HAS_NONE_OF)
        WatchlistFilterField.RELEASE_DATE -> listOf(
            WatchlistFilterCondition.BEFORE,
            WatchlistFilterCondition.ON_OR_AFTER,
            WatchlistFilterCondition.BETWEEN,
            WatchlistFilterCondition.WITHIN_PAST_YEARS
        )
        WatchlistFilterField.DATE_ADDED -> listOf(
            WatchlistFilterCondition.BEFORE,
            WatchlistFilterCondition.AFTER,
            WatchlistFilterCondition.WITHIN_PAST_DAYS
        )
        WatchlistFilterField.YOUR_RATING -> listOf(
            WatchlistFilterCondition.AT_LEAST,
            WatchlistFilterCondition.AT_MOST,
            WatchlistFilterCondition.NOT_RATED_YET
        )
        WatchlistFilterField.TMDB_RATING -> listOf(WatchlistFilterCondition.AT_LEAST, WatchlistFilterCondition.AT_MOST)
        WatchlistFilterField.RUNTIME -> listOf(WatchlistFilterCondition.SHORTER_THAN, WatchlistFilterCondition.LONGER_THAN)
        WatchlistFilterField.SHOW_STATUS -> listOf(WatchlistFilterCondition.STILL_AIRING, WatchlistFilterCondition.ENDED)
    }

fun newWatchlistFilter(id: String, field: WatchlistFilterField): WatchlistFilter {
    val firstCondition = field.conditions.first()
    return WatchlistFilter(id = id, field = field, condition = firstCondition).withCondition(firstCondition)
}

fun WatchlistFilter.withCondition(newCondition: WatchlistFilterCondition): WatchlistFilter {
    val startingNumber = when (newCondition) {
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> 7.0
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> 5.0
        else -> null
    }
    return copy(condition = newCondition, number = number ?: startingNumber)
}

fun List<MovieBlock>.genreChoices(): List<String> =
    mapNotNull { it.details }
        .flatMap { it.genres }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }

fun WatchlistFilter.isUsable(): Boolean = when (condition) {
    WatchlistFilterCondition.IS,
    WatchlistFilterCondition.IS_ANY_OF,
    WatchlistFilterCondition.HAS_ANY_OF,
    WatchlistFilterCondition.HAS_NONE_OF -> picks.isNotEmpty()
    WatchlistFilterCondition.BEFORE,
    WatchlistFilterCondition.AFTER,
    WatchlistFilterCondition.ON_OR_AFTER -> date != null
    WatchlistFilterCondition.BETWEEN -> date != null && endDate != null
    WatchlistFilterCondition.WITHIN_PAST_DAYS,
    WatchlistFilterCondition.WITHIN_PAST_YEARS,
    WatchlistFilterCondition.AT_LEAST,
    WatchlistFilterCondition.AT_MOST,
    WatchlistFilterCondition.SHORTER_THAN,
    WatchlistFilterCondition.LONGER_THAN -> number != null
    WatchlistFilterCondition.NOT_RATED_YET,
    WatchlistFilterCondition.STILL_AIRING,
    WatchlistFilterCondition.ENDED -> true
}
