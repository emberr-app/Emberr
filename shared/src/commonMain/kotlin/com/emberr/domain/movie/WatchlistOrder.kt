package com.emberr.domain.movie

import com.emberr.domain.database.todayInThisTimeZone
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchlistFilter
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField
import com.emberr.domain.model.WatchlistSortKey
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.model.starsTextOf
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Instant

private val StillAiringShowStatuses = setOf("Returning Series", "In Production", "Planned", "Pilot")
private val EndedShowStatuses = setOf("Ended", "Canceled")

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

private fun WatchlistFilter.isUsable(): Boolean = when (condition) {
    WatchlistFilterCondition.IS,
    WatchlistFilterCondition.IS_ANY_OF,
    WatchlistFilterCondition.HAS_ANY_OF,
    WatchlistFilterCondition.HAS_NONE_OF,
    WatchlistFilterCondition.STREAMS_ON_ANY_OF -> picks.isNotEmpty()
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

private fun MovieBlock.passes(filter: WatchlistFilter, today: LocalDate, timeZone: TimeZone): Boolean {
    val details = details ?: return false
    return when (filter.field) {
        WatchlistFilterField.TYPE -> details.kind.name in filter.picks
        WatchlistFilterField.STATUS -> watchStatus.name in filter.picks
        WatchlistFilterField.GENRE -> details.genres.matchesPicksOf(filter)
        WatchlistFilterField.RELEASE_DATE -> details.releaseDate.matchesDateOf(filter, today)
        WatchlistFilterField.DATE_ADDED -> addedDateIn(timeZone).matchesDateOf(filter, today)
        WatchlistFilterField.YOUR_RATING ->
            if (filter.condition == WatchlistFilterCondition.NOT_RATED_YET) {
                ratingHalfStars == null
            } else {
                ratingHalfStars?.let { it / 2.0 }.matchesNumberOf(filter)
            }
        WatchlistFilterField.TMDB_RATING -> details.tmdbRating.matchesNumberOf(filter)
        WatchlistFilterField.RUNTIME -> details.runtimeMinutes?.toDouble().matchesNumberOf(filter)
        WatchlistFilterField.LANGUAGE -> details.language.isNotBlank() && details.language in filter.picks
        WatchlistFilterField.WHERE_TO_WATCH -> details.watchProviders?.stream.orEmpty().map { it.name }.matchesPicksOf(filter)
        WatchlistFilterField.SHOW_STATUS -> details.kind == MovieKind.SHOW && when (filter.condition) {
            WatchlistFilterCondition.STILL_AIRING -> details.productionStatus in StillAiringShowStatuses
            WatchlistFilterCondition.ENDED -> details.productionStatus in EndedShowStatuses
            else -> true
        }
    }
}

private fun MovieBlock.addedDateIn(timeZone: TimeZone): LocalDate? =
    addedAt.takeIf { it > 0L }?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).date }

private fun List<String>.matchesPicksOf(filter: WatchlistFilter): Boolean =
    if (filter.condition == WatchlistFilterCondition.HAS_NONE_OF) none { it in filter.picks } else any { it in filter.picks }

private fun LocalDate?.matchesDateOf(filter: WatchlistFilter, today: LocalDate): Boolean {
    if (this == null) return false
    val chosenDate = filter.date
    val chosenNumber = filter.number?.toInt()
    return when (filter.condition) {
        WatchlistFilterCondition.BEFORE -> chosenDate != null && this < chosenDate
        WatchlistFilterCondition.AFTER -> chosenDate != null && this > chosenDate
        WatchlistFilterCondition.ON_OR_AFTER -> chosenDate != null && this >= chosenDate
        WatchlistFilterCondition.BETWEEN -> chosenDate != null && filter.endDate != null && this in chosenDate..filter.endDate
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> chosenNumber != null && this in today.minus(chosenNumber, DateTimeUnit.DAY)..today
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> chosenNumber != null && this in today.minus(chosenNumber, DateTimeUnit.YEAR)..today
        else -> true
    }
}

private fun Double?.matchesNumberOf(filter: WatchlistFilter): Boolean {
    if (this == null) return false
    val chosenNumber = filter.number ?: return true
    return when (filter.condition) {
        WatchlistFilterCondition.AT_LEAST -> this >= chosenNumber
        WatchlistFilterCondition.AT_MOST -> this <= chosenNumber
        WatchlistFilterCondition.SHORTER_THAN -> this < chosenNumber
        WatchlistFilterCondition.LONGER_THAN -> this > chosenNumber
        else -> true
    }
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
