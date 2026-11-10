package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.database.activeView
import com.emberr.domain.database.filterConditionsFor
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseDateRange
import com.emberr.domain.model.DatabaseFilter
import com.emberr.domain.model.DatabaseFilterCondition
import com.emberr.domain.model.DatabaseRelativeDate
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.tagPoolKey
import com.emberr.domain.model.valueTypeOf
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.MenuChoiceButton
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.displayName
import com.emberr.presentation.shared.components.rememberPeopleToPick
import com.emberr.presentation.shared.editor.blockViews.property.PropertyTagChip
import com.emberr.presentation.shared.editor.blockViews.property.formatPropertyDate
import com.emberr.presentation.shared.editor.blockViews.property.rememberPropertyTagColor
import com.emberr.presentation.shared.components.orHighContrastLine
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.x
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant
import emberr.shared.generated.resources.database_add_filter
import emberr.shared.generated.resources.database_exact_date
import emberr.shared.generated.resources.database_exact_date_value
import emberr.shared.generated.resources.database_filter
import emberr.shared.generated.resources.database_filter_by
import emberr.shared.generated.resources.database_no_options_saved_yet
import emberr.shared.generated.resources.database_no_people_added_yet
import emberr.shared.generated.resources.database_pick_a_date
import emberr.shared.generated.resources.database_pick_a_person
import emberr.shared.generated.resources.database_pick_a_range
import emberr.shared.generated.resources.database_pick_an_option
import emberr.shared.generated.resources.database_remove_filter
import emberr.shared.generated.resources.database_type_a_number
import emberr.shared.generated.resources.database_type_a_value
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseFilterMenu(
    expanded: Boolean,
    block: DatabaseBlock,
    editor: DatabaseBlockEditor,
    onDismiss: () -> Unit
) {
    val view = block.activeView()
    val columnTargets = listOf<DatabaseColumnTarget>(DatabaseColumnTarget.NotesTitle) + block.columns

    DatabaseMenu(expanded = expanded, title = stringResource(Res.string.database_filter), onDismiss = onDismiss) { _ ->
        if (view.filters.isEmpty()) {
            DatabaseMenuSectionLabel(text = stringResource(Res.string.database_filter_by))
            DatabaseColumnChoices(
                block = block,
                columns = columnTargets,
                selectedColumn = null,
                onPick = { target -> editor.addFilter(block.id, view.id, target) }
            )
        } else {
            view.filters.forEachIndexed { index, filter ->
                key(filter.id) {
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = DatabaseMenuRowInset, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
                        )
                    }
                    DatabaseMenuSectionLabel(text = block.displayLabelOf(filter.target))
                    DatabaseFilterEditor(
                        block = block,
                        filter = filter,
                        editor = editor,
                        onChange = { change -> editor.changeFilter(block.id, view.id, filter.id, change) },
                        onRemove = { editor.removeFilter(block.id, view.id, filter.id) }
                    )
                }
            }

            DatabaseMenuSectionDivider()
            DatabaseMenuLayer(
                title = stringResource(Res.string.database_add_filter),
                anchor = { openLayer ->
                    DatabaseMenuOption(
                        label = stringResource(Res.string.database_add_filter),
                        icon = { DatabaseOptionIcon(Res.drawable.plus) },
                        onClick = openLayer
                    )
                }
            ) { closeLayerAnd ->
                DatabaseColumnChoices(
                    block = block,
                    columns = columnTargets,
                    selectedColumn = null,
                    onPick = { target -> closeLayerAnd { editor.addFilter(block.id, view.id, target) } }
                )
            }
        }
    }
}

