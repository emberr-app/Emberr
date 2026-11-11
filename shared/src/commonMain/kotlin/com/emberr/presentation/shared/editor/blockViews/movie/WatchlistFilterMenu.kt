package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.model.WatchlistFilter
import com.emberr.domain.model.WatchlistFilterCondition
import com.emberr.domain.model.WatchlistFilterField
import com.emberr.domain.model.WatchlistViewSettings
import com.emberr.domain.model.starsTextOf
import com.emberr.domain.movie.conditions
import com.emberr.domain.movie.genreChoices
import com.emberr.domain.movie.isUsable
import com.emberr.domain.movie.newWatchlistFilter
import com.emberr.domain.movie.withCondition
import com.emberr.domain.util.format.decimalText
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.MenuChoiceButton
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.SelectedOptionBackground
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.subtleFillColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.x
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import emberr.shared.generated.resources.movie_out_of_ten
import emberr.shared.generated.resources.movie_watchlist_add_filter
import emberr.shared.generated.resources.movie_watchlist_chip_added_after
import emberr.shared.generated.resources.movie_watchlist_chip_added_before
import emberr.shared.generated.resources.movie_watchlist_chip_added_in_past_days
import emberr.shared.generated.resources.movie_watchlist_chip_before
import emberr.shared.generated.resources.movie_watchlist_chip_between
import emberr.shared.generated.resources.movie_watchlist_chip_ended
import emberr.shared.generated.resources.movie_watchlist_chip_not
import emberr.shared.generated.resources.movie_watchlist_chip_not_rated_yet
import emberr.shared.generated.resources.movie_watchlist_chip_only
import emberr.shared.generated.resources.movie_watchlist_chip_or_later
import emberr.shared.generated.resources.movie_watchlist_chip_over
import emberr.shared.generated.resources.movie_watchlist_chip_past_years
import emberr.shared.generated.resources.movie_watchlist_chip_stars_and_up
import emberr.shared.generated.resources.movie_watchlist_chip_still_airing
import emberr.shared.generated.resources.movie_watchlist_chip_tmdb_at_least
import emberr.shared.generated.resources.movie_watchlist_chip_tmdb_up_to
import emberr.shared.generated.resources.movie_watchlist_chip_under
import emberr.shared.generated.resources.movie_watchlist_chip_up_to_stars
import emberr.shared.generated.resources.movie_watchlist_date_from
import emberr.shared.generated.resources.movie_watchlist_date_to
import emberr.shared.generated.resources.movie_watchlist_filter
import emberr.shared.generated.resources.movie_watchlist_filter_by
import emberr.shared.generated.resources.movie_watchlist_no_genres_yet
import emberr.shared.generated.resources.movie_watchlist_number_of_days
import emberr.shared.generated.resources.movie_watchlist_number_of_minutes
import emberr.shared.generated.resources.movie_watchlist_number_of_years
import emberr.shared.generated.resources.movie_watchlist_pick_a_date
import emberr.shared.generated.resources.movie_watchlist_pick_options
import emberr.shared.generated.resources.movie_watchlist_remove_filter
import emberr.shared.generated.resources.movie_watchlist_unit_days
import emberr.shared.generated.resources.movie_watchlist_unit_minutes
import emberr.shared.generated.resources.movie_watchlist_unit_years
import emberr.shared.generated.resources.movie_watchlist_zero_to_ten
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource

private val FilterValueShape = RoundedCornerShape(12.dp)
private const val TMDB_HIGHEST_RATING = 10.0

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun WatchlistFilterMenu(
    expanded: Boolean,
    block: WatchlistBlock,
    openView: WatchlistViewSettings,
    titles: List<MovieBlock>,
    editor: WatchlistBlockEditor,
    onDismiss: () -> Unit
) {
    fun changeFilters(change: (List<WatchlistFilter>) -> List<WatchlistFilter>) {
        editor.changeView(block.id, openView.id) { view -> view.copy(filters = change(view.filters)) }
    }
    fun addFilter(field: WatchlistFilterField) {
        changeFilters { filters -> filters + newWatchlistFilter(Uuid.random().toString(), field) }
    }

    MovieMenu(expanded = expanded, title = stringResource(Res.string.movie_watchlist_filter), onDismiss = onDismiss) { _ ->
        if (openView.filters.isEmpty()) {
            MovieMenuSectionLabel(stringResource(Res.string.movie_watchlist_filter_by))
            WatchlistFilterFieldChoices(onPick = ::addFilter)
        } else {
            openView.filters.forEachIndexed { index, filter ->
                key(filter.id) {
                    if (index > 0) MovieMenuDivider()
                    MovieMenuSectionLabel(filter.field.displayName())
                    WatchlistFilterEditor(
                        filter = filter,
                        titles = titles,
                        onChange = { change ->
                            changeFilters { filters -> filters.map { if (it.id == filter.id) change(it) else it } }
                        },
                        onRemove = { changeFilters { filters -> filters.filter { it.id != filter.id } } }
                    )
                }
            }
            MovieMenuDivider()
            MovieMenuLayer(
                title = stringResource(Res.string.movie_watchlist_add_filter),
                anchor = { openLayer -> MovieMenuOption(label = stringResource(Res.string.movie_watchlist_add_filter), icon = Res.drawable.plus, onClick = openLayer) }
            ) { closeLayerAnd ->
                WatchlistFilterFieldChoices(onPick = { field -> closeLayerAnd { addFilter(field) } })
            }
        }
    }
}

