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

    MovieMenu(expanded = expanded, title = "Filter", onDismiss = onDismiss) { _ ->
        if (openView.filters.isEmpty()) {
            MovieMenuSectionLabel("Filter by")
            WatchlistFilterFieldChoices(onPick = ::addFilter)
        } else {
            openView.filters.forEachIndexed { index, filter ->
                key(filter.id) {
                    if (index > 0) MovieMenuDivider()
                    MovieMenuSectionLabel(filter.field.label)
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
                title = "Add filter",
                anchor = { openLayer -> MovieMenuOption(label = "Add filter", icon = Res.drawable.plus, onClick = openLayer) }
            ) { closeLayerAnd ->
                WatchlistFilterFieldChoices(onPick = { field -> closeLayerAnd { addFilter(field) } })
            }
        }
    }
}

@Composable
private fun WatchlistFilterFieldChoices(onPick: (WatchlistFilterField) -> Unit) {
    WatchlistFilterField.entries.forEach { field ->
        MovieMenuOption(label = field.label, onClick = { onPick(field) })
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
                    title = filter.field.label,
                    anchorIsMenuRow = false,
                    anchor = { openLayer -> MenuChoiceButton(text = filter.condition.label, onClick = openLayer) }
                ) { closeLayerAnd ->
                    filter.field.conditions.forEach { condition ->
                        MovieMenuOption(
                            label = condition.label,
                            isSelected = condition == filter.condition,
                            onClick = { closeLayerAnd { onChange { it.withCondition(condition) } } }
                        )
                    }
                }
            } else {
                Text(
                    text = filter.condition.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            WatchlistCloseButton(label = "Remove filter", touchPadding = 4.dp, onClick = onRemove)
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
            WatchlistFilterDateValue(date = filter.date, placeholder = "Pick a date") { date -> onChange { it.copy(date = date) } }
        }
        WatchlistFilterCondition.BETWEEN -> Column(modifier = valueModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WatchlistFilterDateValue(date = filter.date, placeholder = "From") { date -> onChange { it.copy(date = date) } }
            WatchlistFilterDateValue(date = filter.endDate, placeholder = "To") { date -> onChange { it.copy(endDate = date) } }
        }
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(number = filter.number, placeholder = "Number of days", unit = "days") { number ->
                onChange { it.copy(number = number) }
            }
        }
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(number = filter.number, placeholder = "Number of years", unit = "years") { number ->
                onChange { it.copy(number = number) }
            }
        }
        WatchlistFilterCondition.SHORTER_THAN,
        WatchlistFilterCondition.LONGER_THAN -> Box(modifier = valueModifier) {
            WatchlistFilterNumberValue(number = filter.number, placeholder = "Number of minutes", unit = "min") { number ->
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
                    placeholder = "0 to 10",
                    unit = "/ 10",
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

private fun pickLabelOf(field: WatchlistFilterField, pick: String): String = when (field) {
    WatchlistFilterField.TYPE -> MovieKind.entries.firstOrNull { it.name == pick }?.let { "${it.label}s" } ?: pick
    WatchlistFilterField.STATUS -> WatchStatus.entries.firstOrNull { it.name == pick }?.label ?: pick
    else -> pick
}

@Composable
private fun WatchlistFilterPicksValue(
    filter: WatchlistFilter,
    choices: List<String>,
    picksOnlyOne: Boolean,
    onChange: ((WatchlistFilter) -> WatchlistFilter) -> Unit
) {
    val shownValue = filter.picks.joinToString(", ") { pickLabelOf(filter.field, it) }
    MovieMenuLayer(
        title = filter.field.label,
        anchorIsMenuRow = false,
        anchor = { openLayer ->
            WatchlistFilterValueBox(onClick = openLayer) {
                WatchlistFilterValueText(text = shownValue.ifEmpty { "Pick options" }, isPlaceholder = shownValue.isEmpty())
            }
        }
    ) { closeLayerAnd ->
        if (choices.isEmpty()) {
            MovieMenuContent {
                Text(
                    text = "No genres in this watchlist yet",
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
            contentDescription = "Remove filter",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
    }
}

private fun WatchlistFilter.chipLabel(): String {
    if (!isUsable()) return field.label
    val pickedText = picks.joinToString(", ") { pickLabelOf(field, it) }
    val chosenNumber = number ?: 0.0
    return when (condition) {
        WatchlistFilterCondition.IS -> "$pickedText only"
        WatchlistFilterCondition.IS_ANY_OF, WatchlistFilterCondition.HAS_ANY_OF -> pickedText
        WatchlistFilterCondition.HAS_NONE_OF -> "Not $pickedText"
        WatchlistFilterCondition.BEFORE ->
            if (field == WatchlistFilterField.DATE_ADDED) "Added before ${dateChipText(date)}" else "Before ${yearOrDateText(date, isRangeEnd = false)}"
        WatchlistFilterCondition.AFTER -> "Added after ${dateChipText(date)}"
        WatchlistFilterCondition.ON_OR_AFTER -> "${yearOrDateText(date, isRangeEnd = false)} or later"
        WatchlistFilterCondition.BETWEEN -> "${yearOrDateText(date, isRangeEnd = false)} – ${yearOrDateText(endDate, isRangeEnd = true)}"
        WatchlistFilterCondition.WITHIN_PAST_DAYS -> "Added in the past ${countText(chosenNumber.toInt(), "day")}"
        WatchlistFilterCondition.WITHIN_PAST_YEARS -> "Past ${countText(chosenNumber.toInt(), "year")}"
        WatchlistFilterCondition.AT_LEAST ->
            if (field == WatchlistFilterField.YOUR_RATING) "${starsTextOf((chosenNumber * 2).toInt())}★ and up" else "TMDB ${numberText(chosenNumber)}+"
        WatchlistFilterCondition.AT_MOST ->
            if (field == WatchlistFilterField.YOUR_RATING) "Up to ${starsTextOf((chosenNumber * 2).toInt())}★" else "TMDB up to ${numberText(chosenNumber)}"
        WatchlistFilterCondition.NOT_RATED_YET -> "Not rated yet"
        WatchlistFilterCondition.SHORTER_THAN -> "Under ${minutesText(chosenNumber.toInt())}"
        WatchlistFilterCondition.LONGER_THAN -> "Over ${minutesText(chosenNumber.toInt())}"
        WatchlistFilterCondition.STILL_AIRING -> "Still airing"
        WatchlistFilterCondition.ENDED -> "Ended"
    }
}

private fun dateChipText(date: LocalDate?): String = date?.let(::releaseDateText).orEmpty()

private fun yearOrDateText(date: LocalDate?, isRangeEnd: Boolean): String {
    if (date == null) return ""
    val isWholeYearEdge = if (isRangeEnd) date.month == Month.DECEMBER && date.day == 31 else date.month == Month.JANUARY && date.day == 1
    return if (isWholeYearEdge) date.year.toString() else releaseDateText(date)
}

private fun countText(count: Int, unit: String): String = if (count == 1) unit else "$count ${unit}s"

private fun numberText(number: Double): String = if (number % 1.0 == 0.0) number.toInt().toString() else number.toString()