@Composable
internal fun DatabaseFilterEditor(
    block: DatabaseBlock,
    filter: DatabaseFilter,
    editor: DatabaseBlockEditor,
    onChange: ((DatabaseFilter) -> DatabaseFilter) -> Unit,
    onRemove: () -> Unit,
    removeLabel: String = stringResource(Res.string.database_remove_filter)
) {
    val valueType = block.valueTypeOf(filter.target) ?: PropertyValueType.TEXT
    val columnLabel = block.displayLabelOf(filter.target)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DatabaseMenuRowInset, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DatabaseMenuLayer(
                title = columnLabel,
                anchor = { openLayer ->
                    MenuChoiceButton(text = filter.condition.displayName(), onClick = openLayer)
                },
                anchorIsMenuRow = false
            ) { closeLayerAnd ->
                DatabaseConditionChoices(
                    valueType = valueType,
                    selectedCondition = filter.condition,
                    onPick = { condition -> closeLayerAnd { onChange { it.copy(condition = condition) } } }
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconTooltip(text = removeLabel) {
                Icon(
                    painter = painterResource(Res.drawable.x),
                    contentDescription = removeLabel,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                        .padding(4.dp)
                        .size(14.dp)
                )
            }
        }

        if (filter.condition.needsValue) {
            Spacer(modifier = Modifier.height(8.dp))
            when {
                valueType.holdsDate && filter.condition == DatabaseFilterCondition.IS_WITHIN -> DatabaseFilterDateRangeValue(
                    title = columnLabel,
                    dateRange = filter.dateRange,
                    onDateRangeChosen = { dateRange -> onChange { it.copy(dateRange = dateRange) } }
                )
                valueType.holdsDate -> DatabaseFilterDateValue(
                    title = columnLabel,
                    date = filter.date,
                    relativeDate = filter.relativeDate,
                    onRelativeDateChosen = { relativeDate -> onChange { it.copy(relativeDate = relativeDate, date = null) } },
                    onDateChosen = { date -> onChange { it.copy(date = date, relativeDate = null) } }
                )
                valueType.holdsTags -> DatabaseMenuLayer(
                    title = columnLabel,
                    anchor = { openLayer -> DatabaseFilterOptionValue(tagName = filter.tagName, tagPoolKey = filter.target.tagPoolKey, onClick = openLayer) },
                    anchorIsMenuRow = false
                ) { closeLayerAnd ->
                    DatabaseOptionChoices(
                        filter = filter,
                        editor = editor,
                        onPick = { tagName -> closeLayerAnd { onChange { it.copy(tagName = tagName) } } }
                    )
                }
                valueType.holdsPeople -> DatabaseMenuLayer(
                    title = columnLabel,
                    anchor = { openLayer -> DatabaseFilterPersonValue(personNoteId = filter.personNoteId, onClick = openLayer) },
                    anchorIsMenuRow = false
                ) { closeLayerAnd ->
                    DatabaseFilterPersonChoices(
                        selectedPersonNoteId = filter.personNoteId,
                        onPick = { personNoteId -> closeLayerAnd { onChange { it.copy(personNoteId = personNoteId) } } }
                    )
                }
                else -> EmberrTextField(
                    value = filter.text,
                    onValueChange = { text -> onChange { it.copy(text = text) } },
                    placeholder = stringResource(if (valueType.holdsNumber) Res.string.database_type_a_number else Res.string.database_type_a_value),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = if (valueType.holdsNumber) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default
                )
            }
        }
    }
}

