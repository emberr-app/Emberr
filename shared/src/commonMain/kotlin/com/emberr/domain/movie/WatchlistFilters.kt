package com.emberr.domain.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchlistFilter
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal val StillAiringShowStatuses = setOf("Returning Series", "In Production", "Planned", "Pilot")
private val EndedShowStatuses = setOf("Ended", "Canceled")
private const val STARTING_PAST_DAYS = 7.0
private const val STARTING_PAST_YEARS = 5.0

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
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> STARTING_PAST_DAYS
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> STARTING_PAST_YEARS
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

internal fun MovieBlock.passes(filter: WatchlistFilter, today: LocalDate, timeZone: TimeZone): Boolean {
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
