package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.runtime.Composable
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField
import com.emberr.domain.model.WatchlistSortKey
import com.emberr.domain.model.WatchlistView
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.movie_date_added
import emberr.shared.generated.resources.movie_filter_condition_after
import emberr.shared.generated.resources.movie_filter_condition_at_least
import emberr.shared.generated.resources.movie_filter_condition_at_most
import emberr.shared.generated.resources.movie_filter_condition_before
import emberr.shared.generated.resources.movie_filter_condition_between
import emberr.shared.generated.resources.movie_filter_condition_ended
import emberr.shared.generated.resources.movie_filter_condition_has_any_of
import emberr.shared.generated.resources.movie_filter_condition_has_none_of
import emberr.shared.generated.resources.movie_filter_condition_is
import emberr.shared.generated.resources.movie_filter_condition_is_any_of
import emberr.shared.generated.resources.movie_filter_condition_longer_than
import emberr.shared.generated.resources.movie_filter_condition_not_rated_yet
import emberr.shared.generated.resources.movie_filter_condition_on_or_after
import emberr.shared.generated.resources.movie_filter_condition_shorter_than
import emberr.shared.generated.resources.movie_filter_condition_still_airing
import emberr.shared.generated.resources.movie_filter_condition_within_the_past
import emberr.shared.generated.resources.movie_genre
import emberr.shared.generated.resources.movie_filter_release_date
import emberr.shared.generated.resources.movie_filter_show_status
import emberr.shared.generated.resources.movie_filter_type
import emberr.shared.generated.resources.movie_kind_movie
import emberr.shared.generated.resources.movie_kind_movies
import emberr.shared.generated.resources.movie_kind_show
import emberr.shared.generated.resources.movie_kind_shows
import emberr.shared.generated.resources.movie_runtime
import emberr.shared.generated.resources.movie_sort_title
import emberr.shared.generated.resources.movie_sort_year
import emberr.shared.generated.resources.movie_status
import emberr.shared.generated.resources.movie_tmdb_rating
import emberr.shared.generated.resources.movie_watch_status_not_watched
import emberr.shared.generated.resources.movie_watch_status_watched
import emberr.shared.generated.resources.movie_watch_status_watching
import emberr.shared.generated.resources.movie_watchlist_view_board
import emberr.shared.generated.resources.movie_watchlist_view_gallery
import emberr.shared.generated.resources.movie_watchlist_view_list
import emberr.shared.generated.resources.movie_your_rating
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun MovieKind.displayName(): String = when (this) {
    MovieKind.MOVIE -> stringResource(Res.string.movie_kind_movie)
    MovieKind.SHOW -> stringResource(Res.string.movie_kind_show)
}

@Composable
internal fun MovieKind.pluralDisplayName(): String = when (this) {
    MovieKind.MOVIE -> stringResource(Res.string.movie_kind_movies)
    MovieKind.SHOW -> stringResource(Res.string.movie_kind_shows)
}

internal val WatchStatus.nameResource: StringResource
    get() = when (this) {
        WatchStatus.NOT_WATCHED -> Res.string.movie_watch_status_not_watched
        WatchStatus.WATCHING -> Res.string.movie_watch_status_watching
        WatchStatus.WATCHED -> Res.string.movie_watch_status_watched
    }

@Composable
internal fun WatchStatus.displayName(): String = stringResource(nameResource)

@Composable
internal fun WatchlistView.displayName(): String = when (this) {
    WatchlistView.LIST -> stringResource(Res.string.movie_watchlist_view_list)
    WatchlistView.BOARD -> stringResource(Res.string.movie_watchlist_view_board)
    WatchlistView.GALLERY -> stringResource(Res.string.movie_watchlist_view_gallery)
}

@Composable
internal fun WatchlistSortKey.displayName(): String = when (this) {
    WatchlistSortKey.STATUS -> stringResource(Res.string.movie_status)
    WatchlistSortKey.YOUR_RATING -> stringResource(Res.string.movie_your_rating)
    WatchlistSortKey.TMDB_RATING -> stringResource(Res.string.movie_tmdb_rating)
    WatchlistSortKey.YEAR -> stringResource(Res.string.movie_sort_year)
    WatchlistSortKey.DATE_ADDED -> stringResource(Res.string.movie_date_added)
    WatchlistSortKey.TITLE -> stringResource(Res.string.movie_sort_title)
}

@Composable
internal fun WatchlistFilterField.displayName(): String = when (this) {
    WatchlistFilterField.TYPE -> stringResource(Res.string.movie_filter_type)
    WatchlistFilterField.STATUS -> stringResource(Res.string.movie_status)
    WatchlistFilterField.GENRE -> stringResource(Res.string.movie_genre)
    WatchlistFilterField.RELEASE_DATE -> stringResource(Res.string.movie_filter_release_date)
    WatchlistFilterField.DATE_ADDED -> stringResource(Res.string.movie_date_added)
    WatchlistFilterField.YOUR_RATING -> stringResource(Res.string.movie_your_rating)
    WatchlistFilterField.TMDB_RATING -> stringResource(Res.string.movie_tmdb_rating)
    WatchlistFilterField.RUNTIME -> stringResource(Res.string.movie_runtime)
    WatchlistFilterField.SHOW_STATUS -> stringResource(Res.string.movie_filter_show_status)
}

@Composable
internal fun WatchlistFilterCondition.displayName(): String = when (this) {
    WatchlistFilterCondition.IS -> stringResource(Res.string.movie_filter_condition_is)
    WatchlistFilterCondition.IS_ANY_OF -> stringResource(Res.string.movie_filter_condition_is_any_of)
    WatchlistFilterCondition.HAS_ANY_OF -> stringResource(Res.string.movie_filter_condition_has_any_of)
    WatchlistFilterCondition.HAS_NONE_OF -> stringResource(Res.string.movie_filter_condition_has_none_of)
    WatchlistFilterCondition.BEFORE -> stringResource(Res.string.movie_filter_condition_before)
    WatchlistFilterCondition.AFTER -> stringResource(Res.string.movie_filter_condition_after)
    WatchlistFilterCondition.ON_OR_AFTER -> stringResource(Res.string.movie_filter_condition_on_or_after)
    WatchlistFilterCondition.BETWEEN -> stringResource(Res.string.movie_filter_condition_between)
    WatchlistFilterCondition.WITHIN_PAST_DAYS,
    WatchlistFilterCondition.WITHIN_PAST_YEARS -> stringResource(Res.string.movie_filter_condition_within_the_past)
    WatchlistFilterCondition.AT_LEAST -> stringResource(Res.string.movie_filter_condition_at_least)
    WatchlistFilterCondition.AT_MOST -> stringResource(Res.string.movie_filter_condition_at_most)
    WatchlistFilterCondition.NOT_RATED_YET -> stringResource(Res.string.movie_filter_condition_not_rated_yet)
    WatchlistFilterCondition.SHORTER_THAN -> stringResource(Res.string.movie_filter_condition_shorter_than)
    WatchlistFilterCondition.LONGER_THAN -> stringResource(Res.string.movie_filter_condition_longer_than)
    WatchlistFilterCondition.STILL_AIRING -> stringResource(Res.string.movie_filter_condition_still_airing)
    WatchlistFilterCondition.ENDED -> stringResource(Res.string.movie_filter_condition_ended)
}