@Composable
private fun DatabaseFilterDateValue(
    title: String,
    date: LocalDate?,
    relativeDate: DatabaseRelativeDate?,
    onRelativeDateChosen: (DatabaseRelativeDate) -> Unit,
    onDateChosen: (LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val timeZone = TimeZone.currentSystemDefault()
    val shownValue = relativeDate?.displayName() ?: date?.let { formatPropertyDate(it) }

    DatabaseMenuLayer(
        title = title,
        anchor = { openLayer ->
            DatabaseFilterValueBox(onClick = openLayer) {
                DatabaseFilterValueText(text = shownValue ?: stringResource(Res.string.database_pick_a_date), isPlaceholder = shownValue == null)
            }
        },
        anchorIsMenuRow = false
    ) { closeLayerAnd ->
        DatabaseRelativeDate.entries.forEach { option ->
            DatabaseMenuOption(
                label = option.displayName(),
                isSelected = option == relativeDate,
                onClick = { closeLayerAnd { onRelativeDateChosen(option) } }
            )
        }
        DatabaseMenuSectionDivider()
        DatabaseMenuOption(
            label = date?.let { stringResource(Res.string.database_exact_date_value, formatPropertyDate(it)) }
                ?: stringResource(Res.string.database_exact_date),
            isSelected = relativeDate == null && date != null,
            onClick = { closeLayerAnd { showDatePicker = true } }
        )
    }

    if (showDatePicker) {
        MinimalDatePickerDialog(
            initialTimestamp = date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(),
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedMillis ->
                onDateChosen(Instant.fromEpochMilliseconds(selectedMillis).toLocalDateTime(timeZone).date)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun DatabaseFilterDateRangeValue(
    title: String,
    dateRange: DatabaseDateRange?,
    onDateRangeChosen: (DatabaseDateRange) -> Unit
) {
    DatabaseMenuLayer(
        title = title,
        anchor = { openLayer ->
            DatabaseFilterValueBox(onClick = openLayer) {
                DatabaseFilterValueText(text = dateRange?.displayName() ?: stringResource(Res.string.database_pick_a_range), isPlaceholder = dateRange == null)
            }
        },
        anchorIsMenuRow = false
    ) { closeLayerAnd ->
        DatabaseDateRange.entries.forEach { option ->
            DatabaseMenuOption(
                label = option.displayName(),
                isSelected = option == dateRange,
                onClick = { closeLayerAnd { onDateRangeChosen(option) } }
            )
        }
    }
}

@Composable
private fun DatabaseFilterValueText(text: String, isPlaceholder: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isPlaceholder) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun DatabaseFilterOptionValue(tagName: String?, tagPoolKey: String?, onClick: () -> Unit) {
    DatabaseFilterValueBox(onClick = onClick) {
        if (tagName == null) {
            Text(text = stringResource(Res.string.database_pick_an_option), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.outline)
        } else {
            PropertyTagChip(tagName = tagName, tagPoolKey = tagPoolKey, textStyle = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DatabaseFilterPersonValue(personNoteId: String?, onClick: () -> Unit) {
    val person = rememberPeopleToPick()?.firstOrNull { it.id == personNoteId }
    DatabaseFilterValueBox(onClick = onClick) {
        if (person == null) {
            DatabaseFilterValueText(text = stringResource(Res.string.database_pick_a_person), isPlaceholder = true)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(personNoteId = person.id, name = person.name, icon = person.icon, photoPath = person.photoPath, size = 20.dp)
                Spacer(modifier = Modifier.width(8.dp))
                DatabaseFilterValueText(text = person.displayName(), isPlaceholder = false)
            }
        }
    }
}

@Composable
private fun DatabaseFilterPersonChoices(selectedPersonNoteId: String?, onPick: (String) -> Unit) {
    val people = rememberPeopleToPick().orEmpty()
    if (people.isEmpty()) {
        DatabaseMenuMessage(text = stringResource(Res.string.database_no_people_added_yet), color = MaterialTheme.colorScheme.outline)
    }
    people.forEach { person ->
        DatabaseMenuOption(
            label = person.displayName(),
            isSelected = person.id == selectedPersonNoteId,
            icon = { PersonAvatar(personNoteId = person.id, name = person.name, icon = person.icon, photoPath = person.photoPath, size = 16.dp) },
            onClick = { onPick(person.id) }
        )
    }
}

@Composable
private fun DatabaseFilterValueBox(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        content()
    }
}

@Composable
private fun DatabaseConditionChoices(
    valueType: PropertyValueType,
    selectedCondition: DatabaseFilterCondition,
    onPick: (DatabaseFilterCondition) -> Unit
) {
    filterConditionsFor(valueType).forEach { condition ->
        DatabaseMenuOption(
            label = condition.displayName(),
            isSelected = condition == selectedCondition,
            onClick = { onPick(condition) }
        )
    }
}

@Composable
private fun DatabaseOptionChoices(
    filter: DatabaseFilter,
    editor: DatabaseBlockEditor,
    onPick: (String) -> Unit
) {
    val tagPoolKey = filter.target.tagPoolKey
    val savedTags by remember(tagPoolKey) {
        if (tagPoolKey == null) flowOf(emptyList()) else editor.savedTagsOf(tagPoolKey)
    }.collectAsState(initial = emptyList())
    val tagNames = (savedTags.map { it.name } + listOfNotNull(filter.tagName)).distinctBy { it.lowercase() }

    if (tagNames.isEmpty()) {
        DatabaseMenuMessage(text = stringResource(Res.string.database_no_options_saved_yet), color = MaterialTheme.colorScheme.outline)
    }
    tagNames.forEach { tagName ->
        DatabaseMenuOption(
            label = tagName,
            isSelected = tagName.equals(filter.tagName, ignoreCase = true),
            icon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(rememberPropertyTagColor(tagPoolKey, tagName))
                )
            },
            onClick = { onPick(tagName) }
        )
    }
}
