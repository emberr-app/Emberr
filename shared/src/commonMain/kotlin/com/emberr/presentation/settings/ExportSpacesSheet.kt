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
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary

private val ExportSheetButtonPadding = 20.dp

@Composable
fun ExportSpacesSheet(
    spaces: List<SpaceEntity>,
    onExportClick: (spaceIds: Set<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var uncheckedSpaceIds by remember { mutableStateOf(emptySet<String>()) }
    val checkedSpaceIds = spaces.map { it.spaceId }.filterNot { it in uncheckedSpaceIds }.toSet()

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = "Export Backup",
        subtitle = "Choose the spaces to save.",
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            spaces.forEach { space ->
                EmberrBottomSheetOption(
                    label = space.displayName,
                    isSelected = space.spaceId in checkedSpaceIds,
                    onClick = {
                        uncheckedSpaceIds = if (space.spaceId in uncheckedSpaceIds) {
                            uncheckedSpaceIds - space.spaceId
                        } else {
                            uncheckedSpaceIds + space.spaceId
                        }
                    }
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
