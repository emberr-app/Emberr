package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.MenuChoiceButton
import com.emberr.domain.database.activeView
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DatabaseSort
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_down
import emberr.shared.generated.resources.arrow_up
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.database_add_sort
import emberr.shared.generated.resources.database_remove_all_sorts
import emberr.shared.generated.resources.database_remove_sort
import emberr.shared.generated.resources.database_sort
import emberr.shared.generated.resources.database_sort_ascending
import emberr.shared.generated.resources.database_sort_by
import emberr.shared.generated.resources.database_sort_descending
import emberr.shared.generated.resources.database_sort_order
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DatabaseSortMenu(
    expanded: Boolean,
    block: DatabaseBlock,
    editor: DatabaseBlockEditor,
    onDismiss: () -> Unit
) {
    val view = block.activeView()
    val sorts = view.sorts
    val columnTargets = listOf<DatabaseColumnTarget>(DatabaseColumnTarget.NotesTitle) + block.columns
    val unsortedColumns = columnTargets.filter { target -> sorts.none { it.target == target } }

    fun setSorts(newSorts: List<DatabaseSort>) = editor.setSorts(block.id, view.id, newSorts)

    DatabaseMenu(expanded = expanded, title = stringResource(Res.string.database_sort), onDismiss = onDismiss) { _ ->
        if (sorts.isEmpty()) {
            DatabaseMenuSectionLabel(text = stringResource(Res.string.database_sort_by))
            DatabaseColumnChoices(
                block = block,
                columns = columnTargets,
                selectedColumn = null,
                onPick = { target -> setSorts(listOf(DatabaseSort(target = target))) }
            )
        } else {
            DatabaseActiveSorts(
                block = block,
                sorts = sorts,
                columnTargets = columnTargets,
                unsortedColumns = unsortedColumns,
                setSorts = ::setSorts
            )
        }
    }
}

@Composable
private fun DatabaseActiveSorts(
    block: DatabaseBlock,
    sorts: List<DatabaseSort>,
    columnTargets: List<DatabaseColumnTarget>,
    unsortedColumns: List<DatabaseColumnTarget>,
    setSorts: (List<DatabaseSort>) -> Unit
) {
    sorts.forEachIndexed { position, sort ->
        DatabaseSortEditor(
            block = block,
            sort = sort,
            columnsToPick = columnTargets.filter { it == sort.target || it in unsortedColumns },
            onChange = { changedSort -> setSorts(sorts.toMutableList().apply { set(position, changedSort) }) },
            onRemove = { setSorts(sorts - sort) }
        )
    }

    DatabaseMenuSectionDivider()
    if (unsortedColumns.isNotEmpty()) {
        DatabaseMenuLayer(
            title = stringResource(Res.string.database_add_sort),
            anchor = { openLayer ->
                DatabaseMenuOption(
                    label = stringResource(Res.string.database_add_sort),
                    icon = { DatabaseOptionIcon(Res.drawable.plus) },
                    onClick = openLayer
                )
            }
        ) { closeLayerAnd ->
            DatabaseColumnChoices(
                block = block,
                columns = unsortedColumns,
                selectedColumn = null,
                onPick = { target -> closeLayerAnd { setSorts(sorts + DatabaseSort(target = target)) } }
            )
        }
    }
    DatabaseMenuOption(
        label = stringResource(Res.string.database_remove_all_sorts),
        icon = { DatabaseOptionIcon(Res.drawable.x, tint = MaterialTheme.colorScheme.error) },
        labelColor = MaterialTheme.colorScheme.error,
        onClick = { setSorts(emptyList()) }
    )
}

@Composable
private fun DatabaseSortEditor(
    block: DatabaseBlock,
    sort: DatabaseSort,
    columnsToPick: List<DatabaseColumnTarget>,
    onChange: (DatabaseSort) -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DatabaseMenuRowInset, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DatabaseMenuLayer(
            title = stringResource(Res.string.database_sort_by),
            anchor = { openLayer -> MenuChoiceButton(text = block.displayLabelOf(sort.target), onClick = openLayer) },
            anchorIsMenuRow = false
        ) { closeLayerAnd ->
            DatabaseColumnChoices(
                block = block,
                columns = columnsToPick,
                selectedColumn = sort.target,
                onPick = { target -> closeLayerAnd { onChange(sort.copy(target = target)) } }
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        DatabaseMenuLayer(
            title = stringResource(Res.string.database_sort_order),
            anchor = { openLayer ->
                MenuChoiceButton(
                    text = stringResource(if (sort.isDescending) Res.string.database_sort_descending else Res.string.database_sort_ascending),
                    onClick = openLayer
                )
            },
            anchorIsMenuRow = false
        ) { closeLayerAnd ->
            DatabaseMenuOption(
                label = stringResource(Res.string.database_sort_ascending),
                isSelected = !sort.isDescending,
                icon = { DatabaseOptionIcon(Res.drawable.arrow_up) },
                onClick = { closeLayerAnd { onChange(sort.copy(isDescending = false)) } }
            )
            DatabaseMenuOption(
                label = stringResource(Res.string.database_sort_descending),
                isSelected = sort.isDescending,
                icon = { DatabaseOptionIcon(Res.drawable.arrow_down) },
                onClick = { closeLayerAnd { onChange(sort.copy(isDescending = true)) } }
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconTooltip(text = stringResource(Res.string.database_remove_sort)) {
            Icon(
                painter = painterResource(Res.drawable.x),
                contentDescription = stringResource(Res.string.database_remove_sort),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(4.dp)
                    .size(14.dp)
            )
        }
    }
}

@Composable
internal fun DatabaseColumnChoices(
    block: DatabaseBlock,
    columns: List<DatabaseColumnTarget>,
    selectedColumn: DatabaseColumnTarget?,
    onPick: (DatabaseColumnTarget) -> Unit
) {
    columns.forEach { target ->
        DatabaseMenuOption(
            label = block.displayLabelOf(target),
            isSelected = selectedColumn?.let { target == it },
            icon = { DatabaseOptionIcon(block.iconOf(target)) },
            onClick = { onPick(target) }
        )
    }
}
