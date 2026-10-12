package com.emberr.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrChoiceRow
import com.emberr.presentation.shared.components.EmberrSpaceCheckboxOptions
import com.emberr.presentation.shared.components.checkedSpacesLabel
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.folder

private val ExportSheetButtonPadding = 20.dp

@Composable
fun ExportSpacesSheet(
    spaces: List<SpaceEntity>,
    onExportClick: (spaceIds: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var uncheckedSpaceIds by remember { mutableStateOf(emptySet<String>()) }
    val allSpaceIds = spaces.map { it.spaceId }.toSet()
    val checkedSpaceIds = allSpaceIds - uncheckedSpaceIds

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = "Export Backup",
        subtitle = "Choose the spaces to save.",
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            EmberrChoiceRow(
                label = "Select Spaces",
                value = checkedSpacesLabel(spaces, checkedSpaceIds),
                icon = Res.drawable.folder
            ) {
                EmberrSpaceCheckboxOptions(
                    spaces = spaces,
                    checkedSpaceIds = checkedSpaceIds,
                    onCheckedSpaceIdsChange = { newCheckedSpaceIds -> uncheckedSpaceIds = allSpaceIds - newCheckedSpaceIds }
                )
            }
            EmberrButtonPrimary(
                text = "Export",
                onClick = { closeAnd { onExportClick(checkedSpaceIds) } },
                enabled = checkedSpaceIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = ExportSheetButtonPadding, end = ExportSheetButtonPadding)
            )
            EmberrButtonSecondary(
                text = "Close",
                onClick = { closeAnd {} },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = ExportSheetButtonPadding, end = ExportSheetButtonPadding)
            )
        }
    }
}