@Composable
private fun WatchlistFilterFieldChoices(onPick: (WatchlistFilterField) -> Unit) {
    WatchlistFilterField.entries.forEach { field ->
        MovieMenuOption(label = field.displayName(), onClick = { onPick(field) })
    }
}

@Composable
private fun WatchlistFilterEditor(
    filter: WatchlistFilter,
    titles: List<MovieBlock>,
    onChange: ((WatchlistFilter) -> WatchlistFilter) -> Unit,
    onRemove: () -> Unit
) {
    MovieMenuContent {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (filter.field.conditions.size > 1) {
                MovieMenuLayer(
                    title = filter.field.displayName(),
                    anchorIsMenuRow = false,
                    anchor = { openLayer -> MenuChoiceButton(text = filter.condition.displayName(), onClick = openLayer) }
                ) { closeLayerAnd ->
                    filter.field.conditions.forEach { condition ->
                        MovieMenuOption(
                            label = condition.displayName(),
                            isSelected = condition == filter.condition,
                            onClick = { closeLayerAnd { onChange { it.withCondition(condition) } } }
                        )
                    }
                }
            } else {
                Text(
                    text = filter.condition.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            WatchlistCloseButton(label = stringResource(Res.string.movie_watchlist_remove_filter), touchPadding = 4.dp, onClick = onRemove)
        }
        WatchlistFilterValue(filter = filter, titles = titles, onChange = onChange)
    }
}

@Composable
private fun WatchlistFilterValue(
    filter: WatchlistFilter,
    titles: List<MovieBlock>,
    onChange: ((WatchlistFilter) -> WatchlistFilter) -> Unit
) {
    val valueModifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    when (filter.condition) {
        WatchlistFilterCondition.IS,
        WatchlistFilterCondition.IS_ANY_OF,
        WatchlistFilterCondition.HAS_ANY_OF,
        WatchlistFilterCondition.HAS_NONE_OF -> Box(modifier = valueModifier) {
            WatchlistFilterPicksValue(
                filter = filter,
                choices = pickChoicesOf(filter, titles),
                picksOnlyOne = filter.condition == WatchlistFilterCondition.IS,
                onChange = onChange
            )
        }
        WatchlistFilterCondition.BEFORE,
        WatchlistFilterCondition.AFTER,
        WatchlistFilterCondition.ON_OR_AFTER -> Box(modifier = valueModifier) {
            WatchlistFilterDateValue(date = filter.date, placeholder = stringResource(Res.string.movie_watchlist_pick_a_date)) { date -> onChange { it.copy(date = date) } }
        }
        WatchlistFilterCondition.BETWEEN -> Column(modifier = valueModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchlistFilterDateValue(date = filter.date, placeholder = stringResource(Res.string.movie_watchlist_date_from)) { date -> onChange { it.copy(date = date) } }
            WatchlistFilterDateValue(date = filter.endDate, placeholder = stringResource(Res.string.movie_watchlist_date_to)) { date -> onChange { it.copy(endDate = date) } }
        }
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(
                number = filter.number,
                placeholder = stringResource(Res.string.movie_watchlist_number_of_days),
                unit = stringResource(Res.string.movie_watchlist_unit_days)
            ) { number ->
                onChange { it.copy(number = number) }
            }
        }
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(
                number = filter.number,
                placeholder = stringResource(Res.string.movie_watchlist_number_of_years),
                unit = stringResource(Res.string.movie_watchlist_unit_years)
            ) { number ->
                onChange { it.copy(number = number) }
            }
        }
        WatchlistFilterCondition.SHORTER_THAN,
        WatchlistFilterCondition.LONGER_THAN -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(
                number = filter.number,
                placeholder = stringResource(Res.string.movie_watchlist_number_of_minutes),
                unit = stringResource(Res.string.movie_watchlist_unit_minutes)
            ) { number ->
                onChange { it.copy(number = number) }
            }
        }
        WatchlistFilterCondition.AT_LEAST,
        WatchlistFilterCondition.AT_MOST -> Box(modifier = valueModifier) {
            if (filter.field == WatchlistFilterField.YOUR_RATING) {
                StarRatingPicker(
                    ratingHalfStars = filter.number?.let { (it * 2).toInt() },
                    emptyColor = chipContainerColor(),
                    onRate = { halfStars -> onChange { it.copy(number = halfStars?.let { half -> half / 2.0 }) } }
                )
            } else {
                WatchlistFilterNumberValue(
                    number = filter.number,
                    placeholder = stringResource(Res.string.movie_watchlist_zero_to_ten),
                    unit = stringResource(Res.string.movie_out_of_ten),
                    highestNumber = TMDB_HIGHEST_RATING
                ) { number -> onChange { it.copy(number = number) } }
            }
        }
        WatchlistFilterCondition.NOT_RATED_YET,
        WatchlistFilterCondition.STILL_AIRING,
        WatchlistFilterCondition.ENDED -> Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun pickChoicesOf(filter: WatchlistFilter, titles: List<MovieBlock>): List<String> {
    val choices = when (filter.field) {
        WatchlistFilterField.TYPE -> MovieKind.entries.map { it.name }
        WatchlistFilterField.STATUS -> WatchStatus.entries.map { it.name }
        WatchlistFilterField.GENRE -> titles.genreChoices()
        else -> emptyList()
    }
    return choices + filter.picks.filter { it !in choices }
}

