package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.emberr.domain.database.activeView
import com.emberr.domain.database.builtInPropertiesNotYetAdded
import com.emberr.domain.database.calculationsFor
import com.emberr.domain.database.canGroupBy
import com.emberr.domain.database.columnWithKey
import com.emberr.domain.database.customPropertiesNotShown
import com.emberr.domain.database.groupByColumn
import com.emberr.domain.database.groupableColumns
import com.emberr.domain.database.groupedBy
import com.emberr.domain.database.sharedPropertiesNotYetAdded
import com.emberr.domain.database.valueTypesThatCanGroupABoard
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseCardSize
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseDateGrouping
import com.emberr.domain.model.DatabaseLoadLimitChoices
import com.emberr.domain.model.DatabaseView
import com.emberr.domain.model.DatabaseViewType
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.columnKey
import com.emberr.domain.model.valueTypeOf
import com.emberr.domain.table.DeviceTableLayoutStore
import com.emberr.presentation.shared.components.EmberrSwitch
import com.emberr.presentation.shared.editor.blockViews.property.iconResource
import com.emberr.presentation.shared.editor.blockViews.rememberDeviceTableLayout
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_down
import emberr.shared.generated.resources.calendar_day
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.eye_open
import emberr.shared.generated.resources.ghost_smile
import emberr.shared.generated.resources.hash
import emberr.shared.generated.resources.image
import emberr.shared.generated.resources.kanban
import emberr.shared.generated.resources.lock
import emberr.shared.generated.resources.maximize_2
import emberr.shared.generated.resources.pin_tack
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.sigma
import emberr.shared.generated.resources.textalign_justifycenter2
import emberr.shared.generated.resources.trash
import org.koin.compose.koinInject
import com.emberr.presentation.shared.editor.blockViews.property.displayName
import emberr.shared.generated.resources.database_add_a_property
import emberr.shared.generated.resources.database_all_views
import emberr.shared.generated.resources.database_card_size
import emberr.shared.generated.resources.database_count_only
import emberr.shared.generated.resources.database_delete_database
import emberr.shared.generated.resources.database_freeze_title_column
import emberr.shared.generated.resources.database_group_by
import emberr.shared.generated.resources.database_group_calculation
import emberr.shared.generated.resources.database_group_dates_by
import emberr.shared.generated.resources.database_hidden_column_count
import emberr.shared.generated.resources.database_hide_empty_groups
import emberr.shared.generated.resources.database_load_limit
import emberr.shared.generated.resources.database_lock_database
import emberr.shared.generated.resources.database_named_view
import emberr.shared.generated.resources.database_new_property
import emberr.shared.generated.resources.database_no_columns_to_show_or_hide
import emberr.shared.generated.resources.database_row_count_setting
import emberr.shared.generated.resources.database_settings_title
import emberr.shared.generated.resources.database_show_hide_columns
import emberr.shared.generated.resources.database_wrap_cell_text
import emberr.shared.generated.resources.editor_show_cover_image_sentence_case
import emberr.shared.generated.resources.editor_show_icon
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseSettingsMenu(
    expanded: Boolean,
    block: DatabaseBlock,
    editor: DatabaseBlockEditor,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val activeView = block.activeView()
    val hiddenColumnCount = block.columns.count { it.columnKey in activeView.hiddenColumnKeys }
    val tableLayoutStore = koinInject<DeviceTableLayoutStore>()
    val freezesTitleColumn = rememberDeviceTableLayout(block.id).freezesTitleColumn(activeView)

    DatabaseMenu(expanded = expanded, title = stringResource(Res.string.database_settings_title), onDismiss = onDismiss) { closeAnd ->
        DatabaseMenuSectionLabel(text = stringResource(Res.string.database_named_view, activeView.shownName()))
        DatabaseMenuOption(
            label = stringResource(Res.string.editor_show_icon),
            icon = { DatabaseOptionIcon(Res.drawable.ghost_smile) },
            trailing = { EmberrSwitch(isOn = activeView.showsIcon) },
            onClick = { editor.setViewShowsIcon(block.id, activeView.id, !activeView.showsIcon) }
        )
        if (activeView.type == DatabaseViewType.TABLE) {
            DatabaseMenuOption(
                label = stringResource(Res.string.database_freeze_title_column),
                icon = { DatabaseOptionIcon(Res.drawable.pin_tack) },
                trailing = { EmberrSwitch(isOn = freezesTitleColumn) },
                onClick = { tableLayoutStore.setFreezesTitleColumn(block.id, activeView.id, !freezesTitleColumn) }
            )
            DatabaseMenuOption(
                label = stringResource(Res.string.database_wrap_cell_text),
                icon = { DatabaseOptionIcon(Res.drawable.textalign_justifycenter2) },
                trailing = { EmberrSwitch(isOn = activeView.wrapsCellText) },
                onClick = { editor.changeView(block.id, activeView.id) { it.copy(wrapsCellText = !it.wrapsCellText) } }
            )
        }
        if (activeView.type != DatabaseViewType.TABLE) {
            DatabaseMenuOption(
                label = stringResource(Res.string.editor_show_cover_image_sentence_case),
                icon = { DatabaseOptionIcon(Res.drawable.image) },
                trailing = { EmberrSwitch(isOn = activeView.showsCoverImage) },
                onClick = { editor.setViewShowsCoverImage(block.id, activeView.id, !activeView.showsCoverImage) }
            )
        }
        if (activeView.type == DatabaseViewType.BOARD) {
            DatabaseBoardGroupingOptions(block = block, view = activeView, editor = editor)
        }
        if (activeView.type != DatabaseViewType.TABLE) {
            DatabaseMenuLayer(
                title = stringResource(Res.string.database_card_size),
                anchor = { openLayer ->
                    DatabaseMenuOption(
                        label = stringResource(Res.string.database_card_size),
                        icon = { DatabaseOptionIcon(Res.drawable.maximize_2) },
                        trailing = {
                            Text(
                                text = activeView.cardSize.displayName(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        },
                        onClick = openLayer
                    )
                }
            ) { closeLayerAnd ->
                DatabaseCardSize.entries.forEach { cardSize ->
                    DatabaseMenuOption(
                        label = cardSize.displayName(),
                        isSelected = cardSize == activeView.cardSize,
                        onClick = { closeLayerAnd { editor.setViewCardSize(block.id, activeView.id, cardSize) } }
                    )
                }
            }
        }

        DatabaseMenuLayer(
            title = stringResource(Res.string.database_load_limit),
            anchor = { openLayer ->
                DatabaseMenuOption(
                    label = stringResource(Res.string.database_load_limit),
                    icon = { DatabaseOptionIcon(Res.drawable.arrow_down) },
                    trailing = { DatabaseSettingValueText(text = rowCountText(activeView.loadLimit)) },
                    onClick = openLayer
                )
            }
        ) { closeLayerAnd ->
            DatabaseLoadLimitChoices.forEach { loadLimit ->
                DatabaseMenuOption(
                    label = rowCountText(loadLimit),
                    isSelected = loadLimit == activeView.loadLimit,
                    onClick = { closeLayerAnd { editor.changeView(block.id, activeView.id) { it.copy(loadLimit = loadLimit) } } }
                )
            }
        }

        DatabaseMenuLayer(
            title = stringResource(Res.string.database_show_hide_columns),
            anchor = { openLayer ->
                DatabaseMenuOption(
                    label = stringResource(Res.string.database_show_hide_columns),
                    icon = { DatabaseOptionIcon(Res.drawable.eye_open) },
                    trailing = if (hiddenColumnCount == 0) null else {
                        {
                            Text(
                                text = pluralStringResource(Res.plurals.database_hidden_column_count, hiddenColumnCount, hiddenColumnCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1
                            )
                        }
                    },
                    onClick = openLayer
                )
            }
        ) { _ ->
            if (block.columns.isEmpty()) {
                DatabaseMenuMessage(text = stringResource(Res.string.database_no_columns_to_show_or_hide), color = MaterialTheme.colorScheme.outline)
            }
            block.columns.forEach { column ->
                key(column.columnKey) {
                    val isShown = column.columnKey !in activeView.hiddenColumnKeys
                    DatabaseMenuOption(
                        label = block.displayLabelOf(column),
                        icon = { DatabaseOptionIcon(block.iconOf(column)) },
                        trailing = { EmberrSwitch(isOn = isShown) },
                        onClick = { editor.setColumnShown(block.id, activeView.id, column, !isShown) }
                    )
                }
            }
        }

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.database_all_views))
        DatabaseMenuOption(
            label = stringResource(Res.string.database_row_count_setting),
            icon = { DatabaseOptionIcon(Res.drawable.hash) },
            trailing = { EmberrSwitch(isOn = block.showsRowCount) },
            onClick = { editor.setShowsRowCount(block.id, !block.showsRowCount) }
        )
        DatabaseColorRulesOption(block = block, editor = editor)
        DatabaseMenuOption(
            label = stringResource(Res.string.database_lock_database),
            icon = { DatabaseOptionIcon(Res.drawable.lock) },
            trailing = { EmberrSwitch(isOn = block.isLocked) },
            onClick = { closeAnd { editor.setLocked(block.id, true) } }
        )
        DatabaseMenuOption(
            label = stringResource(Res.string.database_delete_database),
            icon = { DatabaseOptionIcon(Res.drawable.trash, tint = MaterialTheme.colorScheme.error) },
            labelColor = MaterialTheme.colorScheme.error,
            onClick = { closeAnd(onDelete) }
        )
    }
}

@Composable
private fun DatabaseBoardGroupingOptions(block: DatabaseBlock, view: DatabaseView, editor: DatabaseBlockEditor) {
    val groupColumn = block.groupByColumn(view)
    val groupValueType = groupColumn?.let { block.valueTypeOf(it) }

    DatabaseMenuLayer(
        title = stringResource(Res.string.database_group_by),
        anchor = { openLayer ->
            DatabaseMenuOption(
                label = stringResource(Res.string.database_group_by),
                icon = { DatabaseOptionIcon(Res.drawable.kanban) },
                trailing = if (groupColumn == null) null else { { DatabaseSettingValueText(text = block.displayLabelOf(groupColumn)) } },
                onClick = openLayer
            )
        }
    ) { closeLayerAnd ->
        block.groupableColumns().forEach { column ->
            key(column.columnKey) {
                DatabaseMenuOption(
                    label = block.displayLabelOf(column),
                    icon = { DatabaseOptionIcon(block.iconOf(column)) },
                    isSelected = column == groupColumn,
                    onClick = { closeLayerAnd { editor.changeView(block.id, view.id) { it.groupedBy(column.columnKey) } } }
                )
            }
        }

        val builtInPropertiesToAdd = block.builtInPropertiesNotYetAdded().filter { canGroupBy(it.valueType) }
        val customPropertiesToShow = block.customPropertiesNotShown().filter { canGroupBy(it.valueType) }
        val sharedPropertiesToAdd = block.sharedPropertiesNotYetAdded(rememberSharedProperties()).filter { canGroupBy(it.valueType) }
        if (builtInPropertiesToAdd.isNotEmpty() || customPropertiesToShow.isNotEmpty() || sharedPropertiesToAdd.isNotEmpty()) {
            if (block.groupableColumns().isNotEmpty()) {
                DatabaseMenuSectionDivider()
            }
            DatabaseMenuSectionLabel(text = stringResource(Res.string.database_add_a_property))
        }
        builtInPropertiesToAdd.forEach { propertyType ->
            DatabaseMenuOption(
                label = propertyType.displayName(),
                icon = { DatabaseOptionIcon(propertyType.iconResource()) },
                onClick = {
                    closeLayerAnd { editor.addColumn(block.id, DatabaseColumnTarget.Property(propertyType), viewIdToGroupByIt = view.id) }
                }
            )
        }
        customPropertiesToShow.forEach { property ->
            key(property.id) {
                DatabaseMenuOption(
                    label = property.name,
                    icon = { DatabaseOptionIcon(property.valueType.iconResource()) },
                    onClick = {
                        closeLayerAnd { editor.addColumn(block.id, DatabaseColumnTarget.CustomProperty(property.id), viewIdToGroupByIt = view.id) }
                    }
                )
            }
        }
        sharedPropertiesToAdd.forEach { property ->
            key(property.id) {
                DatabaseMenuOption(
                    label = property.name,
                    icon = { DatabaseOptionIcon(property.valueType.iconResource()) },
                    onClick = { closeLayerAnd { editor.addSharedProperty(block.id, property, viewIdToGroupByIt = view.id) } }
                )
            }
        }

        DatabaseMenuLayer(
            title = stringResource(Res.string.database_new_property),
            showsCloseButton = false,
            anchor = { openNewProperty ->
                DatabaseMenuOption(
                    label = stringResource(Res.string.database_new_property),
                    icon = { DatabaseOptionIcon(Res.drawable.plus) },
                    onClick = openNewProperty
                )
            }
        ) { closeNewPropertyAnd ->
            DatabaseNewPropertyPage(
                block = block,
                editor = editor,
                onCancel = { closeNewPropertyAnd { } },
                closeAnd = { action -> closeNewPropertyAnd { closeLayerAnd(action) } },
                allowedValueTypes = valueTypesThatCanGroupABoard,
                viewIdToGroupByIt = view.id
            )
        }
    }

    if (groupValueType?.holdsDate == true) {
        DatabaseMenuLayer(
            title = stringResource(Res.string.database_group_dates_by),
            anchor = { openLayer ->
                DatabaseMenuOption(
                    label = stringResource(Res.string.database_group_dates_by),
                    icon = { DatabaseOptionIcon(Res.drawable.calendar_day) },
                    trailing = { DatabaseSettingValueText(text = view.dateGrouping.displayName()) },
                    onClick = openLayer
                )
            }
        ) { closeLayerAnd ->
            DatabaseDateGrouping.entries.forEach { dateGrouping ->
                DatabaseMenuOption(
                    label = dateGrouping.displayName(),
                    isSelected = dateGrouping == view.dateGrouping,
                    onClick = {
                        closeLayerAnd {
                            editor.changeView(block.id, view.id) {
                                it.copy(dateGrouping = dateGrouping, hiddenGroupKeys = emptyList(), collapsedGroupKeys = emptyList())
                            }
                        }
                    }
                )
            }
        }
    }

    DatabaseMenuOption(
        label = stringResource(Res.string.database_hide_empty_groups),
        icon = { DatabaseOptionIcon(Res.drawable.eye_closed) },
        trailing = { EmberrSwitch(isOn = view.hidesEmptyGroups) },
        onClick = { editor.changeView(block.id, view.id) { it.copy(hidesEmptyGroups = !it.hidesEmptyGroups) } }
    )

    DatabaseGroupCalculationOption(block = block, view = view, editor = editor)
}

@Composable
private fun DatabaseGroupCalculationOption(block: DatabaseBlock, view: DatabaseView, editor: DatabaseBlockEditor) {
    val calculationColumn = block.columnWithKey(view.groupCalculationColumnKey)
    val calculation = view.groupCalculation
    val columnsToCalculate = listOf<DatabaseColumnTarget>(DatabaseColumnTarget.NotesTitle) + block.columns

    DatabaseMenuLayer(
        title = stringResource(Res.string.database_group_calculation),
        anchor = { openLayer ->
            DatabaseMenuOption(
                label = stringResource(Res.string.database_group_calculation),
                icon = { DatabaseOptionIcon(Res.drawable.sigma) },
                trailing = if (calculationColumn == null || calculation == null) null else {
                    { DatabaseSettingValueText(text = "${calculation.shortDisplayName()} · ${block.displayLabelOf(calculationColumn)}") }
                },
                onClick = openLayer
            )
        }
    ) { closeCalculationAnd ->
        DatabaseMenuOption(
            label = stringResource(Res.string.database_count_only),
            isSelected = calculation == null,
            onClick = {
                closeCalculationAnd {
                    editor.changeView(block.id, view.id) { it.copy(groupCalculationColumnKey = null, groupCalculation = null) }
                }
            }
        )
        columnsToCalculate.forEach { column ->
            key(column.columnKey) {
                val valueType = block.valueTypeOf(column) ?: PropertyValueType.TEXT
                DatabaseMenuLayer(
                    title = block.displayLabelOf(column),
                    anchor = { openColumn ->
                        DatabaseMenuOption(
                            label = block.displayLabelOf(column),
                            icon = { DatabaseOptionIcon(block.iconOf(column)) },
                            isSelected = column == calculationColumn && calculation != null,
                            onClick = openColumn
                        )
                    }
                ) { closeColumnAnd ->
                    calculationsFor(valueType).forEach { option ->
                        DatabaseMenuOption(
                            label = option.displayName(),
                            isSelected = column == calculationColumn && option == calculation,
                            onClick = {
                                closeColumnAnd {
                                    closeCalculationAnd {
                                        editor.changeView(block.id, view.id) {
                                            it.copy(groupCalculationColumnKey = column.columnKey, groupCalculation = option)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseSettingValueText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1
    )
}
