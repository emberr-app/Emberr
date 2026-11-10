package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.database.activeView
import com.emberr.domain.database.isFormulaColumn
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyDateRange
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.isNumberBeingTyped
import com.emberr.domain.model.tagPoolKey
import com.emberr.domain.model.valueTypeOf
import com.emberr.domain.model.withDateRange
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.MinimalDatePickerDialog
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.displayName
import com.emberr.presentation.shared.components.rememberPeopleToPick
import com.emberr.presentation.shared.editor.blockViews.property.rememberPropertyTagColor
import com.emberr.ui.theme.tableGridLineColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Instant
import emberr.shared.generated.resources.common_delete
import emberr.shared.generated.resources.database_checked
import emberr.shared.generated.resources.database_clear
import emberr.shared.generated.resources.database_duplicate
import emberr.shared.generated.resources.database_edit
import emberr.shared.generated.resources.database_edit_rows
import emberr.shared.generated.resources.database_no_options_saved_yet
import emberr.shared.generated.resources.database_no_people_added_yet
import emberr.shared.generated.resources.database_no_properties_to_edit
import emberr.shared.generated.resources.database_pick_a_date
import emberr.shared.generated.resources.database_rows_selected
import emberr.shared.generated.resources.database_set_for_every_row
import emberr.shared.generated.resources.database_set_property_for_selected_rows
import emberr.shared.generated.resources.database_type_a_number
import emberr.shared.generated.resources.database_type_a_value
import emberr.shared.generated.resources.database_unchecked
import emberr.shared.generated.resources.editor_clear_selection_sentence_case
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseRowCheckboxCell(
    isChecked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lineColor = tableGridLineColor
    Box(
        modifier = modifier
            .width(DatabaseSelectionColumnWidth)
            .fillMaxHeight()
            .defaultMinSize(minHeight = DatabaseCellMinHeight)
            .databaseCellLines(lineColor)
            .clickable(enabled = enabled, onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        EmberrCheckbox(checked = isChecked, onCheckedChange = null)
    }
}

@Composable
internal fun DatabaseSelectionBar(
    block: DatabaseBlock,
    selectedRowIds: List<String>,
    shownRowIds: List<String>,
    editor: DatabaseBlockEditor,
    onClearSelection: () -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showsEditMenu by remember { mutableStateOf(false) }
    val viewId = block.activeView().id

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = pluralStringResource(Res.plurals.database_rows_selected, selectedRowIds.size, selectedRowIds.size),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        val hasSelectedRows = selectedRowIds.isNotEmpty()
        Box {
            DatabaseSelectionBarButton(
                label = stringResource(Res.string.database_edit),
                icon = Res.drawable.pen,
                enabled = hasSelectedRows,
                onClick = { runAfterKeyboardCloses { showsEditMenu = true } }
            )
            DatabaseMenu(expanded = showsEditMenu, title = pluralStringResource(Res.plurals.database_edit_rows, selectedRowIds.size, selectedRowIds.size), onDismiss = { showsEditMenu = false }) { closeMenuAnd ->
                DatabaseBulkEditChoices(block = block, selectedRowIds = selectedRowIds, editor = editor, closeMenuAnd = closeMenuAnd)
            }
        }
        DatabaseSelectionBarButton(
            label = stringResource(Res.string.database_duplicate),
            icon = Res.drawable.copy,
            enabled = hasSelectedRows,
            onClick = {
                editor.duplicateRows(block.id, viewId, shownRowIds, selectedRowIds)
                onClearSelection()
            }
        )
        DatabaseSelectionBarButton(
            label = stringResource(Res.string.common_delete),
            icon = Res.drawable.trash,
            isDestructive = true,
            enabled = hasSelectedRows,
            onClick = {
                editor.deleteRows(selectedRowIds)
                onClearSelection()
            }
        )
        IconTooltip(text = stringResource(Res.string.editor_clear_selection_sentence_case)) {
            Icon(
                painter = painterResource(Res.drawable.x),
                contentDescription = stringResource(Res.string.editor_clear_selection_sentence_case),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClearSelection)
                    .padding(6.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
private fun DatabaseSelectionBarButton(
    label: String,
    icon: DrawableResource,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val fullColor = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val color = if (enabled) fullColor else fullColor.copy(alpha = 0.38f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painter = painterResource(icon), contentDescription = if (isDesktopPlatform) null else label, tint = color, modifier = Modifier.size(16.dp))
        if (isDesktopPlatform) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun DatabaseBulkEditChoices(
    block: DatabaseBlock,
    selectedRowIds: List<String>,
    editor: DatabaseBlockEditor,
    closeMenuAnd: (() -> Unit) -> Unit
) {
    val editableColumns = block.columns.filterNot { block.isFormulaColumn(it) }
    if (editableColumns.isEmpty()) {
        DatabaseMenuMessage(text = stringResource(Res.string.database_no_properties_to_edit), color = MaterialTheme.colorScheme.outline)
    }

    DatabaseMenuSectionLabel(text = stringResource(Res.string.database_set_property_for_selected_rows))
    editableColumns.forEach { column ->
        key(column) {
            val label = block.displayLabelOf(column)
            fun setEveryCell(change: (PropertyBlock) -> PropertyBlock) = editor.updateCells(block.id, selectedRowIds, column, change)

            DatabaseMenuLayer(
                title = label,
                anchor = { openLayer ->
                    DatabaseMenuOption(
                        label = label,
                        icon = { DatabaseOptionIcon(block.iconOf(column)) },
                        onClick = openLayer
                    )
                }
            ) { closeLayerAnd ->
                DatabaseBulkValueChoices(
                    block = block,
                    column = column,
                    editor = editor,
                    onSet = { change -> closeLayerAnd { closeMenuAnd { setEveryCell(change) } } }
                )
            }
        }
    }
}

@Composable
private fun DatabaseBulkValueChoices(
    block: DatabaseBlock,
    column: DatabaseColumnTarget,
    editor: DatabaseBlockEditor,
    onSet: ((PropertyBlock) -> PropertyBlock) -> Unit
) {
    val valueType = block.valueTypeOf(column) ?: PropertyValueType.TEXT
    when {
        valueType.holdsTags -> DatabaseBulkOptionChoices(column = column, editor = editor, onSet = onSet)
        valueType.holdsCheck -> {
            DatabaseMenuOption(label = stringResource(Res.string.database_checked), isSelected = false, onClick = { onSet { it.copy(isChecked = true) } })
            DatabaseMenuOption(label = stringResource(Res.string.database_unchecked), isSelected = false, onClick = { onSet { it.copy(isChecked = false) } })
        }
        valueType.holdsDate -> DatabaseBulkDateChoices(onSet = onSet)
        valueType.holdsPeople -> DatabaseBulkPeopleChoices(onSet = onSet)
        else -> DatabaseBulkTextChoices(holdsNumber = valueType.holdsNumber, onSet = onSet)
    }
}

@Composable
private fun DatabaseBulkOptionChoices(
    column: DatabaseColumnTarget,
    editor: DatabaseBlockEditor,
    onSet: ((PropertyBlock) -> PropertyBlock) -> Unit
) {
    val tagPoolKey = column.tagPoolKey
    val savedTags by remember(tagPoolKey) {
        if (tagPoolKey == null) flowOf(emptyList()) else editor.savedTagsOf(tagPoolKey)
    }.collectAsState(initial = emptyList())

    if (savedTags.isEmpty()) {
        DatabaseMenuMessage(text = stringResource(Res.string.database_no_options_saved_yet), color = MaterialTheme.colorScheme.outline)
    }
    savedTags.forEach { tag ->
        DatabaseMenuOption(
            label = tag.name,
            isSelected = false,
            icon = {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(rememberPropertyTagColor(tagPoolKey, tag.name))
                )
            },
            onClick = { onSet { it.copy(tags = listOf(tag.name)) } }
        )
    }
    DatabaseMenuSectionDivider()
    DatabaseBulkClearOption(onClear = { onSet { it.copy(tags = emptyList()) } })
}

@Composable
private fun DatabaseBulkPeopleChoices(onSet: ((PropertyBlock) -> PropertyBlock) -> Unit) {
    val people = rememberPeopleToPick().orEmpty()

    if (people.isEmpty()) {
        DatabaseMenuMessage(text = stringResource(Res.string.database_no_people_added_yet), color = MaterialTheme.colorScheme.outline)
    }
    people.forEach { person ->
        DatabaseMenuOption(
            label = person.displayName(),
            isSelected = false,
            icon = { PersonAvatar(personNoteId = person.id, name = person.name, icon = person.icon, photoPath = person.photoPath, size = 16.dp) },
            onClick = { onSet { it.copy(personNoteIds = listOf(person.id)) } }
        )
    }
    DatabaseMenuSectionDivider()
    DatabaseBulkClearOption(onClear = { onSet { it.copy(personNoteIds = emptyList()) } })
}

@Composable
private fun DatabaseBulkDateChoices(onSet: ((PropertyBlock) -> PropertyBlock) -> Unit) {
    var showsDatePicker by remember { mutableStateOf(false) }
    val timeZone = TimeZone.currentSystemDefault()

    DatabaseMenuOption(label = stringResource(Res.string.database_pick_a_date), onClick = { showsDatePicker = true })
    DatabaseMenuSectionDivider()
    DatabaseBulkClearOption(onClear = { onSet { it.withDateRange(PropertyDateRange()) } })

    if (showsDatePicker) {
        MinimalDatePickerDialog(
            initialTimestamp = null,
            onDismiss = { showsDatePicker = false },
            onConfirm = { selectedMillis ->
                val pickedDate = Instant.fromEpochMilliseconds(selectedMillis).toLocalDateTime(timeZone).date
                showsDatePicker = false
                onSet { it.withDateRange(PropertyDateRange(start = pickedDate)) }
            }
        )
    }
}

@Composable
private fun DatabaseBulkTextChoices(holdsNumber: Boolean, onSet: ((PropertyBlock) -> PropertyBlock) -> Unit) {
    var typedText by remember { mutableStateOf("") }

    DatabaseMenuContent {
        EmberrTextField(
            value = typedText,
            onValueChange = { text -> if (!holdsNumber || isNumberBeingTyped(text)) typedText = text.replace('\n', ' ') },
            placeholder = stringResource(if (holdsNumber) Res.string.database_type_a_number else Res.string.database_type_a_value),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            keyboardOptions = if (holdsNumber) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
            onSubmit = { onSet { it.copy(text = typedText) } }
        )
    }
    DatabaseMenuOption(label = stringResource(Res.string.database_set_for_every_row), onClick = { onSet { it.copy(text = typedText) } })
    DatabaseMenuSectionDivider()
    DatabaseBulkClearOption(onClear = { onSet { it.copy(text = "") } })
}

@Composable
private fun DatabaseBulkClearOption(onClear: () -> Unit) {
    DatabaseMenuOption(
        label = stringResource(Res.string.database_clear),
        icon = { DatabaseOptionIcon(Res.drawable.x, tint = MaterialTheme.colorScheme.error) },
        labelColor = MaterialTheme.colorScheme.error,
        onClick = onClear
    )
}