@Composable
private fun pickLabelOf(field: WatchlistFilterField, pick: String): String = when (field) {
    WatchlistFilterField.TYPE -> MovieKind.entries.firstOrNull { it.name == pick }?.pluralDisplayName() ?: pick
    WatchlistFilterField.STATUS -> WatchStatus.entries.firstOrNull { it.name == pick }?.displayName() ?: pick
    else -> pick
}

@Composable
private fun WatchlistFilterPicksValue(
    filter: WatchlistFilter,
    choices: List<String>,
    picksOnlyOne: Boolean,
    onChange: ((WatchlistFilter) -> WatchlistFilter) -> Unit
) {
    val shownValue = filter.picks.map { pickLabelOf(filter.field, it) }.joinToString(", ")
    MovieMenuLayer(
        title = filter.field.displayName(),
        anchorIsMenuRow = false,
        anchor = { openLayer ->
            WatchlistFilterValueBox(onClick = openLayer) {
                WatchlistFilterValueText(text = shownValue.ifEmpty { stringResource(Res.string.movie_watchlist_pick_options) }, isPlaceholder = shownValue.isEmpty())
            }
        }
    ) { closeLayerAnd ->
        if (choices.isEmpty()) {
            MovieMenuContent {
                Text(
                    text = stringResource(Res.string.movie_watchlist_no_genres_yet),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        choices.forEach { choice ->
            val isPicked = choice in filter.picks
            MovieMenuOption(
                label = pickLabelOf(filter.field, choice),
                isSelected = isPicked,
                onClick = {
                    if (picksOnlyOne) {
                        closeLayerAnd { onChange { it.copy(picks = listOf(choice)) } }
                    } else {
                        onChange { current ->
                            current.copy(picks = if (choice in current.picks) current.picks - choice else current.picks + choice)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun WatchlistFilterDateValue(date: LocalDate?, placeholder: String, onDateChosen: (LocalDate) -> Unit) {
    var showsDatePicker by remember { mutableStateOf(false) }
    val timeZone = TimeZone.currentSystemDefault()

    WatchlistFilterValueBox(onClick = { showsDatePicker = true }) {
        WatchlistFilterValueText(text = date?.let(::releaseDateText) ?: placeholder, isPlaceholder = date == null)
    }

    if (showsDatePicker) {
        MinimalDatePickerDialog(
            initialTimestamp = date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(),
            onDismiss = { showsDatePicker = false },
            onConfirm = { selectedMillis ->
                onDateChosen(Instant.fromEpochMilliseconds(selectedMillis).toLocalDateTime(timeZone).date)
                showsDatePicker = false
            }
        )
    }
}

@Composable
private fun WatchlistFilterNumberValue(
    number: Double?,
    placeholder: String,
    unit: String,
    highestNumber: Double? = null,
    onNumberChange: (Double?) -> Unit
) {
    var text by remember { mutableStateOf(number?.let(::numberText).orEmpty()) }
    EmberrTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            val typedNumber = newText.replace(',', '.').toDoubleOrNull()
            onNumberChange(typedNumber?.takeIf { it >= 0 && (highestNumber == null || it <= highestNumber) })
        },
        placeholder = placeholder,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        trailingIcon = {
            Text(text = unit, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        }
    )
}

@Composable
private fun WatchlistFilterValueBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FilterValueShape)
            .background(subtleFillColor())
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
private fun WatchlistFilterValueText(text: String, isPlaceholder: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isPlaceholder) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WatchlistFilterChips(
    block: WatchlistBlock,
    openView: WatchlistViewSettings,
    editor: WatchlistBlockEditor,
    isEnabled: Boolean
) {
    if (openView.filters.isEmpty()) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        openView.filters.forEach { filter ->
            key(filter.id) {
                WatchlistFilterChip(label = filter.chipLabel(), isEnabled = isEnabled) {
                    editor.changeView(block.id, openView.id) { view -> view.copy(filters = view.filters.filter { it.id != filter.id }) }
                }
            }
        }
    }
}

@Composable
private fun WatchlistFilterChip(label: String, isEnabled: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(SmallRoundedCornerShape)
            .background(SelectedOptionBackground)
            .clickable(enabled = isEnabled, onClick = onRemove)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Icon(
            painter = painterResource(Res.drawable.x),
            contentDescription = stringResource(Res.string.movie_watchlist_remove_filter),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun WatchlistFilter.chipLabel(): String {
    if (!isUsable()) return field.displayName()
    val pickedText = picks.map { pickLabelOf(field, it) }.joinToString(", ")
    val chosenNumber = number ?: 0.0
    val chosenCount = chosenNumber.toInt()
    return when (condition) {
        WatchlistFilterCondition.IS -> stringResource(Res.string.movie_watchlist_chip_only, pickedText)
        WatchlistFilterCondition.IS_ANY_OF, WatchlistFilterCondition.HAS_ANY_OF -> pickedText
        WatchlistFilterCondition.HAS_NONE_OF -> stringResource(Res.string.movie_watchlist_chip_not, pickedText)
        WatchlistFilterCondition.BEFORE ->
            if (field == WatchlistFilterField.DATE_ADDED) {
                stringResource(Res.string.movie_watchlist_chip_added_before, dateChipText(date))
            } else {
                stringResource(Res.string.movie_watchlist_chip_before, yearOrDateText(date, isRangeEnd = false))
            }
        WatchlistFilterCondition.AFTER -> stringResource(Res.string.movie_watchlist_chip_added_after, dateChipText(date))
        WatchlistFilterCondition.ON_OR_AFTER -> stringResource(Res.string.movie_watchlist_chip_or_later, yearOrDateText(date, isRangeEnd = false))
        WatchlistFilterCondition.BETWEEN -> stringResource(
            Res.string.movie_watchlist_chip_between,
            yearOrDateText(date, isRangeEnd = false),
            yearOrDateText(endDate, isRangeEnd = true)
        )
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> pluralStringResource(Res.plurals.movie_watchlist_chip_added_in_past_days, chosenCount, chosenCount)
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> pluralStringResource(Res.plurals.movie_watchlist_chip_past_years, chosenCount, chosenCount)
        WatchlistFilterCondition.AT_LEAST ->
            if (field == WatchlistFilterField.YOUR_RATING) {
                stringResource(Res.string.movie_watchlist_chip_stars_and_up, starsTextOf((chosenNumber * 2).toInt()))
            } else {
                stringResource(Res.string.movie_watchlist_chip_tmdb_at_least, numberText(chosenNumber))
            }
        WatchlistFilterCondition.AT_MOST ->
            if (field == WatchlistFilterField.YOUR_RATING) {
                stringResource(Res.string.movie_watchlist_chip_up_to_stars, starsTextOf((chosenNumber * 2).toInt()))
            } else {
                stringResource(Res.string.movie_watchlist_chip_tmdb_up_to, numberText(chosenNumber))
            }
        WatchlistFilterCondition.NOT_RATED_YET -> stringResource(Res.string.movie_watchlist_chip_not_rated_yet)
        WatchlistFilterCondition.SHORTER_THAN -> stringResource(Res.string.movie_watchlist_chip_under, minutesText(chosenCount))
        WatchlistFilterCondition.LONGER_THAN -> stringResource(Res.string.movie_watchlist_chip_over, minutesText(chosenCount))
        WatchlistFilterCondition.STILL_AIRING -> stringResource(Res.string.movie_watchlist_chip_still_airing)
        WatchlistFilterCondition.ENDED -> stringResource(Res.string.movie_watchlist_chip_ended)
    }
}

private fun dateChipText(date: LocalDate?): String = date?.let(::releaseDateText).orEmpty()

private fun yearOrDateText(date: LocalDate?, isRangeEnd: Boolean): String {
    if (date == null) return ""
    val isWholeYearEdge = if (isRangeEnd) date.month == Month.DECEMBER && date.day == 31 else date.month == Month.JANUARY && date.day == 1
    return if (isWholeYearEdge) date.year.toString() else releaseDateText(date)
}

private fun numberText(number: Double): String = decimalText(number, minimumFractionDigits = 0, maximumFractionDigits = 2)
