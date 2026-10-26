package com.emberr.domain.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchlistFilter
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField
import com.emberr.domain.model.WatchlistView
import com.emberr.domain.model.WatchlistViewSettings
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class WatchlistFilterTest {

    private val today = LocalDate(2026, 10, 26)

    private fun title(
        id: String,
        genres: List<String> = emptyList(),
        releaseDate: LocalDate? = null,
        ratingHalfStars: Int? = null
    ) = MovieBlock(
        id = id,
        details = MovieDetails(tmdbId = id.hashCode().toLong(), kind = MovieKind.MOVIE, title = id, genres = genres, releaseDate = releaseDate),
        ratingHalfStars = ratingHalfStars
    )

    private fun List<MovieBlock>.idsShownWith(vararg filters: WatchlistFilter): List<String> =
        shownIn(WatchlistViewSettings(id = "view", name = "List", layout = WatchlistView.LIST, filters = filters.toList()), today = today)
            .map { it.id }
            .sorted()

    @Test
    fun aMissingValueFailsExceptForHasNoneOfAndNotRatedYet() {
        val titles = listOf(
            title("no-genres-unrated"),
            title("horror-rated", genres = listOf("Horror"), ratingHalfStars = 8),
            title("drama-rated", genres = listOf("Drama"), ratingHalfStars = 5)
        )
        fun filterOf(field: WatchlistFilterField, condition: WatchlistFilterCondition, number: Double? = null) =
            WatchlistFilter(id = field.name, field = field, condition = condition, picks = listOf("Horror"), number = number)

        assertEquals(listOf("horror-rated"), titles.idsShownWith(filterOf(WatchlistFilterField.GENRE, WatchlistFilterCondition.HAS_ANY_OF)))
        assertEquals(
            listOf("drama-rated", "no-genres-unrated"),
            titles.idsShownWith(filterOf(WatchlistFilterField.GENRE, WatchlistFilterCondition.HAS_NONE_OF))
        )
        assertEquals(
            listOf("no-genres-unrated"),
            titles.idsShownWith(filterOf(WatchlistFilterField.YOUR_RATING, WatchlistFilterCondition.NOT_RATED_YET))
        )
        assertEquals(
            listOf("horror-rated"),
            titles.idsShownWith(filterOf(WatchlistFilterField.YOUR_RATING, WatchlistFilterCondition.AT_LEAST, number = 3.5))
        )
    }

    @Test
    fun releaseDateRangesIncludeBothEndsAndLeaveOutTitlesWithoutADate() {
        val titles = listOf(
            title("no-date"),
            title("first-day", releaseDate = LocalDate(2021, 10, 26)),
            title("day-before", releaseDate = LocalDate(2021, 10, 25)),
            title("today", releaseDate = today),
            title("next-year", releaseDate = LocalDate(2027, 1, 1))
        )

        assertEquals(
            listOf("first-day", "today"),
            titles.idsShownWith(
                WatchlistFilter(
                    id = "past",
                    field = WatchlistFilterField.RELEASE_DATE,
                    condition = WatchlistFilterCondition.WITHIN_PAST_YEARS,
                    number = 5.0
                )
            )
        )
        assertEquals(
            listOf("day-before", "first-day"),
            titles.idsShownWith(
                WatchlistFilter(
                    id = "between",
                    field = WatchlistFilterField.RELEASE_DATE,
                    condition = WatchlistFilterCondition.BETWEEN,
                    date = LocalDate(2021, 10, 25),
                    endDate = LocalDate(2021, 10, 26)
                )
            )
        )
    }
}
